package com.knowledge.base.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdjustBalanceRequest {
    @NotNull(message = "调整金额不能为空")
    @DecimalMin(value = "-10000", message = "单次调整金额不能超过 -10000")
    @DecimalMax(value = "10000", message = "单次调整金额不能超过 10000")
    @Digits(integer = 5, fraction = 2, message = "金额最多两位小数")
    private BigDecimal change;

    @Size(max = 200, message = "备注最多 200 字")
    private String remark;
}
