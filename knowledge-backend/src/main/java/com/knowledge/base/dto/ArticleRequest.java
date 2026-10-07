package com.knowledge.base.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ArticleRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    private String summary;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /** 售价，null 或 0 为免费 */
    @DecimalMin(value = "0", message = "价格不能为负")
    @DecimalMax(value = "10000", message = "价格不能超过 10000 元")
    @Digits(integer = 5, fraction = 2, message = "价格最多两位小数")
    private BigDecimal price;

    private List<Long> tagIds;
}
