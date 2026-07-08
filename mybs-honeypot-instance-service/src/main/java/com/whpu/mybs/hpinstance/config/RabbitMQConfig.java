package com.whpu.mybs.hpinstance.config;

import com.whpu.mybs.common.constant.MqConstants;
import org.springframework.amqp.core.*;
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
        return BindingBuilder.bind(deployQueue)
                .to(deployExchange)
                .with(MqConstants.DEPLOY_ROUTING_KEY);
    }

    @Bean
    public Queue stopQueue() {
        return new Queue(MqConstants.STOP_QUEUE, true); // 持久化
    }

    @Bean
    public Binding stopBinding(Queue stopQueue, DirectExchange deployExchange) {
        return BindingBuilder.bind(stopQueue)
                .to(deployExchange)
                .with(MqConstants.STOP_ROUTING_KEY);
    }

    @Bean
    public Queue startQueue() {
        return new Queue(MqConstants.START_QUEUE, true);
    }

    @Bean
    public Binding startBinding(Queue startQueue, DirectExchange deployExchange) {
        return BindingBuilder.bind(startQueue)
                .to(deployExchange)
                .with(MqConstants.START_ROUTING_KEY);
    }

    @Bean
    public Queue destroyQueue() {
        return new Queue(MqConstants.DESTROY_QUEUE, true);
    }

    @Bean
    public Binding destroyBinding(Queue destroyQueue, DirectExchange deployExchange) {
        return BindingBuilder.bind(destroyQueue)
                .to(deployExchange)
                .with(MqConstants.DESTROY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange healthExchange() {
        return new DirectExchange(MqConstants.HEALTH_EXCHANGE);
    }

    // 延迟队列（消息在此等待 30 秒）
    @Bean
    public Queue healthDelayQueue() {
        return QueueBuilder.durable(MqConstants.HEALTH_DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", MqConstants.HEALTH_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstants.HEALTH_WORK_ROUTING_KEY)
                .build();
    }

    // 工作队列（实际消费）
    @Bean
    public Queue healthWorkQueue() {
        return QueueBuilder.durable(MqConstants.HEALTH_WORK_QUEUE).build();
    }

    // 绑定：延迟队列 → 交换机（用于发送延迟消息）
    @Bean
    public Binding healthDelayBinding(Queue healthDelayQueue, DirectExchange healthExchange) {
        return BindingBuilder.bind(healthDelayQueue)
                .to(healthExchange)
                .with(MqConstants.HEALTH_DELAY_ROUTING_KEY);
    }

    // 绑定：工作队列 → 交换机（用于死信转发）
    @Bean
    public Binding healthWorkBinding(Queue healthWorkQueue, DirectExchange healthExchange) {
        return BindingBuilder.bind(healthWorkQueue)
                .to(healthExchange)
                .with(MqConstants.HEALTH_WORK_ROUTING_KEY);
    }
}