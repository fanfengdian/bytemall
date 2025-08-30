package com.bytemall.bytemall.listener;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MemberEventListener {

    /**
     * 监听新用户注册事件
     * @param memberId 消息内容，即注册成功的用户ID
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "mail.queue", durable = "true"), // 队列：用于发送欢迎邮件
            exchange = @Exchange(name = "member.exchange", type = "topic", ignoreDeclarationExceptions = "true"), // 交换机
            key = "member.registered" // 路由键
    ))
    public void listenToMemberRegistered(Long memberId) {
        System.out.println("====== [邮件服务] 收到新用户注册消息，用户ID: " + memberId + " ======");

        // 模拟发送欢迎邮件的耗时操作
        try {
            System.out.println("====== [邮件服务] 正在准备发送欢迎邮件... ======");
            Thread.sleep(3000); // 暂停3秒，模拟网络延迟
            System.out.println("====== [邮件服务] 欢迎邮件发送成功！======");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 监听同一个事件，用于送积分
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "points.queue", durable = "true"), // 队列：用于送积分
            exchange = @Exchange(name = "member.exchange", type = "topic", ignoreDeclarationExceptions = "true"),
            key = "member.registered"
    ))
    public void listenToMemberRegisteredForPoints(Long memberId) {
        System.out.println("====== [积分服务] 收到新用户注册消息，用户ID: " + memberId + " ======");
        System.out.println("====== [积分服务] 正在为用户增加100初始积分... ======");
        // ... (这里可以调用积分相关的Service)
        System.out.println("====== [积分服务] 初始积分赠送成功！======");
    }
}
