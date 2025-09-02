package com.bytemall.bytemall.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRabbitMQConfig {

    /**
     * 将Jackson2JsonMessageConverter注册为Bean
     * Spring Boot会自动检测到这个Bean，并用它来序列化和反序列化MQ消息
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
