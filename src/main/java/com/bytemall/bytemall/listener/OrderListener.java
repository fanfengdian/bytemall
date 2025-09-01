package com.bytemall.bytemall.listener;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderListener {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "order.seckill.queue", durable = "true"),
            exchange = @Exchange(name = "order.exchange", type = "topic", ignoreDeclarationExceptions = "true"),
            key = "order.seckill.create"
    ))
    public void listenToSeckillOrder(Map<String, Object> orderTask) {
        System.out.println("====== [订单服务] 收到异步下单任务 ======");
        System.out.println("订单号: " + orderTask.get("orderSn"));
        System.out.println("用户ID: " + orderTask.get("userId"));
        System.out.println("商品ID: " + orderTask.get("skuId"));

        // 模拟数据库创建订单和扣减库存
        try {
            System.out.println("====== [订单服务] 正在创建订单并扣减数据库库存... ======");
            // 1. 创建订单 (调用OrderService.createOrder(...))
            // 2. 扣减库存 (调用ProductService.decreaseStock(...))
            Thread.sleep(500); // 模拟DB操作耗时
            System.out.println("====== [订单服务] 数据库操作完成，订单创建成功！ ======");
        } catch (Exception e) {
            // 在真实业务中，如果这里失败了，需要有补偿机制
            // 比如记录失败日志，或者把库存信号量还回去
            // semaphore.release();
            System.err.println("异步下单失败: " + e.getMessage());
        }
    }
}
