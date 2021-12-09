package com.jsrdxzw.flashsale.app.service.activity.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jsrdxzw.flashsale.app.service.activity.cache.model.FlashActivitiesCache;
import com.jsrdxzw.flashsale.cache.DistributedCacheService;
import com.jsrdxzw.flashsale.domain.model.PageResult;
import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashActivity;
import com.jsrdxzw.flashsale.domain.service.FlashActivityDomainService;
import com.jsrdxzw.flashsale.lock.DistributedLock;
import com.jsrdxzw.flashsale.lock.DistributedLockFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.ACTIVITIES_CACHE_KEY;
import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.FIVE_MINUTES;
import static com.jsrdxzw.flashsale.util.StringHelper.link;

/**
 * @author xuzhiwei
 * @date 2021/12/3 5:59 PM
 */
@Slf4j
@Service
public class FlashActivitiesCacheService {
    private static final Cache<Integer, FlashActivitiesCache> flashActivitiesLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .concurrencyLevel(5)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();
    private static final String UPDATE_ACTIVITIES_CACHE_LOCK_KEY = "UPDATE_ACTIVITIES_CACHE_LOCK_KEY";

    @Autowired
    private DistributedCacheService distributedCacheService;

    @Autowired
    private FlashActivityDomainService flashActivityDomainService;

    @Autowired
    private DistributedLockFactoryService distributedLockFactoryService;

    public FlashActivitiesCache getCachedActivities(Integer pageNumber, Long version) {
        if (pageNumber == null) {
            pageNumber = 1;
        }
        FlashActivitiesCache flashActivityCache = flashActivitiesLocalCache.getIfPresent(pageNumber);
        if (flashActivityCache != null) {
            if (version == null) {
                log.info("activitiesCache|命中本地缓存|{}", pageNumber);
                return flashActivityCache;
            }
            if (version.equals(flashActivityCache.getVersion()) || version < flashActivityCache.getVersion()) {
                log.info("activitiesCache|命中本地缓存|{},{}", pageNumber, version);
                return flashActivityCache;
            }
            return getLatestDistributedCache(pageNumber);
        }
        return getLatestDistributedCache(pageNumber);
    }

    private FlashActivitiesCache getLatestDistributedCache(Integer pageNumber) {
        log.info("activitiesCache|读取远程缓存|{}", pageNumber);
        FlashActivitiesCache distributedCachedFlashActivity = distributedCacheService.getObject(buildActivityCacheKey(pageNumber), FlashActivitiesCache.class);
        if (distributedCachedFlashActivity == null) {
            return tryToUpdateActivitiesCacheByLock(pageNumber);
        }
        return distributedCachedFlashActivity;
    }

    public FlashActivitiesCache tryToUpdateActivitiesCacheByLock(Integer pageNumber) {
        log.info("activitiesCache|更新远程缓存|{}", pageNumber);
        DistributedLock lock = distributedLockFactoryService.getDistributedLock(UPDATE_ACTIVITIES_CACHE_LOCK_KEY);
        try {
            boolean isLockSuccess = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLockSuccess) {
                return new FlashActivitiesCache().tryLater();
            }
            PagesQueryCondition pagesQueryCondition = new PagesQueryCondition();
            PageResult<FlashActivity> flashActivityPageResult = flashActivityDomainService.getFlashActivities(pagesQueryCondition);
            if (flashActivityPageResult == null) {
                return new FlashActivitiesCache().notExist();
            }
            FlashActivitiesCache flashActivityCache = new FlashActivitiesCache()
                    .setTotal(flashActivityPageResult.getTotal())
                    .setFlashActivities(flashActivityPageResult.getData())
                    .setVersion(System.currentTimeMillis());
            distributedCacheService.put(buildActivityCacheKey(pageNumber), flashActivityCache, FIVE_MINUTES);
            log.info("activitiesCache|远程缓存已更新|{}", pageNumber);

            flashActivitiesLocalCache.put(pageNumber, flashActivityCache);
            log.info("activitiesCache|本地缓存已更新|{}", pageNumber);
            return flashActivityCache;
        } catch (InterruptedException e) {
            log.error("activitiesCache|远程缓存更新失败", e);
            return new FlashActivitiesCache().tryLater();
        } finally {
            lock.forceUnlock();
        }
    }

    private String buildActivityCacheKey(Integer pageNumber) {
        return link(ACTIVITIES_CACHE_KEY, pageNumber);
    }
}
