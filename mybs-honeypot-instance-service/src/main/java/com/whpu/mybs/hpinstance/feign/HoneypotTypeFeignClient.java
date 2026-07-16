package com.whpu.mybs.hpinstance.feign;

import com.whpu.mybs.common.dto.HoneypotTypeDTO;
import com.whpu.mybs.common.dto.R;
import com.whpu.mybs.hpinstance.fallback.HoneypotTypeClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 蜜罐类型服务 Feign 客户端
 * <p>
 * 通过 Nacos 服务发现调用 mybs-honeypot-type-service
 */
@FeignClient(name = "mybs-honeypot-type-service", fallbackFactory = HoneypotTypeClientFallbackFactory.class)
public interface HoneypotTypeFeignClient {

    /**
     * 查询蜜罐类型详情
     */
    @GetMapping("/api/hp-type/{id}")
    R<HoneypotTypeDTO> getByTypeId(@PathVariable Long id);

}
