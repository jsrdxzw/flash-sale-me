package com.jsrdxzw.flashsale.domain.repository;

import com.jsrdxzw.flashsale.domain.model.Bucket;

import java.util.List;

/**
 * @author xuzhiwei
 * @date 2021/12/13 10:14 AM
 */
public interface BucketsRepository {
    boolean submitBuckets(Long itemId, List<Bucket> buckets);

    boolean decreaseItemStock(Long itemId, Integer quantity, Integer serialNo);

    boolean increaseItemStock(Long itemId, Integer quantity, Integer serialNo);

    List<Bucket> getBucketsByItem(Long itemId);

    boolean suspendBuckets(Long itemId);

    boolean resumeStockBuckets(Long itemId);
}
