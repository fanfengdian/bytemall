package com.bytemall.bytemall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytemall.bytemall.entity.SeckillSession;
import com.bytemall.bytemall.entity.SeckillSkuRelation;
import com.bytemall.bytemall.service.SeckillService;
import com.bytemall.bytemall.service.SeckillSessionService;
import com.bytemall.bytemall.service.SeckillSkuRelationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Resource
    private SeckillSessionService sessionService;

    @Resource
    private SeckillSkuRelationService skuRelationService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient; // 注入Redisson客户端

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 定义清晰的Redis Key前缀，方便管理
    private static final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private static final String SKUS_CACHE_PREFIX = "seckill:skus"; // Hash的大Key
    private static final String SKU_STOCK_SEMAPHORE_PREFIX = "seckill:stock:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        // TODO: 在这里实现核心的上架逻辑
        System.out.println("====== 开始上架秒杀商品... ======");
        // 1. 查询未来3天内需要参与秒杀的活动场次
        List<SeckillSession> sessions = sessionService.list(new LambdaQueryWrapper<SeckillSession>()
                .between(SeckillSession::getStartTime, LocalDateTime.now(), LocalDateTime.now().plusDays(3)));

        if (sessions != null && !sessions.isEmpty()) {
            sessions.forEach(session -> {
                long startTime = session.getStartTime().toEpochSecond(ZoneOffset.UTC);
                long endTime = session.getEndTime().toEpochSecond(ZoneOffset.UTC);

                // 2. 缓存场次与商品ID的关联
                // Key -> seckill:sessions:1724943888_1725030588
                // Value -> [1, 2, 3] (商品ID列表)
                String sessionKey = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
                List<SeckillSkuRelation> relations = skuRelationService.list(new LambdaQueryWrapper<SeckillSkuRelation>()
                        .eq(SeckillSkuRelation::getSessionId, session.getId()));

                if (relations != null && !relations.isEmpty()) {
                    Set<String> skuIds = relations.stream()
                            .map(r -> r.getSkuId().toString())
                            .collect(Collectors.toSet());
                    // 确保每次上架前，旧的数据被清理 (可选)
                    stringRedisTemplate.delete(sessionKey);
                    stringRedisTemplate.opsForSet().add(sessionKey, skuIds.toArray(new String[0]));

                    // 3. 缓存每个秒杀商品的详细信息 (使用Hash)
                    // Key -> seckill:skus
                    // Field -> sessionId_skuId (例如 1_2)
                    // Value -> 商品详情JSON
                    relations.forEach(relation -> {
                        String skuField = relation.getSessionId() + "_" + relation.getSkuId();
                        try {
                            String skuInfoJson = MAPPER.writeValueAsString(relation);
                            stringRedisTemplate.opsForHash().put(SKUS_CACHE_PREFIX, skuField, skuInfoJson);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // 4. 缓存库存信号量 (使用Redisson)
                        // Key -> seckill:stock:2 (商品ID)
                        String semaphoreKey = SKU_STOCK_SEMAPHORE_PREFIX + relation.getSkuId();
                        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
                        // 设置信号量的“许可”数量为库存数
                        semaphore.trySetPermits(relation.getSeckillCount());
                        System.out.println("商品 " + relation.getSkuId() + " 的库存信号量已设置，数量：" + relation.getSeckillCount());
                    });
                }
            });
        }
        System.out.println("====== 秒杀商品上架完成 ======");
    }


    @Resource
    private RabbitTemplate rabbitTemplate; // 注入RabbitTemplate

    @Override
    public String kill(Long skuId, Long userId) throws Exception {
        // 1. 获取当前秒杀商品的详细信息
        String skuInfoJson = (String) stringRedisTemplate.opsForHash().get(SKUS_CACHE_PREFIX, "1_" + skuId); // 假设sessionId都是1
        if (skuInfoJson == null) {
            throw new Exception("秒杀商品信息不存在");
        }
        SeckillSkuRelation skuInfo = MAPPER.readValue(skuInfoJson, SeckillSkuRelation.class);

        // 2. 合法性校验
        long now = new Date().getTime();
        // 假设场次信息也缓存在商品详情里，或者需要再次查询
        // long startTime = skuInfo.getStartTime().toEpochSecond(ZoneOffset.UTC) * 1000;
        // long endTime = skuInfo.getEndTime().toEpochSecond(ZoneOffset.UTC) * 1000;
        // if (now < startTime || now > endTime) {
        //     throw new Exception("当前时间不在秒杀活动范围内");
        // }

        // 3. 校验每人限购 (使用Redis的Set数据结构)
        String userBoughtKey = "seckill:bought:" + skuId;
        // sadd命令会尝试将userId加入Set，如果成功，返回1；如果已存在，返回0
        if (!stringRedisTemplate.opsForSet().add(userBoughtKey, userId.toString()).equals(1L)) {
            throw new Exception("您已经购买过该商品，请勿重复抢购");
        }

        // 4. 【核心】扣减库存信号量
        String semaphoreKey = SKU_STOCK_SEMAPHORE_PREFIX + skuId;
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
        // 尝试获取一个许可（库存），最多等待100毫秒
        boolean acquired = semaphore.tryAcquire(1, 100, TimeUnit.MILLISECONDS);

        if (acquired) {
            // 5. 【成功】获取到库存，发送异步下单消息
            System.out.println("====== 用户 " + userId + " 成功抢到商品 " + skuId + "，准备发送异步下单消息 ======");
            String orderSn = UUID.randomUUID().toString().replace("-", ""); // 生成一个简单的订单号

            // 创建一个消息对象（可以用Map或专门的DTO）
            Map<String, Object> orderTask = new HashMap<>();
            orderTask.put("orderSn", orderSn);
            orderTask.put("skuId", skuId);
            orderTask.put("userId", userId);
            orderTask.put("price", skuInfo.getSeckillPrice());

            rabbitTemplate.convertAndSend("order.exchange", "order.seckill.create", orderTask);

            return orderSn; // 立即返回订单号，给用户“秒杀成功”的体验
        } else {
            // 6. 【失败】没有获取到库存，秒杀结束
            System.out.println("====== 商品 " + skuId + " 库存不足，用户 " + userId + " 秒杀失败 ======");
            // 恢复限购记录（因为并没有真正买到）
            stringRedisTemplate.opsForSet().remove(userBoughtKey, userId.toString());
            throw new Exception("手慢了，商品已被抢光！");
        }
    }
}
