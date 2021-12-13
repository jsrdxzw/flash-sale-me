package com.jsrdxzw.flashsale.app.model.command;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xuzhiwei
 * @date 2021/12/12 10:03 PM
 */
@Data
@Accessors(chain = true)
public class BucketsArrangementCommand {
    private Integer totalStocksAmount;
    private Integer bucketsQuantity;
    private Integer arrangementMode;
}
