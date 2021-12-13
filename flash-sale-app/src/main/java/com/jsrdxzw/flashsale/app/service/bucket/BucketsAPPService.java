package com.jsrdxzw.flashsale.app.service.bucket;

import com.jsrdxzw.flashsale.app.model.command.BucketsArrangementCommand;
import com.jsrdxzw.flashsale.app.model.dto.StockBucketSummaryDTO;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;

/**
 * @author xuzhiwei
 * @date 2021/12/12 10:05 PM
 */
public interface BucketsAPPService {
    AppSimpleResult<?> arrangeStockBuckets(Long userId, Long itemId, BucketsArrangementCommand arrangementCommand);

    AppSimpleResult<StockBucketSummaryDTO> getStockBucketsSummary(Long userId, Long itemId);
}
