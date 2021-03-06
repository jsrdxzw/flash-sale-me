package com.jsrdxzw.flashsale.app.service.bucket;

import com.jsrdxzw.flashsale.app.exception.StockBucketException;
import com.jsrdxzw.flashsale.app.model.converter.FlashBucketAppMapping;
import com.jsrdxzw.flashsale.app.model.dto.StockBucketDTO;
import com.jsrdxzw.flashsale.app.model.dto.StockBucketSummaryDTO;
import com.jsrdxzw.flashsale.app.model.enums.ArrangementMode;
import com.jsrdxzw.flashsale.cache.DistributedCacheService;
import com.jsrdxzw.flashsale.domain.model.Bucket;
import com.jsrdxzw.flashsale.domain.model.enums.BucketStatus;
import com.jsrdxzw.flashsale.domain.service.BucketsDomainService;
import com.jsrdxzw.flashsale.lock.DistributedLock;
import com.jsrdxzw.flashsale.lock.DistributedLockFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.*;
import static com.jsrdxzw.flashsale.util.StringHelper.link;

/**
 * @author xuzhiwei
 * @date 2021/12/12 10:13 PM
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
public class DefaultBucketsArrangementService implements BucketsArrangementService {

    @Resource
    private DistributedLockFactoryService lockFactoryService;
    @Resource
    private DataSourceTransactionManager dataSourceTransactionManager;
    @Resource
    private TransactionDefinition transactionDefinition;
    @Resource
    private BucketsDomainService bucketsDomainService;
    @Resource
    private DistributedCacheService distributedCacheService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void arrangeStockBuckets(Long itemId, Integer stocksAmount, Integer bucketsQuantity, Integer assignmentMode) {
        log.info("arrangeBuckets|??????????????????|{},{},{}", itemId, stocksAmount, bucketsQuantity);
        if (itemId == null || stocksAmount == null || stocksAmount < 0 || bucketsQuantity == null || bucketsQuantity <= 0) {
            throw new StockBucketException("????????????");
        }
        DistributedLock lock = lockFactoryService.getDistributedLock(ITEM_STOCK_BUCKETS_SUSPEND_KEY + itemId);
        try {
            boolean isLockSuccess = lock.tryLock(5, 5, TimeUnit.SECONDS);
            if (!isLockSuccess) {
                log.info("arrangeBuckets|??????????????????????????????|{}", itemId);
                return;
            }
            // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            try {
                boolean success = bucketsDomainService.suspendBuckets(itemId);
                if (!success) {
                    log.info("arrangeBuckets|????????????????????????|{}", itemId);
                    throw new StockBucketException("????????????????????????");
                }
                dataSourceTransactionManager.commit(transactionStatus);
            } catch (Exception e) {
                log.info("arrangeBuckets|???????????????????????????|{}", itemId, e);
                dataSourceTransactionManager.rollback(transactionStatus);
            }
            List<Bucket> buckets = bucketsDomainService.getBucketsByItem(itemId);
            if (buckets.size() == 0) {
                initStockBuckets(itemId, stocksAmount, bucketsQuantity);
                return;
            }
            // ??????????????????
            // ???????????????????????????????????????????????????????????????????????????????????????????????????
            // ??????????????????????????????????????????????????????????????????????????????????????????????????????
            if (ArrangementMode.isTotalAmountMode(assignmentMode)) {
                arrangeStockBucketsBasedTotalMode(itemId, stocksAmount, bucketsQuantity, buckets);
            }
            if (ArrangementMode.isIncrementalAmountMode(assignmentMode)) {
                rearrangeStockBucketsBasedIncrementalMode(itemId, stocksAmount, bucketsQuantity, buckets);
            }
        } catch (Exception e) {
            log.error("arrangeBuckets|??????????????????|", e);
            throw new StockBucketException("??????????????????");
        } finally {
            boolean success = bucketsDomainService.resumeBuckets(itemId);
            if (!success) {
                log.error("arrangeBuckets|????????????????????????|");
            }
            lock.forceUnlock();
        }
    }

    @Override
    public StockBucketSummaryDTO queryStockBucketsSummary(Long itemId) {
        if (itemId == null) {
            throw new StockBucketException("????????????");
        }
        List<Bucket> buckets = bucketsDomainService.getBucketsByItem(itemId);
        int remainAvailableStocks = buckets.stream().mapToInt(Bucket::getAvailableStocksAmount).sum();
        Optional<Bucket> primaryBucketOptional = buckets.stream().filter(Bucket::isPrimaryBucket).findFirst();
        if (primaryBucketOptional.isEmpty()) {
            return new StockBucketSummaryDTO();
        }
        List<StockBucketDTO> subBuckets = FlashBucketAppMapping.INSTANCE.toDTOList(buckets);
        return new StockBucketSummaryDTO()
                .setTotalStocksAmount(primaryBucketOptional.get().getTotalStocksAmount())
                .setAvailableStocksAmount(remainAvailableStocks)
                .setBuckets(subBuckets);
    }

    private void initStockBuckets(Long itemId, Integer totalStockAmount, Integer bucketsQuantity) {
        Bucket primaryBucket = new Bucket()
                .initPrimary()
                .setTotalStocksAmount(totalStockAmount)
                .setItemId(itemId);
        List<Bucket> presentBuckets = buildBuckets(itemId, totalStockAmount, bucketsQuantity, primaryBucket);
        submitBucketsToArrange(itemId, presentBuckets);
    }

    private List<Bucket> buildBuckets(Long itemId, Integer availableStocksAmount, Integer bucketsQuantity, Bucket primaryBucket) {
        if (itemId == null || availableStocksAmount == null || bucketsQuantity == null || bucketsQuantity <= 0) {
            throw new StockBucketException("???????????????????????????");
        }
        List<Bucket> buckets = new ArrayList<>();
        int averageStocksInEachBucket = availableStocksAmount / bucketsQuantity;
        int pieceStocks = availableStocksAmount % bucketsQuantity;

        for (int i = 0; i < bucketsQuantity; i++) {
            if (i == 0) {
                if (primaryBucket == null) {
                    primaryBucket = new Bucket();
                }
                primaryBucket.setSerialNo(i);
                primaryBucket.setAvailableStocksAmount(averageStocksInEachBucket);
                primaryBucket.setStatus(BucketStatus.ENABLED.getCode());
                buckets.add(primaryBucket);
                continue;
            }
            Bucket subBucket = new Bucket()
                    .setStatus(BucketStatus.ENABLED.getCode())
                    .setSerialNo(i)
                    .setItemId(itemId);
            if (i < bucketsQuantity - 1) {
                subBucket.setTotalStocksAmount(averageStocksInEachBucket);
                subBucket.setAvailableStocksAmount(averageStocksInEachBucket);
            }
            // ????????????????????????????????????
            if (i == bucketsQuantity - 1) {
                Integer restAvailableStocksAmount = averageStocksInEachBucket + pieceStocks;
                subBucket.setTotalStocksAmount(restAvailableStocksAmount);
                subBucket.setAvailableStocksAmount(restAvailableStocksAmount);
            }
            buckets.add(subBucket);
        }
        return buckets;
    }

    private void submitBucketsToArrange(Long itemId, List<Bucket> presentBuckets) {
        boolean result = bucketsDomainService.arrangeBuckets(itemId, presentBuckets);
        if (result) {
            presentBuckets.forEach(bucket -> distributedCacheService.put(getBucketAvailableStocksCacheKey(itemId, bucket.getSerialNo()), bucket.getAvailableStocksAmount()));
            distributedCacheService.put(getItemStockBucketsQuantityCacheKey(itemId), presentBuckets.size());
        } else {
            throw new StockBucketException("??????????????????");
        }
    }

    /**
     * ??????????????????????????????
     */
    private void arrangeStockBucketsBasedTotalMode(Long itemId, Integer totalStockAmount, Integer bucketsAmount, List<Bucket> existingBuckets) {
        // ????????????
        int remainAvailableStocks = existingBuckets.stream()
                .filter(Bucket::isSubBucket)
                .mapToInt(Bucket::getAvailableStocksAmount).sum();
        Optional<Bucket> primaryBucketOptional = existingBuckets.stream().filter(Bucket::isPrimaryBucket).findFirst();
        if (primaryBucketOptional.isEmpty()) {
            return;
        }
        // ???????????????????????????
        Bucket primaryBucket = primaryBucketOptional.get();
        primaryBucket.addAvailableStocks(remainAvailableStocks);
        int soldStocksAmount = primaryBucket.getTotalStocksAmount() - primaryBucket.getAvailableStocksAmount();
        if (soldStocksAmount > totalStockAmount) {
            throw new StockBucketException("?????????????????????????????????????????????");
        }
        primaryBucket.setTotalStocksAmount(totalStockAmount);

        List<Bucket> presentBuckets = buildBuckets(itemId, totalStockAmount, bucketsAmount, primaryBucket);
        submitBucketsToArrange(itemId, presentBuckets);
    }

    /**
     * ??????????????????????????????
     */
    private void rearrangeStockBucketsBasedIncrementalMode(
            Long itemId, Integer incrementalStocksAmount, Integer bucketsAmount, List<Bucket> buckets) {
        Optional<Bucket> primaryStockBucketOptional = buckets.stream().filter(Bucket::isPrimaryBucket).findFirst();
        if (primaryStockBucketOptional.isEmpty()) {
            return;
        }
        // ??????????????????
        int remainAvailableStocks = buckets.stream().mapToInt(Bucket::getAvailableStocksAmount).sum();
        Integer totalAvailableStocksAmount = remainAvailableStocks + incrementalStocksAmount;
        int presentAvailableStocks = remainAvailableStocks + incrementalStocksAmount;
        if (presentAvailableStocks < 0) {
            throw new StockBucketException("?????????????????????");
        }

        Bucket primaryBucket = primaryStockBucketOptional.get();
        primaryBucket.increaseTotalStocksAmount(incrementalStocksAmount);

        List<Bucket> presentBuckets = buildBuckets(itemId, totalAvailableStocksAmount, bucketsAmount, primaryBucket);
        submitBucketsToArrange(itemId, presentBuckets);
    }

    public static String getBucketAvailableStocksCacheKey(Long itemId, Integer serialNumber) {
        return link(ITEM_BUCKET_AVAILABLE_STOCKS_KEY, itemId, serialNumber);
    }

    public static String getItemStockBucketsSuspendKey(Long itemId) {
        return link(ITEM_STOCK_BUCKETS_SUSPEND_KEY, itemId);
    }

    public static String getItemStockBucketsQuantityCacheKey(Long itemId) {
        return link(ITEM_BUCKETS_QUANTITY_KEY, itemId);
    }
}
