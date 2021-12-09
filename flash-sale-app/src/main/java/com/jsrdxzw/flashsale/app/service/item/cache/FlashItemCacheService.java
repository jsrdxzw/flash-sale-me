package com.jsrdxzw.flashsale.app.service.item.cache;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jsrdxzw.flashsale.app.service.item.cache.model.FlashItemCache;
import com.jsrdxzw.flashsale.cache.DistributedCacheService;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import com.jsrdxzw.flashsale.domain.service.FlashItemDomainService;
import com.jsrdxzw.flashsale.lock.DistributedLock;
import com.jsrdxzw.flashsale.lock.DistributedLockFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.FIVE_MINUTES;
import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.ITEM_CACHE_KEY;
import static com.jsrdxzw.flashsale.util.StringHelper.link;

/**
 * @author xuzhiwei
 * @date 2021/12/3 5:29 PM
 */
@Slf4j
@Service
public class FlashItemCacheService {
    private static final Cache<Long, FlashItemCache> flashItemLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .concurrencyLevel(5)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();
    private static final String UPDATE_ITEM_CACHE_LOCK_KEY = "UPDATE_ITEM_CACHE_LOCK_KEY_";

    @Autowired
    private DistributedCacheService distributedCacheService;

    @Autowired
    private FlashItemDomainService flashItemDomainService;

    @Autowired
    private DistributedLockFactoryService distributedLockFactoryService;

    public FlashItemCache getCachedItem(Long itemId, Long version) {
        FlashItemCache flashItemCache = flashItemLocalCache.getIfPresent(itemId);
        if (flashItemCache != null) {
            if (version == null) {
                log.info("itemCache|命中本地缓存|{}", itemId);
                return flashItemCache;
            }
            if (version.equals(flashItemCache.getVersion()) || version < flashItemCache.getVersion()) {
                log.info("itemCache|命中本地缓存|{},{}", itemId, version);
                return flashItemCache;
            }
            return getLatestDistributedCache(itemId);
        }
        return getLatestDistributedCache(itemId);
    }

    private FlashItemCache getLatestDistributedCache(Long itemId) {
        log.info("itemCache|读取远程缓存|{}", itemId);
        FlashItemCache distributedCachedFlashItem = distributedCacheService.getObject(buildItemCacheKey(itemId), FlashItemCache.class);
        if (distributedCachedFlashItem == null) {
            return tryToUpdateItemCacheByLock(itemId);
        }
        return distributedCachedFlashItem;
    }

    public FlashItemCache tryToUpdateItemCacheByLock(Long itemId) {
        log.info("itemCache|更新远程缓存|{}", itemId);
        DistributedLock lock = distributedLockFactoryService.getDistributedLock(UPDATE_ITEM_CACHE_LOCK_KEY + itemId);
        try {
            boolean isLockSuccess = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLockSuccess) {
                return new FlashItemCache().tryLater();
            }
            FlashItem flashItem = flashItemDomainService.getFlashItem(itemId);
            if (flashItem == null) {
                return new FlashItemCache().notExist();
            }
            FlashItemCache flashItemCache = new FlashItemCache().with(flashItem).withVersion(System.currentTimeMillis());
            distributedCacheService.put(buildItemCacheKey(itemId), JSON.toJSONString(flashItemCache), FIVE_MINUTES);
            log.info("itemCache|远程缓存已更新|{}", itemId);

            flashItemLocalCache.put(itemId, flashItemCache);
            log.info("itemCache|本地缓存已更新|{}", itemId);
            return flashItemCache;
        } catch (InterruptedException e) {
            log.error("itemCache|远程缓存更新失败|{}", itemId);
            return new FlashItemCache().tryLater();
        } finally {
            lock.forceUnlock();
        }
    }

    private String buildItemCacheKey(Long itemId) {
        return link(ITEM_CACHE_KEY, itemId);
    }
}
