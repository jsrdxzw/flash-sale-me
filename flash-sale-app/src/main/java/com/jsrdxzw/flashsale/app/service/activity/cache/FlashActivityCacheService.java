package com.jsrdxzw.flashsale.app.service.activity.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jsrdxzw.flashsale.app.service.activity.cache.model.FlashActivityCache;
import com.jsrdxzw.flashsale.cache.DistributedCacheService;
import com.jsrdxzw.flashsale.domain.model.entity.FlashActivity;
import com.jsrdxzw.flashsale.domain.service.FlashActivityDomainService;
import com.jsrdxzw.flashsale.lock.DistributedLock;
import com.jsrdxzw.flashsale.lock.DistributedLockFactoryService;
import com.jsrdxzw.flashsale.util.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.ACTIVITY_CACHE_KEY;
import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.FIVE_MINUTES;

/**
 * @author xuzhiwei
 * @date 2021/12/3 11:15 AM
 */
@Service
@Slf4j
public class FlashActivityCacheService {
    private static final Cache<Long, FlashActivityCache> flashActivityLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .concurrencyLevel(5)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();
    private static final String UPDATE_ACTIVITY_CACHE_LOCK_KEY = "UPDATE_ACTIVITY_CACHE_LOCK_KEY_";

    @Autowired
    private DistributedCacheService distributedCacheService;

    @Autowired
    private FlashActivityDomainService flashActivityDomainService;

    @Autowired
    private DistributedLockFactoryService distributedLockFactoryService;

    public FlashActivityCache getCachedActivity(Long activityId, Long version) {
        FlashActivityCache flashActivityCache = flashActivityLocalCache.getIfPresent(activityId);
        if (flashActivityCache != null) {
            if (version == null) {
                log.info("activityCache|命中本地缓存|{}", activityId);
                return flashActivityCache;
            }
            if (version.equals(flashActivityCache.getVersion()) || version < flashActivityCache.getVersion()) {
                log.info("activityCache|命中本地缓存|{},{}", activityId, version);
                return flashActivityCache;
            }
            if (version > flashActivityCache.getVersion()) {
                return getLatestDistributedCache(activityId);
            }
        }
        return getLatestDistributedCache(activityId);
    }

    /**
     * 从分布式缓存中获取, 然后存入本地缓存
     *
     * @param activityId
     * @return
     */
    private FlashActivityCache getLatestDistributedCache(Long activityId) {
        log.info("activityCache|读取远程缓存|{}", activityId);
        FlashActivityCache distributedCachedFlashActivity = distributedCacheService.getObject(buildActivityCacheKey(activityId), FlashActivityCache.class);
        // 分布式缓存中获取不到，需要在数据库中读取并放入缓存, 必须保证原子性操作
        if (distributedCachedFlashActivity == null) {
            return tryToUpdateActivityCacheByLock(activityId);
        }
        return distributedCachedFlashActivity;
    }

    public FlashActivityCache tryToUpdateActivityCacheByLock(Long activityId) {
        log.info("activityCache|更新远程缓存|{}", activityId);
        DistributedLock lock = distributedLockFactoryService.getDistributedLock(UPDATE_ACTIVITY_CACHE_LOCK_KEY + activityId);
        try {
            boolean isLockSuccess = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLockSuccess) {
                return new FlashActivityCache().tryLater();
            }
            FlashActivity flashActivity = flashActivityDomainService.getFlashActivity(activityId);
            if (flashActivity == null) {
                // 防止缓存击穿
                return new FlashActivityCache().notExist();
            }
            // 版本号使用了本地的时间
            FlashActivityCache flashActivityCache = new FlashActivityCache().with(flashActivity).withVersion(System.currentTimeMillis());
            // 分布式缓存过期5分钟
            distributedCacheService.put(buildActivityCacheKey(activityId), flashActivityCache, FIVE_MINUTES);
            log.info("activityCache|远程缓存已更新|{}", activityId);
            // 放入本地缓存
            flashActivityLocalCache.put(activityId, flashActivityCache);
            log.info("activityCache|本地缓存已更新|{}", activityId);
            return flashActivityCache;
        } catch (InterruptedException e) {
            log.error("activityCache|远程缓存更新失败|{},{}", activityId, e);
            return new FlashActivityCache().tryLater();
        } finally {
            lock.forceUnlock();
        }
    }

    private String buildActivityCacheKey(Long activityId) {
        return StringHelper.link(ACTIVITY_CACHE_KEY, activityId);
    }
}
