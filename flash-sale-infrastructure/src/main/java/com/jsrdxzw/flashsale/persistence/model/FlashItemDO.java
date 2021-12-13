package com.jsrdxzw.flashsale.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author xuzhiwei
 * @date 2021/12/3 5:38 PM
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FlashItemDO extends BaseDO {
    private String itemTitle;
    private String itemSubTitle;
    private Integer initialStock;
    private Integer availableStock;
    private Integer stockWarmUp;
    private Long originalPrice;
    private Long flashPrice;
    private Date startTime;
    private Date endTime;
    private Integer status;
    private Long activityId;
    private String itemDesc;
    private String rules;
}
