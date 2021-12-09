package com.jsrdxzw.flashsale.app.service.item.cache;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jsrdxzw.flashsale.app.service.item.cache.model.FlashItemsCache;
import com.jsrdxzw.flashsale.cache.DistributedCacheService;
import com.jsrdxzw.flashsale.domain.model.PageResult;
import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import com.jsrdxzw.flashsale.domain.model.enums.FlashItemStatus;
import com.jsrdxzw.flashsale.domain.service.FlashItemDomainService;
import com.jsrdxzw.flashsale.lock.DistributedLock;
import com.jsrdxzw.flashsale.lock.DistributedLockFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.FIVE_MINUTES;
import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.ITEMS_CACHE_KEY;

/**
 * @author xuzhiwei
 * @date 2021/12/3 5:29 PM
 */
@Slf4j
@Service
public class FlashItemsCacheService {
    private static final Cache<Long, FlashItemsCache> flashItemsLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .concurrencyLevel(5)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();
    private static final String UPDATE_ITEMS_CACHE_LOCK_KEY = "UPDATE_ITEMS_CACHE_LOCK_KEY_";

    @Autowired
    private DistributedCacheService distributedCacheService;

    @Autowired
    private FlashItemDomainService flashItemDomainService;

    @Autowired
    private DistributedLockFactoryService distributedLockFactoryService;

    public FlashItemsCache getCachedItems(Long activityId, Long version) {
        FlashItemsCache flashItemCache = flashItemsLocalCache.getIfPresent(activityId);
        if (flashItemCache != null) {
            if (version == null) {
                log.info("itemsCache|命中本地缓存|{}", activityId);
                return flashItemCache;
            }
            if (version.equals(flashItemCache.getVersion()) || version < flashItemCache.getVersion()) {
                log.info("itemsCache|命中本地缓存|{},{}", activityId, version);
                return flashItemCache;
            }
            return getLatestDistributedCache(activityId);
        }
        return getLatestDistributedCache(activityId);
    }

    private FlashItemsCache getLatestDistributedCache(Long activityId) {
        log.info("itemsCache|读取远程缓存|{}", activityId);
        FlashItemsCache distributedCachedFlashItem = distributedCacheService.getObject(buildItemCacheKey(activityId), FlashItemsCache.class);
        if (distributedCachedFlashItem == null) {
            return tryToUpdateItemsCacheByLock(activityId);
        }
        return distributedCachedFlashItem;
    }

    public FlashItemsCache tryToUpdateItemsCacheByLock(Long activityId) {
        log.info("itemsCache|更新远程缓存|{}", activityId);
        DistributedLock lock = distributedLockFactoryService.getDistributedLock(UPDATE_ITEMS_CACHE_LOCK_KEY + activityId);
        try {
            boolean isLockSuccess = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLockSuccess) {
                return new FlashItemsCache().tryLater();
            }
            PagesQueryCondition pagesQueryCondition = new PagesQueryCondition();
            pagesQueryCondition.setActivityId(activityId);
            pagesQueryCondition.setStatus(FlashItemStatus.ONLINE.getCode());
            PageResult<FlashItem> flashItemPageResult = flashItemDomainService.getFlashItems(pagesQueryCondition);
            if (flashItemPageResult == null) {
                return new FlashItemsCache().notExist();
            }
            FlashItemsCache flashItemCache = new FlashItemsCache()
                    .setTotal(flashItemPageResult.getTotal())
                    .setFlashItems(flashItemPageResult.getData())
                    .setVersion(System.currentTimeMillis());
            distributedCacheService.put(buildItemCacheKey(activityId), JSON.toJSONString(flashItemCache), FIVE_MINUTES);
            log.info("itemsCache|远程缓存已更新|{}", activityId);

            flashItemsLocalCache.put(activityId, flashItemCache);
            log.info("itemsCache|本地缓存已更新|{}", activityId);
            return flashItemCache;
        } catch (Exception e) {
            log.error("itemsCache|远程缓存更新失败|{}", activityId);
            return new FlashItemsCache().tryLater();
        } finally {
            lock.forceUnlock();
        }
    }

    private String buildItemCacheKey(Long activityId) {
        return ITEMS_CACHE_KEY + activityId;
    }
}
