package com.whpu.mybs.hpinstance.config;

import com.whpu.mybs.common.constant.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue deployQueue() {
        return new Queue(MqConstants.DEPLOY_QUEUE, true); // 持久化队列
    }

    @Bean
    public DirectExchange deployExchange() {
        return new DirectExchange(MqConstants.DEPLOY_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue deployQueue, DirectExchange deployExchange) {
        // 这里的 with("deploy.routingkey") 必须与生产者的路由键一致！
        return BindingBuilder.bind(deployQueue)
                .to(deployExchange)
                .with(MqConstants.DEPLOY_ROUTING_KEY);
    }
}