package com.jsrdxzw.flashsale.controller.model.response;

import lombok.Data;

import java.util.Date;

/**
 * @author xuzhiwei
 * @date 2021/12/2 7:50 PM
 */
@Data
public class FlashActivityResponse {
    /**
     * 活动ID
     */
    private Long id;
    /**
     * 活动名称
     */
    private String activityName;
    /**
     * 活动开始时间
     */
    private Date startTime;
    /**
     * 活动结束时间
     */
    private Date endTime;
    /**
     * 活动状态
     */
    private Integer status;
    /**
     * 活动描述
     */
    private String activityDesc;

    /**
     * 数据版本
     */
    private Long version;
}
