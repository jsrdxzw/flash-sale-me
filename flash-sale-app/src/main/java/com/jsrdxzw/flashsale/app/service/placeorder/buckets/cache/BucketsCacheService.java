package com.jsrdxzw.flashsale.app.service.placeorder.buckets.cache;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.jsrdxzw.flashsale.app.service.stock.ItemStockCacheService;
import com.jsrdxzw.flashsale.app.service.stock.model.ItemStockCache;
import com.jsrdxzw.flashsale.cache.DistributedCacheService;
import com.jsrdxzw.flashsale.cache.redis.RedisCacheService;
import com.jsrdxzw.flashsale.domain.model.Bucket;
import com.jsrdxzw.flashsale.domain.model.StockDeduction;
import com.jsrdxzw.flashsale.domain.service.BucketsDomainService;
import com.jsrdxzw.flashsale.lock.DistributedLock;
import com.jsrdxzw.flashsale.lock.DistributedLockFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.ITEM_BUCKETS_CACHE_INIT_KEY;
import static com.jsrdxzw.flashsale.app.service.bucket.DefaultBucketsArrangementService.*;
import static com.jsrdxzw.flashsale.app.service.placeorder.normal.cache.NormalStockCacheService.getItemStocksCacheAlignKey;
import static com.jsrdxzw.flashsale.util.StringHelper.link;

/**
 * @author xuzhiwei
 * @date 2021/12/13 2:18 PM
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
public class BucketsCacheService implements ItemStockCacheService {
    private static final String INIT_OR_ALIGN_ITEM_STOCK_LUA;
    private static final String INCREASE_ITEM_STOCK_LUA;
    private static final String DECREASE_ITEM_STOCK_LUA;
    private final static Cache<String, Integer> bucketAvailableStocksLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(1000).concurrencyLevel(5).expireAfterWrite(100, TimeUnit.MILLISECONDS).build();
    private final static Cache<Long, Integer> itemBucketsQuantityLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(10).concurrencyLevel(5).expireAfterWrite(10, TimeUnit.SECONDS).build();

    static {
        INIT_OR_ALIGN_ITEM_STOCK_LUA = "if (redis.call('exists', KEYS[2]) == 1) then" +
                "    return -998;" +
                "end;" +
                "if (redis.call('exists', KEYS[3]) == 1) then" +
                "    return -997;" +
                "end;" +
                "redis.call('set', KEYS[3] , 1);" +
                "local stocksAmount = tonumber(ARGV[1]);" +
                "local bucketsQuantity = tonumber(ARGV[2]);" +
                "redis.call('set', KEYS[1] , stocksAmount);" +
                "redis.call('set', KEYS[4] , bucketsQuantity);" +
                "redis.call('del', KEYS[3]);" +
                "return 1";
        INCREASE_ITEM_STOCK_LUA = "if (redis.call('exists', KEYS[1]) == 0) then" +
                "    return -996;" +
                "end;" +
                "if (redis.call('exists', KEYS[2]) == 1) then" +
                "    return -998;" +
                "end;" +
                "if (redis.call('exists', KEYS[3]) == 1) then" +
                "    return -997;" +
                "end;" +
                "if (redis.call('exists', KEYS1]) == 1) then" +
                "    local quantity = tonumber(ARGV[1]);" +
                "    redis.call('incrby', KEYS[1] , quantity);" +
                "    return 1;" +
                "end;" +
                "return -10000;";
        DECREASE_ITEM_STOCK_LUA = "if (redis.call('exists', KEYS[1]) == 0) then" +
                "    return -996;" +
                "end;" +
                "if (redis.call('exists', KEYS[2]) == 1) then" +
                "    return -998;" +
                "end;" +
                "if (redis.call('exists', KEYS[3]) == 1) then" +
                "    return -997;" +
                "end;" +
                "if (redis.call('exists', KEYS[1]) == 1) then" +
                "    local stocksAmount = tonumber(redis.call('get', KEYS[1]));" +
                "    local quantity = tonumber(ARGV[1]);" +
                "    if (stocksAmount < quantity) then" +
                "        return -1" +
                "    end;" +
                "    if (stocksAmount >= quantity) then" +
                "        redis.call('incrby', KEYS[1], 0 - quantity);" +
                "        return 1" +
                "    end;" +
                "end;" +
                "return -10000;";
    }

    @Resource
    private RedisCacheService redisCacheService;
    @Resource
    private BucketsDomainService bucketsDomainService;
    @Resource
    private DistributedLockFactoryService distributedLockFactoryService;
    @Resource
    private DistributedCacheService distributedCacheService;

    @Override
    public boolean alignItemStocks(Long itemId) {
        if (itemId == null) {
            log.info("alignItemStocks|参数为空");
            return false;
        }
        String stockBucketCacheInitLockKey = getStockBucketCacheInitLockKey(itemId);
        DistributedLock lock = distributedLockFactoryService.getDistributedLock(stockBucketCacheInitLockKey);
        try {
            boolean isLockSuccess = lock.tryLock(5, 5, TimeUnit.SECONDS);
            if (!isLockSuccess) {
                log.info("alignItemStocks|校准库存时获取锁失败|{}", itemId);
                return false;
            }
            List<Bucket> buckets = bucketsDomainService.getBucketsByItem(itemId);
            if (CollectionUtils.isEmpty(buckets)) {
                log.info("alignItemStocks|秒杀品未设置库存|{}", itemId);
                return false;
            }
            buckets.forEach(stockBucket -> {
                String key1StockBucketCacheKey = getBucketAvailableStocksCacheKey(stockBucket.getItemId(), stockBucket.getSerialNo());
                String key2StockBucketsSuspendKey = getItemStockBucketsSuspendKey(stockBucket.getItemId());
                String key3ItemStocksCacheAlignKey = getItemStocksCacheAlignKey(stockBucket.getItemId());
                String key4ItemStockBucketsQuantityCacheKey = getItemStockBucketsQuantityCacheKey(stockBucket.getItemId());
                List<String> keys = Lists.newArrayList(key1StockBucketCacheKey, key2StockBucketsSuspendKey, key3ItemStocksCacheAlignKey, key4ItemStockBucketsQuantityCacheKey);
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(INIT_OR_ALIGN_ITEM_STOCK_LUA, Long.class);
                Long result = redisCacheService.getRedisTemplate().execute(redisScript, keys, stockBucket.getAvailableStocksAmount(), buckets.size());
                if (result == null) {
                    log.info("alignItemStocks|分桶库存校准失败|{},{}", itemId, stockBucketCacheInitLockKey);
                    return;
                }
                if (result == -998) {
                    log.info("alignItemStocks|库存维护中，已暂停服务|{},{},{}", result, itemId, stockBucketCacheInitLockKey);
                    return;
                }
                if (result == -997) {
                    log.info("alignItemStocks|库存数据校准对齐中|{},{},{}", result, itemId, stockBucketCacheInitLockKey);
                    return;
                }
                if (result == 1) {
                    log.info("alignItemStocks|分桶库存校准完成|{},{},{}", result, itemId, stockBucketCacheInitLockKey);
                }
            });
            log.info("alignItemStocks|分桶库存校准全部完成|{},{}", itemId, stockBucketCacheInitLockKey);
            return true;
        } catch (Exception e) {
            log.error("alignItemStocks|秒杀品库存初始化错误|{},{}", itemId, stockBucketCacheInitLockKey, e);
            return false;
        } finally {
            lock.forceUnlock();
        }
    }

    @Override
    public boolean decreaseItemStock(StockDeduction stockDeduction) {
        log.info("decreaseItemStock|申请库存预扣减{}", JSON.toJSONString(stockDeduction));
        if (stockDeduction == null || !stockDeduction.validate()) {
            return false;
        }
        try {
            Integer subBucketsQuantity = getSubBucketsQuantity(stockDeduction.getItemId());
            if (subBucketsQuantity == null) {
                return false;
            }
            Integer targetBucketSerialNo = getTargetBucketSerialNo(stockDeduction.getUserId(), subBucketsQuantity);
            stockDeduction.setSerialNo(targetBucketSerialNo);

            String key1StockBucketCacheKey = getBucketAvailableStocksCacheKey(stockDeduction.getItemId(), targetBucketSerialNo);
            String key2StockBucketsSuspendKey = getItemStockBucketsSuspendKey(stockDeduction.getItemId());
            String key3ItemStocksAlignKey = getItemStocksCacheAlignKey(stockDeduction.getItemId());

            List<String> keys = Lists.newArrayList(key1StockBucketCacheKey, key2StockBucketsSuspendKey, key3ItemStocksAlignKey);

            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(DECREASE_ITEM_STOCK_LUA, Long.class);
            Long result = redisCacheService.getRedisTemplate().execute(redisScript, keys, stockDeduction.getQuantity());
            if (result == null || result == -996) {
                log.info("decreaseItemStock|分桶库存不存在|{},{}", targetBucketSerialNo, key1StockBucketCacheKey);
                return false;
            }
            if (result == -998) {
                log.info("decreaseItemStock|库存维护中，已暂停服务|{},{}", result, key1StockBucketCacheKey);
                return false;
            }
            if (result == -997) {
                log.info("decreaseItemStock|库存数据校准对齐中|{},{}", result, key1StockBucketCacheKey);
                return false;
            }
            if (result == -1) {
                log.info("decreaseItemStock|库存不足|{},{}", result, key1StockBucketCacheKey);
                return false;
            }
            if (result == 1) {
                log.info("decreaseItemStock|库存扣减成功|{},{}", result, key1StockBucketCacheKey);
                return true;
            }
            log.info("decreaseItemStock|库存扣减失败|{},{}", result, key1StockBucketCacheKey);
            return false;
        } catch (Exception e) {
            log.error("decreaseItemStock|库存扣减失败", e);
            return false;
        }
    }

    @Override
    public boolean increaseItemStock(StockDeduction stockDeduction) {
        log.info("increaseItemStock|恢复预扣减库存|{}", JSON.toJSONString(stockDeduction));
        if (stockDeduction == null || !stockDeduction.validate()) {
            return false;
        }
        try {
            Integer subBucketsQuantity = getSubBucketsQuantity(stockDeduction.getItemId());
            if (subBucketsQuantity == null) {
                return false;
            }
            Integer targetBucketSerialNo = getTargetBucketSerialNo(stockDeduction.getUserId(), subBucketsQuantity);
            String key1StockBucketCacheKey = getBucketAvailableStocksCacheKey(stockDeduction.getItemId(), targetBucketSerialNo);
            String key2StockBucketsSuspendKey = getItemStockBucketsSuspendKey(stockDeduction.getItemId());
            String key3ItemStocksAlignKey = getItemStocksCacheAlignKey(stockDeduction.getItemId());

            List<String> keys = Lists.newArrayList(key1StockBucketCacheKey, key2StockBucketsSuspendKey, key3ItemStocksAlignKey);

            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(INCREASE_ITEM_STOCK_LUA, Long.class);
            Long result = redisCacheService.getRedisTemplate().execute(redisScript, keys, stockDeduction.getQuantity());
            if (result == null || result == -996) {
                log.info("increaseItemStock|分桶库存不存在|{},{},{}", stockDeduction.getItemId(), targetBucketSerialNo, key1StockBucketCacheKey);
                return false;
            }
            if (result == -998) {
                log.info("increaseItemStock|库存维护中，已暂停服务|{},{},{}", result, stockDeduction.getItemId(), key1StockBucketCacheKey);
                return false;
            }
            if (result == -997) {
                log.info("increaseItemStock|库存数据校准对齐中|{},{}", result, key1StockBucketCacheKey);
                return false;
            }
            if (result == 1) {
                log.info("increaseItemStock|库存恢复成功|{},{}", result, key1StockBucketCacheKey);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("increaseItemStock|库存恢复失败|{}", e);
            return false;
        }
    }

    @Override
    public ItemStockCache getAvailableItemStock(Long userId, Long itemId) {
        Integer subBucketsQuantity = getSubBucketsQuantity(itemId);
        if (subBucketsQuantity == null) {
            return null;
        }
        Integer targetBucketSerialNo = getTargetBucketSerialNo(userId, subBucketsQuantity);
        String bucketCacheKey = getBucketAvailableStocksCacheKey(itemId, targetBucketSerialNo);
        Integer availableBucketStocks = bucketAvailableStocksLocalCache.getIfPresent(bucketCacheKey);
        if (availableBucketStocks == null) {
            availableBucketStocks = distributedCacheService.getObject(bucketCacheKey, Integer.class);
        }
        return new ItemStockCache().with(availableBucketStocks);
    }

    private Integer getTargetBucketSerialNo(Long userId, Integer bucketsQuantity) {
        if (userId == null || bucketsQuantity == null || bucketsQuantity <= 0) {
            return null;
        }
        if (bucketsQuantity == 1) {
            return 0;
        }
        return userId.hashCode() % bucketsQuantity;
    }

    private Integer getSubBucketsQuantity(Long itemId) {
        Integer subBucketsQuantity = itemBucketsQuantityLocalCache.getIfPresent(itemId);
        if (subBucketsQuantity != null) {
            return subBucketsQuantity;
        }
        subBucketsQuantity = distributedCacheService.getObject(getItemStockBucketsQuantityCacheKey(itemId), Integer.class);
        if (subBucketsQuantity != null) {
            itemBucketsQuantityLocalCache.put(itemId, subBucketsQuantity);
        }
        return subBucketsQuantity;
    }

    private String getStockBucketCacheInitLockKey(Long itemId) {
        return link(ITEM_BUCKETS_CACHE_INIT_KEY, itemId);
    }
}
