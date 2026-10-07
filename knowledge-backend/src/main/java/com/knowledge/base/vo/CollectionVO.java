package com.knowledge.base.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合集展示对象：广场/我的列表用基本信息，详情时填充 articles
 */
@Data
public class CollectionVO {
    private Long id;
    /** 创建者ID */
    private Long userId;
    private String name;
    private String description;
    private BigDecimal price;
    private LocalDateTime createTime;
    /** 创建者昵称 */
    private String creatorNickname;
    /** 合集内文章数 */
    private Long articleCount;
    /** 当前用户是否已购：1-已购 0-未购（匿名恒 0） */
    private Integer purchased;
    /** 当前用户是否为创建者 */
    private Boolean own;
    /** 合集内文章（详情时填充，子集字段无正文） */
    private List<ArticleSummaryVO> articles;
}
