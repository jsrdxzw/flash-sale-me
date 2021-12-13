package com.jsrdxzw.flashsale.app.service.bucket;


import com.jsrdxzw.flashsale.app.model.dto.StockBucketSummaryDTO;

public interface BucketsArrangementService {
    void arrangeStockBuckets(Long itemId, Integer stocksAmount, Integer bucketsQuantity, Integer assignmentMode);

    StockBucketSummaryDTO queryStockBucketsSummary(Long itemId);
}
