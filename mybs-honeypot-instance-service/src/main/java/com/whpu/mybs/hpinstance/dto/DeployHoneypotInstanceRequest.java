package com.whpu.mybs.hpinstance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeployHoneypotInstanceRequest {
    @NotBlank
    private String instanceName;
    @NotBlank
    private Long typeId;
    private String description;
}
