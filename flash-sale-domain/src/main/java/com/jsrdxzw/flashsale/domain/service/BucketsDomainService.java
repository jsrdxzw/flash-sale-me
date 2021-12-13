package com.jsrdxzw.flashsale.domain.service;

import com.jsrdxzw.flashsale.domain.model.Bucket;

import java.util.List;

/**
 * @author xuzhiwei
 * @date 2021/12/13 10:03 AM
 */
public interface BucketsDomainService {
    boolean suspendBuckets(Long itemId);

    List<Bucket> getBucketsByItem(Long itemId);

    boolean arrangeBuckets(Long itemId, List<Bucket> buckets);

    boolean resumeBuckets(Long itemId);
}
