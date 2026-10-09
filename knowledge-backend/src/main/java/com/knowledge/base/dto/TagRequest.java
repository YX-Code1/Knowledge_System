package com.knowledge.base.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建/更新标签请求。
 * 不直接绑定 Tag 实体：id/创建时间等字段由服务端设置，防止前端提交篡改（mass assignment）。
 */
@Data
public class TagRequest {

    @NotBlank(message = "标签名称不能为空")
    private String name;
}
