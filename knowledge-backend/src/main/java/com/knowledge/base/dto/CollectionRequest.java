package com.knowledge.base.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CollectionRequest {
    @NotBlank(message = "合集名称不能为空")
    @Size(max = 50, message = "合集名称最多 50 字")
    private String name;

    @Size(max = 200, message = "合集描述最多 200 字")
    private String description;

    @DecimalMin(value = "0", message = "价格不能为负")
    @DecimalMax(value = "10000", message = "价格不能超过 10000 元")
    @Digits(integer = 5, fraction = 2, message = "价格最多两位小数")
    private BigDecimal price;
}
