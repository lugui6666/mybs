package com.whpu.mybs.hpinstance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.whpu.mybs.hpinstance", "com.whpu.mybs.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.whpu.mybs.hpinstance.feign")
@MapperScan("com.whpu.mybs.hpinstance.mapper")
public class HpInstanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HpInstanceServiceApplication.class, args);
    }

}
