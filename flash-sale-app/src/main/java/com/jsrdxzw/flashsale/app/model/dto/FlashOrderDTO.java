package com.jsrdxzw.flashsale.app.model.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author xuzhiwei
 * @date 2021/12/5 10:00 AM
 */
@Data
public class FlashOrderDTO {
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
    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单创建时间
     */
    private Date createTime;
}
