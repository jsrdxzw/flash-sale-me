package com.jsrdxzw.flashsale.app.model.command;

import lombok.Data;

/**
 * @author xuzhiwei
 * @date 2021/12/4 11:12 PM
 */
@Data
public class FlashPlaceOrderCommand {
    /**
     * 订单ID
     */
    private Long id;
    /**
     * 商品ID
     */
    private Long itemId;
    /**
     * 活动ID
     */
    private Long activityId;
    /**
     * 下单商品数量
     */
    private Integer quantity;
    /**
     * 总金额
     */
    private Long totalAmount;

    public boolean validateParams() {
        return itemId != null && activityId != null && quantity != null && quantity > 0;
    }
}
