package com.knowledge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("collection_purchase")
public class CollectionPurchase {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 合集ID */
    private Long collectionId;

    /** 买家ID */
    private Long buyerId;

    /** 成交价 */
    private BigDecimal amount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
