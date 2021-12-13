package com.jsrdxzw.flashsale.persistence;

import com.jsrdxzw.flashsale.domain.model.Bucket;
import com.jsrdxzw.flashsale.domain.model.enums.BucketStatus;
import com.jsrdxzw.flashsale.domain.repository.BucketsRepository;
import com.jsrdxzw.flashsale.persistence.coverter.FlashBucketMapping;
import com.jsrdxzw.flashsale.persistence.mapper.BucketMapper;
import com.jsrdxzw.flashsale.persistence.model.BucketDO;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xuzhiwei
 * @date 2021/12/13 10:15 AM
 */
@Repository
public class BucketsRepositoryImpl implements BucketsRepository {
    @Resource
    private BucketMapper bucketMapper;

    @Override
    public boolean submitBuckets(Long itemId, List<Bucket> buckets) {
        if (itemId == null || CollectionUtils.isEmpty(buckets)) {
            return false;
        }
        List<BucketDO> bucketDOS = FlashBucketMapping.INSTANCE.toBucketDOList(buckets);
        bucketMapper.deleteByItem(itemId);
        bucketMapper.insertBatch(bucketDOS);
        return true;
    }

    @Override
    public boolean decreaseItemStock(Long itemId, Integer quantity, Integer serialNo) {
        if (itemId == null || quantity == null || serialNo == null) {
            return false;
        }
        return bucketMapper.decreaseItemStock(itemId, quantity, serialNo);
    }

    @Override
    public boolean increaseItemStock(Long itemId, Integer quantity, Integer serialNo) {
        if (itemId == null || quantity == null || serialNo == null) {
            return false;
        }
        return bucketMapper.increaseItemStock(itemId, quantity, serialNo);
    }

    @Override
    public List<Bucket> getBucketsByItem(Long itemId) {
        if (itemId == null) {
            return new ArrayList<>();
        }
        List<BucketDO> bucketDOS = bucketMapper.getBucketsByItem(itemId);
        return FlashBucketMapping.INSTANCE.toBucketList(bucketDOS);
    }

    @Override
    public boolean suspendBuckets(Long itemId) {
        if (itemId == null) {
            return false;
        }
        bucketMapper.updateStatusByItem(itemId, BucketStatus.DISABLED.getCode());
        return true;
    }

    @Override
    public boolean resumeStockBuckets(Long itemId) {
        if (itemId == null) {
            return false;
        }
        bucketMapper.updateStatusByItem(itemId, BucketStatus.ENABLED.getCode());
        return true;
    }
}
