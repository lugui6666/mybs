package com.whpu.mybs.hpinstance.fallback;

import com.whpu.mybs.common.dto.HoneypotTypeDTO;
import com.whpu.mybs.common.dto.R;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.hpinstance.feign.HoneypotTypeFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class HoneypotTypeClientFallbackFactory implements FallbackFactory<HoneypotTypeFeignClient> {
    @Override
    public HoneypotTypeFeignClient create(Throwable cause) {
        log.error("获取类型信息失败", cause);
        return id -> {
            HoneypotTypeDTO defaultDTO = new HoneypotTypeDTO();
            defaultDTO.setId(id);
            defaultDTO.setImageName("baseVM");   // 默认镜像名
            defaultDTO.setMinCpu(1);              // 默认最小 CPU
            defaultDTO.setMinMemory(512);         // 默认内存（MB）
            // 返回成功响应，携带默认数据
            return R.ok(defaultDTO);
        };
    }
}
