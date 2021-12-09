package com.jsrdxzw.flashsale.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xuzhiwei
 * @date 2021/12/5 12:09 PM
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FlashOrderDO extends BaseDO {
    private Long itemId;
    private String itemTitle;
    private Long flashPrice;
    private Long activityId;
    private Integer quantity;
    private Long totalAmount;
    private Integer status;
    private Long userId;
}
