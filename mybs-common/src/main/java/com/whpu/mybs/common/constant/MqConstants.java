package com.whpu.mybs.common.constant;

import javax.lang.model.element.NestingKind;

public class MqConstants {
    public static final String DEPLOY_EXCHANGE = "deploy.exchange";
    public static final String DEPLOY_ROUTING_KEY = "deploy.routingkey";
    public static final String DEPLOY_QUEUE = "deploy.queue";

    public static final String STOP_QUEUE = "stop.queue";
    public static final String STOP_ROUTING_KEY = "stop.routingkey";

    public static final String START_QUEUE = "start.queue";
    public static final String START_ROUTING_KEY = "start.routingkey";

    public static final String DESTROY_QUEUE = "destroy.queue";
    public static final String DESTROY_ROUTING_KEY = "destroy.routingkey";

    public static final String HEALTH_EXCHANGE = "health.exchange";
    public static final String HEALTH_DELAY_QUEUE = "health.delay.queue";
    public static final String HEALTH_WORK_QUEUE = "health.work.queue";
    public static final String HEALTH_DELAY_ROUTING_KEY = "health.delay.routingkey";
    public static final String HEALTH_WORK_ROUTING_KEY = "health.work.routingkey";

    public static final String LOG_EXCHANGE = "logs.exchange";
    public static final String LOG_QUEUE = "logs.queue";
    public static final String LOG_ROUTING_KEY = "logs.routingkey";
}
