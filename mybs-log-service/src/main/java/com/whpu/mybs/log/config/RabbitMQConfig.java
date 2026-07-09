package com.whpu.mybs.log.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.whpu.mybs.common.constant.MqConstants;

@Configuration
public class RabbitMQConfig {
    // ========== 声明交换机（Topic类型，持久化） ==========
    @Bean
    public TopicExchange logsExchange() {
        return new TopicExchange(MqConstants.LOG_EXCHANGE, true, false);
    }

    // ========== 声明队列（持久化） ==========
    @Bean
    public Queue logsQueue() {
        return QueueBuilder.durable(MqConstants.LOG_QUEUE).build();
    }

    // ========== 绑定队列到交换机 ==========
    @Bean
    public Binding logsBinding() {
        return BindingBuilder.bind(logsQueue())
                .to(logsExchange())
                .with(MqConstants.LOG_ROUTING_KEY);
    }

    // ========== 消息序列化：JSON格式 ==========
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ========== 可选：自定义 RabbitTemplate ==========
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}