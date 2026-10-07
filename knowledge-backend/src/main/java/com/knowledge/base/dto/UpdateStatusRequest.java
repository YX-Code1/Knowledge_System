package com.knowledge.base.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull(message = "状态不能为空")
    private Integer status;
}
