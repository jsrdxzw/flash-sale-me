package com.jsrdxzw.flashsale.app.service.placeorder.normal.cache;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.jsrdxzw.flashsale.app.service.stock.ItemStockCacheService;
import com.jsrdxzw.flashsale.app.service.stock.model.ItemStockCache;
import com.jsrdxzw.flashsale.cache.DistributedCacheService;
import com.jsrdxzw.flashsale.cache.redis.RedisCacheService;
import com.jsrdxzw.flashsale.domain.model.StockDeduction;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import com.jsrdxzw.flashsale.domain.service.FlashItemDomainService;
import com.jsrdxzw.flashsale.lock.DistributedLock;
import com.jsrdxzw.flashsale.lock.DistributedLockFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jsrdxzw.flashsale.util.StringHelper.link;

/**
 * 库存这边也可以采取版本号机制，这边可以容忍本地短暂的延时
 *
 * @author xuzhiwei
 * @date 2021/12/4 4:00 PM
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "place_order_type", havingValue = "normal", matchIfMissing = true)
public class NormalStockCacheService implements ItemStockCacheService {
    private static final String ITEM_STOCK_ALIGN_LOCK_KEY = "ITEM_STOCK_ALIGN_LOCK_KEY";
    private static final String ITEM_STOCKS_CACHE_KEY = "ITEM_STOCKS_CACHE_KEY";

    private static final int IN_STOCK_ALIGNING = -9;
    private static final String INIT_OR_ALIGN_ITEM_STOCK_LUA;
    private static final String INCREASE_ITEM_STOCK_LUA;
    private static final String DECREASE_ITEM_STOCK_LUA;
    private final static Cache<Long, ItemStockCache> itemStockLocalCache = CacheBuilder
            .newBuilder().initialCapacity(10)
            .concurrencyLevel(5)
            .expireAfterWrite(10, TimeUnit.SECONDS).build();

    static {
        INIT_OR_ALIGN_ITEM_STOCK_LUA = "if (redis.call('exists', KEYS[1]) == 1) then" +
                "    return -1;" +
                "end;" +
                "local stockNumber = tonumber(ARGV[1]);" +
                "redis.call('set', KEYS[1] , stockNumber);" +
                "return 1";

        INCREASE_ITEM_STOCK_LUA = "if (redis.call('exists', KEYS[2]) == 1) then" +
                "    return -9;" +
                "end;" +
                "if (redis.call('exists', KEYS[1]) == 1) then" +
                "    local stock = tonumber(redis.call('get', KEYS[1]));" +
                "    local num = tonumber(ARGV[1]);" +
                "    redis.call('incrby', KEYS[1] , num);" +
                "    return 1;" +
                "end;" +
                "return -1;";

        DECREASE_ITEM_STOCK_LUA = "if (redis.call('exists', KEYS[2]) == 1) then" +
                "    return -9;" +
                "end;" +
                "if (redis.call('exists', KEYS[1]) == 1) then" +
                "    local stock = tonumber(redis.call('get', KEYS[1]));" +
                "    local num = tonumber(ARGV[1]);" +
                "    if (stock < num) then" +
                "        return -3" +
                "    end;" +
                "    if (stock >= num) then" +
                "        redis.call('incrby', KEYS[1], 0 - num);" +
                "        return 1" +
                "    end;" +
                "    return -2;" +
                "end;" +
                "return -1;";
    }

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private FlashItemDomainService flashItemDomainService;

    @Autowired
    private DistributedLockFactoryService distributedLockFactoryService;

    @Autowired
    private DistributedCacheService distributedCacheService;

    @Override
    public boolean alignItemStocks(Long itemId) {
        if (itemId == null) {
            log.info("alignItemStocks|参数为空");
            return false;
        }
        DistributedLock distributedLock = distributedLockFactoryService.getDistributedLock(getItemStocksCacheAlignKey(itemId));
        try {
            boolean isLockSuccess = distributedLock.tryLock(5, 5, TimeUnit.SECONDS);
            if (!isLockSuccess) {
                log.info("alignItemStocks|校准库存时获取锁失败|{}", itemId);
                return false;
            }
            FlashItem flashItem = flashItemDomainService.getFlashItem(itemId);
            if (flashItem == null) {
                log.info("alignItemStocks|秒杀品不存在|{}", itemId);
                return false;
            }
            if (flashItem.getInitialStock() == null) {
                log.info("alignItemStocks|秒杀品未设置库存|{}", itemId);
                return false;
            }
            String key1ItemStocksCacheKey = getItemStocksCacheKey(itemId);
            List<String> keys = Lists.newArrayList(key1ItemStocksCacheKey);

            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(INIT_OR_ALIGN_ITEM_STOCK_LUA, Long.class);

            Long result = redisCacheService.getRedisTemplate().execute(redisScript, keys, flashItem.getAvailableStock());
            if (result == null) {
                log.info("alignItemStocks|秒杀品库存校准失败|{},{},{}", itemId, key1ItemStocksCacheKey, flashItem.getInitialStock());
                return false;
            }
            if (result == -1) {
                log.info("alignItemStocks|已在校准中，本次校准取消|{},{},{},{}", result, itemId, key1ItemStocksCacheKey, flashItem.getInitialStock());
                return true;
            }
            if (result == 1) {
                log.info("alignItemStocks|秒杀品库存校准完成|{},{},{},{}", result, itemId, key1ItemStocksCacheKey, flashItem.getInitialStock());
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            log.error("alignItemStocks|秒杀品库存校准错误|{}", itemId, e);
            return false;
        } finally {
            distributedLock.forceUnlock();
        }
    }

    @Override
    public boolean decreaseItemStock(StockDeduction stockDeduction) {
        log.info("decreaseItemStock|申请库存预扣减|{}", JSON.toJSONString(stockDeduction));
        if (stockDeduction == null || !stockDeduction.validate()) {
            return false;
        }
        try {
            String key1ItemStocksCacheKey = getItemStocksCacheKey(stockDeduction.getItemId());
            String key2ItemStocksCacheAlignKey = getItemStocksCacheAlignKey(stockDeduction.getItemId());
            List<String> keys = Lists.newArrayList(key1ItemStocksCacheKey, key2ItemStocksCacheAlignKey);
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(DECREASE_ITEM_STOCK_LUA, Long.class);
            Long result = null;
            long startTime = System.currentTimeMillis();
            while ((result == null || result == IN_STOCK_ALIGNING) && (System.currentTimeMillis() - startTime) < 1500) {
                result = redisCacheService.getRedisTemplate().execute(redisScript, keys, stockDeduction.getQuantity());
                if (result == null) {
                    log.info("decreaseItemStock|库存扣减失败|{}", key1ItemStocksCacheKey);
                    return false;
                }
                if (result == IN_STOCK_ALIGNING) {
                    log.info("decreaseItemStock|库存校准中|{}", key1ItemStocksCacheKey);
                    Thread.yield();
                }
                if (result == -1 || result == -2) {
                    log.info("decreaseItemStock|库存扣减失败|{}", key1ItemStocksCacheKey);
                    return false;
                }
                if (result == -3) {
                    log.info("decreaseItemStock|库存扣减失败|{}", key1ItemStocksCacheKey);
                    return false;
                }
                if (result == 1) {
                    log.info("decreaseItemStock|库存扣减成功|{}", key1ItemStocksCacheKey);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("decreaseItemStock|库存扣减失败", e);
            return false;
        }
        return false;
    }

    @Override
    public boolean increaseItemStock(StockDeduction stockDeduction) {
        if (stockDeduction == null || !stockDeduction.validate()) {
            return false;
        }
        try {
            String key1ItemStocksCacheKey = getItemStocksCacheKey(stockDeduction.getItemId());
            String key2ItemStocksCacheAlignKey = getItemStocksCacheAlignKey(stockDeduction.getItemId());
            List<String> keys = Lists.newArrayList(key1ItemStocksCacheKey, key2ItemStocksCacheAlignKey);

            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(INCREASE_ITEM_STOCK_LUA, Long.class);
            Long result = null;
            long startTime = System.currentTimeMillis();
            while ((result == null || result == IN_STOCK_ALIGNING) && (System.currentTimeMillis() - startTime) < 1500) {
                result = redisCacheService.getRedisTemplate().execute(redisScript, keys, stockDeduction.getQuantity());
                if (result == null) {
                    log.info("increaseItemStock|库存增加失败|{}", key1ItemStocksCacheKey);
                    return false;
                }
                if (result == IN_STOCK_ALIGNING) {
                    log.info("increaseItemStock|库存校准中|{}", key1ItemStocksCacheKey);
                    Thread.yield();
                }
                if (result == -1) {
                    log.info("increaseItemStock|库存增加失败|{}", key1ItemStocksCacheKey);
                    return false;
                }
                if (result == 1) {
                    log.info("increaseItemStock|库存增加成功|{}", key1ItemStocksCacheKey);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("increaseItemStock|库存增加失败", e);
            return false;
        }
        return false;
    }

    @Override
    public ItemStockCache getAvailableItemStock(Long userId, Long itemId) {
        ItemStockCache itemStockCache = itemStockLocalCache.getIfPresent(itemId);
        if (itemStockCache != null) {
            return itemStockCache;
        }
        // 无需到数据库，缓存没有则没有, 缓存的库存没有过期时间
        Integer availableStock = distributedCacheService.getObject(getItemStocksCacheKey(itemId), Integer.class);
        if (availableStock == null) {
            return null;
        }
        itemStockCache = new ItemStockCache().with(availableStock);
        itemStockLocalCache.put(itemId, itemStockCache);
        return itemStockCache;
    }

    public static String getItemStocksCacheAlignKey(Long itemId) {
        return link(ITEM_STOCK_ALIGN_LOCK_KEY, itemId);
    }

    public static String getItemStocksCacheKey(Long itemId) {
        return link(ITEM_STOCKS_CACHE_KEY, itemId);
    }
}
