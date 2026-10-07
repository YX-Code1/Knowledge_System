package com.knowledge.base.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RechargeRequest {
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额最少 0.01 元")
    @DecimalMax(value = "10000", message = "单次充值金额最多 10000 元")
    @Digits(integer = 5, fraction = 2, message = "金额最多两位小数")
    private BigDecimal amount;
}
