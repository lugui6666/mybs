package com.whpu.mybs.hptype;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.whpu.mybs.hptype", "com.whpu.mybs.common"})
@EnableDiscoveryClient
@MapperScan("com.whpu.mybs.hptype.mapper")
public class HpTypeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HpTypeServiceApplication.class, args);
    }

}
