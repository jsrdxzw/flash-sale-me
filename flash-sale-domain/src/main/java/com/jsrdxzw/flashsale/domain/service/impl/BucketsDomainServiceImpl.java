package com.jsrdxzw.flashsale.domain.service.impl;

import com.jsrdxzw.flashsale.domain.event.DomainEventPublisher;
import com.jsrdxzw.flashsale.domain.event.StockBucketEvent;
import com.jsrdxzw.flashsale.domain.event.StockBucketEventType;
import com.jsrdxzw.flashsale.domain.exception.DomainException;
import com.jsrdxzw.flashsale.domain.model.Bucket;
import com.jsrdxzw.flashsale.domain.repository.BucketsRepository;
import com.jsrdxzw.flashsale.domain.service.BucketsDomainService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

import static com.jsrdxzw.flashsale.domain.exception.DomainErrorCode.*;

/**
 * @author xuzhiwei
 * @date 2021/12/13 10:13 AM
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
public class BucketsDomainServiceImpl implements BucketsDomainService {
    @Resource
    private BucketsRepository bucketsRepository;
    @Resource
    private DomainEventPublisher domainEventPublisher;

    @Override
    public boolean suspendBuckets(Long itemId) {
        log.info("suspendBuckets|禁用库存分桶|{}", itemId);
        if (itemId == null || itemId <= 0) {
            throw new DomainException(PARAMS_INVALID);
        }
        boolean success = bucketsRepository.suspendBuckets(itemId);
        if (!success) {
            return false;
        }
        StockBucketEvent stockBucketEvent = new StockBucketEvent();
        stockBucketEvent.setEventType(StockBucketEventType.DISABLED);
        stockBucketEvent.setItemId(itemId);
        domainEventPublisher.publish(stockBucketEvent);
        log.info("suspendBuckets|库存分桶已禁用|{}", itemId);
        return true;
    }

    @Override
    public List<Bucket> getBucketsByItem(Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new DomainException(PARAMS_INVALID);
        }
        return bucketsRepository.getBucketsByItem(itemId);
    }

    @Override
    public boolean arrangeBuckets(Long itemId, List<Bucket> buckets) {
        log.info("arrangeBuckets|编排库存分桶|{},{}", itemId, JSONUtil.toJSONString(buckets));
        if (itemId == null || itemId <= 0 || CollectionUtils.isEmpty(buckets)) {
            log.info("arrangeBuckets|库存分桶参数错误|{}", itemId);
            throw new DomainException(PARAMS_INVALID);
        }
        Optional<Bucket> primaryBucketOptional = buckets.stream().filter(Bucket::isPrimaryBucket).findFirst();
        if (primaryBucketOptional.isEmpty()) {
            throw new DomainException(PRIMARY_BUCKET_IS_MISSING);
        }
        if (buckets.stream().filter(Bucket::isPrimaryBucket).count() > 1) {
            throw new DomainException(MULTI_PRIMARY_BUCKETS_FOUND_BUT_EXPECT_ONE);
        }
        buckets.forEach(stockBucket -> {
            if (stockBucket.getTotalStocksAmount() == null || stockBucket.getTotalStocksAmount() < 0) {
                throw new DomainException(TOTAL_STOCKS_AMOUNT_INVALID);
            }
            if (stockBucket.getAvailableStocksAmount() == null || stockBucket.getAvailableStocksAmount() <= 0) {
                throw new DomainException(AVAILABLE_STOCKS_AMOUNT_INVALID);
            }
            if (!stockBucket.getAvailableStocksAmount().equals(stockBucket.getTotalStocksAmount()) && stockBucket.isSubBucket()) {
                throw new DomainException(AVAILABLE_STOCKS_AMOUNT_NOT_EQUALS_TO_TOTAL_STOCKS_AMOUNT);
            }
            if (!itemId.equals(stockBucket.getItemId())) {
                throw new DomainException(STOCK_BUCKET_ITEM_INVALID);
            }
        });

        boolean success = bucketsRepository.submitBuckets(itemId, buckets);
        if (!success) {
            return false;
        }
        StockBucketEvent stockBucketEvent = new StockBucketEvent();
        stockBucketEvent.setEventType(StockBucketEventType.ARRANGED);
        stockBucketEvent.setItemId(itemId);
        domainEventPublisher.publish(stockBucketEvent);
        log.info("arrangeBuckets|编排库存分桶已完成|{}", itemId);
        return true;
    }

    @Override
    public boolean resumeBuckets(Long itemId) {
        log.info("resumeBuckets|启用库存分桶|{}", itemId);
        if (itemId == null || itemId <= 0) {
            throw new DomainException(PARAMS_INVALID);
        }
        boolean success = bucketsRepository.resumeStockBuckets(itemId);
        if (!success) {
            return false;
        }
        StockBucketEvent stockBucketEvent = new StockBucketEvent();
        stockBucketEvent.setEventType(StockBucketEventType.ENABLED);
        stockBucketEvent.setItemId(itemId);
        domainEventPublisher.publish(stockBucketEvent);
        log.info("suspendBuckets|库存桶已禁用|{}", itemId);
        return true;
    }
}
