package com.whpu.mybs.hpinstance.config;

import com.whpu.mybs.hpinstance.fallback.HoneypotTypeClientFallbackFactory;
import org.springframework.context.annotation.Bean;

public class FeignConfig {
    @Bean
    public HoneypotTypeClientFallbackFactory honeypotTypeClientFallbackFactory() {
        return new HoneypotTypeClientFallbackFactory();
    }
}
