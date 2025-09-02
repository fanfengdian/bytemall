package com.bytemall.bytemall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytemall.bytemall.entity.SeckillSession;
import com.bytemall.bytemall.entity.SeckillSkuRelation;
import com.bytemall.bytemall.mapper.SeckillSkuRelationMapper;
import com.bytemall.bytemall.service.SeckillService;
import com.bytemall.bytemall.service.SeckillSessionService;
import com.bytemall.bytemall.service.SeckillSkuRelationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
public class SeckillServiceImpl extends ServiceImpl<SeckillSkuRelationMapper, SeckillSkuRelation> implements SeckillService {

    @Resource
    private SeckillSessionService sessionService;
    @Resource
    private SeckillSkuRelationService skuRelationService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RabbitTemplate rabbitTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private static final String SKUS_CACHE_PREFIX = "seckill:skus";
    private static final String SKU_STOCK_SEMAPHORE_PREFIX = "seckill:stock:";
    private static final String USER_BOUGHT_PREFIX = "seckill:bought:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        System.out.println("====== 开始上架秒杀商品... ======");
        List<SeckillSession> sessions = sessionService.list(new LambdaQueryWrapper<SeckillSession>()
                .between(SeckillSession::getStartTime, LocalDateTime.now(), LocalDateTime.now().plusDays(3)));

        if (sessions != null && !sessions.isEmpty()) {
            sessions.forEach(session -> {
                long startTime = session.getStartTime().toEpochSecond(ZoneOffset.UTC);
                long endTime = session.getEndTime().toEpochSecond(ZoneOffset.UTC);
                String sessionKey = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;

                List<SeckillSkuRelation> relations = skuRelationService.list(new LambdaQueryWrapper<SeckillSkuRelation>()
                        .eq(SeckillSkuRelation::getSessionId, session.getId()));

                if (relations != null && !relations.isEmpty()) {
                    Set<String> skuIds = relations.stream()
                            .map(r -> session.getId() + "_" + r.getSkuId().toString())
                            .collect(Collectors.toSet());
                    stringRedisTemplate.delete(sessionKey);
                    stringRedisTemplate.opsForSet().add(sessionKey, skuIds.toArray(new String[0]));

                    Map<String, String> skuMap = new HashMap<>();
                    relations.forEach(relation -> {
                        String skuField = relation.getSessionId() + "_" + relation.getSkuId();
                        try {
                            Map<String, Object> skuDetail = new HashMap<>();
                            skuDetail.put("relation", relation);
                            skuDetail.put("session", session);
                            String skuInfoJson = MAPPER.writeValueAsString(skuDetail);
                            skuMap.put(skuField, skuInfoJson);
                        } catch (JsonProcessingException e) {
                            System.err.println("序列化秒杀商品失败: " + skuField);
                            e.printStackTrace();
                        }

                        String semaphoreKey = SKU_STOCK_SEMAPHORE_PREFIX + relation.getSkuId();
                        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
                        semaphore.trySetPermits(relation.getSeckillCount());
                    });
                    stringRedisTemplate.opsForHash().putAll(SKUS_CACHE_PREFIX, skuMap);
                    System.out.println("场次 " + session.getId() + " 的 " + relations.size() + " 个商品已上架。");
                }
            });
        }
        System.out.println("====== 秒杀商品上架完成 ======");
    }

    @Override
    public String kill(Long skuId, Long userId) throws Exception {
        // 假设场次ID为1，你需要确保测试数据与之匹配
        long sessionId = 1;

        String skuField = sessionId + "_" + skuId;
        String skuInfoJson = (String) stringRedisTemplate.opsForHash().get(SKUS_CACHE_PREFIX, skuField);
        if (skuInfoJson == null) {
            throw new Exception("秒杀商品信息不存在或活动已下架");
        }

        Map<String, Object> skuDetail = MAPPER.readValue(skuInfoJson, new TypeReference<>() {});
        SeckillSkuRelation skuInfo = MAPPER.convertValue(skuDetail.get("relation"), SeckillSkuRelation.class);
        SeckillSession sessionInfo = MAPPER.convertValue(skuDetail.get("session"), SeckillSession.class);

        // --- 2. 【最终版】使用LocalDateTime进行时间校验 ---
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = sessionInfo.getStartTime();
        LocalDateTime endTime = sessionInfo.getEndTime();

        if (now.isBefore(startTime)) {
            throw new Exception("秒杀尚未开始，请耐心等待！");
        }
        if (now.isAfter(endTime)) {
            throw new Exception("秒杀已经结束！");
        }

        // 3. 校验每人限购
        String userBoughtKey = USER_BOUGHT_PREFIX + skuId + "_" + userId;
        Boolean firstTimeBuy = stringRedisTemplate.opsForValue().setIfAbsent(userBoughtKey, "1", 2, TimeUnit.DAYS); // 设置一个较长的过期时间
        if (Boolean.FALSE.equals(firstTimeBuy)) {
            throw new Exception("您已经购买过该商品，请勿重复抢购");
        }

        // 4. 扣减库存信号量
        String semaphoreKey = SKU_STOCK_SEMAPHORE_PREFIX + skuId;
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
        boolean acquired = semaphore.tryAcquire();

        if (acquired) {
            // 5. 发送异步下单消息
            String orderSn = UUID.randomUUID().toString().replace("-", "");
            Map<String, Object> orderTask = new HashMap<>();
            orderTask.put("orderSn", orderSn);
            orderTask.put("relation", skuInfo);
            orderTask.put("userId", userId);
            rabbitTemplate.convertAndSend("order.exchange", "order.seckill.create", orderTask);
            System.out.println("====== 用户 " + userId + " 成功抢到商品 " + skuId + "，订单号：" + orderSn + " ======");
            return orderSn;
        } else {
            // 6. 库存不足，秒杀失败
            stringRedisTemplate.delete(userBoughtKey); // 恢复限购记录
            System.out.println("====== 商品 " + skuId + " 库存不足，用户 " + userId + " 秒杀失败 ======");
            throw new Exception("手慢了，商品已被抢光！");
        }
    }
}