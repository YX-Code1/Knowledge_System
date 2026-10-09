package com.knowledge.base.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建/更新分类请求。
 * 不直接绑定 Category 实体：id/创建时间等字段由服务端设置，防止前端提交篡改（mass assignment）。
 */
@Data
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    /** 父分类ID，0 或 null 表示顶级分类 */
    private Long parentId;

    /** 排序值，可空 */
    private Integer sort;
}
