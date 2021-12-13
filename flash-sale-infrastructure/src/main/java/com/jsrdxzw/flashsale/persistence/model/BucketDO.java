package com.jsrdxzw.flashsale.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BucketDO extends BaseDO {
    private Long itemId;
    private Integer totalStocksAmount;
    private Integer availableStocksAmount;
    private Integer serialNo;
    private Integer status;
}
