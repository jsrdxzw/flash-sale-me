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
        log.info("arrangeBuckets|准备库存分桶|{},{},{}", itemId, stocksAmount, bucketsQuantity);
        if (itemId == null || stocksAmount == null || stocksAmount < 0 || bucketsQuantity == null || bucketsQuantity <= 0) {
            throw new StockBucketException("参数错误");
        }
        DistributedLock lock = lockFactoryService.getDistributedLock(ITEM_STOCK_BUCKETS_SUSPEND_KEY + itemId);
        try {
            boolean isLockSuccess = lock.tryLock(5, 5, TimeUnit.SECONDS);
            if (!isLockSuccess) {
                log.info("arrangeBuckets|库存分桶时获取锁失败|{}", itemId);
                return;
            }
            // 暂停分桶服务时，必须使用独立事务手动提交，确保在继续执行分桶前，分桶状态已经提交到数据库
            TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            try {
                boolean success = bucketsDomainService.suspendBuckets(itemId);
                if (!success) {
                    log.info("arrangeBuckets|关闭库存分桶失败|{}", itemId);
                    throw new StockBucketException("关闭库存分桶失败");
                }
                dataSourceTransactionManager.commit(transactionStatus);
            } catch (Exception e) {
                log.info("arrangeBuckets|关闭分桶失败回滚中|{}", itemId, e);
                dataSourceTransactionManager.rollback(transactionStatus);
            }
            List<Bucket> buckets = bucketsDomainService.getBucketsByItem(itemId);
            if (buckets.size() == 0) {
                initStockBuckets(itemId, stocksAmount, bucketsQuantity);
                return;
            }
            // 主桶存总库存
            // 全量分桶意味着将当前传入的库存总量作为最终总量，重新计算分桶数据；
            // 而增量则是将传入的库存总量累加到已有的库存中，然后再重新计算分桶数据
            if (ArrangementMode.isTotalAmountMode(assignmentMode)) {
                arrangeStockBucketsBasedTotalMode(itemId, stocksAmount, bucketsQuantity, buckets);
            }
            if (ArrangementMode.isIncrementalAmountMode(assignmentMode)) {
                rearrangeStockBucketsBasedIncrementalMode(itemId, stocksAmount, bucketsQuantity, buckets);
            }
        } catch (Exception e) {
            log.error("arrangeBuckets|库存分桶错误|", e);
            throw new StockBucketException("库存分桶错误");
        } finally {
            boolean success = bucketsDomainService.resumeBuckets(itemId);
            if (!success) {
                log.error("arrangeBuckets|打开库存分桶失败|");
            }
            lock.forceUnlock();
        }
    }

    @Override
    public StockBucketSummaryDTO queryStockBucketsSummary(Long itemId) {
        if (itemId == null) {
            throw new StockBucketException("参数错误");
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
            throw new StockBucketException("构建分桶时参数错误");
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
            // 最后一个桶需要把余数加上
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
            throw new StockBucketException("库存分桶失败");
        }
    }

    /**
     * 根据总量库存重新分桶
     */
    private void arrangeStockBucketsBasedTotalMode(Long itemId, Integer totalStockAmount, Integer bucketsAmount, List<Bucket> existingBuckets) {
        // 重新分桶
        int remainAvailableStocks = existingBuckets.stream()
                .filter(Bucket::isSubBucket)
                .mapToInt(Bucket::getAvailableStocksAmount).sum();
        Optional<Bucket> primaryBucketOptional = existingBuckets.stream().filter(Bucket::isPrimaryBucket).findFirst();
        if (primaryBucketOptional.isEmpty()) {
            return;
        }
        // 回收分桶库存到主桶
        Bucket primaryBucket = primaryBucketOptional.get();
        primaryBucket.addAvailableStocks(remainAvailableStocks);
        int soldStocksAmount = primaryBucket.getTotalStocksAmount() - primaryBucket.getAvailableStocksAmount();
        if (soldStocksAmount > totalStockAmount) {
            throw new StockBucketException("已售库存大于当期所设库存总量！");
        }
        primaryBucket.setTotalStocksAmount(totalStockAmount);

        List<Bucket> presentBuckets = buildBuckets(itemId, totalStockAmount, bucketsAmount, primaryBucket);
        submitBucketsToArrange(itemId, presentBuckets);
    }

    /**
     * 根据增量库存重新分桶
     */
    private void rearrangeStockBucketsBasedIncrementalMode(
            Long itemId, Integer incrementalStocksAmount, Integer bucketsAmount, List<Bucket> buckets) {
        Optional<Bucket> primaryStockBucketOptional = buckets.stream().filter(Bucket::isPrimaryBucket).findFirst();
        if (primaryStockBucketOptional.isEmpty()) {
            return;
        }
        // 回收分桶库存
        int remainAvailableStocks = buckets.stream().mapToInt(Bucket::getAvailableStocksAmount).sum();
        Integer totalAvailableStocksAmount = remainAvailableStocks + incrementalStocksAmount;
        int presentAvailableStocks = remainAvailableStocks + incrementalStocksAmount;
        if (presentAvailableStocks < 0) {
            throw new StockBucketException("可用库存不足！");
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
