package com.jsrdxzw.flashsale.controller.model.request;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xuzhiwei
 * @date 2021/12/12 10:01 PM
 */
@Data
@Accessors(chain = true)
public class BucketsArrangementRequest {
    /**
     * 总库存
     */
    private Integer totalStocksAmount;
    /**
     * 桶的个数
     */
    private Integer bucketsQuantity;
    /**
     * 全量，增量
     */
    private Integer arrangementMode;
}
