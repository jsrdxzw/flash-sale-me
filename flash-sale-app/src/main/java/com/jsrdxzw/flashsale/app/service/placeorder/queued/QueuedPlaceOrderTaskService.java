package com.jsrdxzw.flashsale.app.service.placeorder.queued;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jsrdxzw.flashsale.app.model.PlaceOrderTask;
import com.jsrdxzw.flashsale.app.model.enums.OrderTaskStatus;
import com.jsrdxzw.flashsale.app.model.result.OrderTaskSubmitResult;
import com.jsrdxzw.flashsale.app.mq.OrderTaskPostService;
import com.jsrdxzw.flashsale.app.service.stock.ItemStockCacheService;
import com.jsrdxzw.flashsale.app.service.stock.model.ItemStockCache;
import com.jsrdxzw.flashsale.cache.redis.RedisCacheService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import com.jsrdxzw.flashsale.lock.DistributedLock;
import com.jsrdxzw.flashsale.lock.DistributedLockFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jsrdxzw.flashsale.app.exception.AppErrorCode.*;
import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.HOURS_24;

/**
 * @author xuzhiwei
 * @date 2021/12/10 2:53 PM
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "queued")
public class QueuedPlaceOrderTaskService implements PlaceOrderTaskService {

    private static final String PLACE_ORDER_TASK_ID_KEY = "PLACE_ORDER_TASK_ID_KEY_";
    private static final String PLACE_ORDER_TASK_AVAILABLE_TOKENS_KEY = "PLACE_ORDER_TASK_AVAILABLE_TOKENS_KEY_";
    private static final String LOCK_REFRESH_LATEST_AVAILABLE_TOKENS_KEY = "LOCK_REFRESH_LATEST_AVAILABLE_TOKENS_KEY_";

    private static final String TAKE_ORDER_TOKEN_LUA;
    private static final String RECOVER_ORDER_TOKEN_LUA;

    private final static Cache<Long, Integer> availableOrderTokensLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(20)
            .concurrencyLevel(5)
            .expireAfterWrite(20, TimeUnit.MILLISECONDS)
            .build();

    static {
        TAKE_ORDER_TOKEN_LUA = "if (redis.call('exists', KEYS[1]) == 1) then" +
                "    local availableTokensCount = tonumber(redis.call('get', KEYS[1]));" +
                "    if (availableTokensCount == 0) then" +
                "        return -1;" +
                "    end;" +
                "    if (availableTokensCount > 0) then" +
                "        redis.call('incrby', KEYS[1], -1);" +
                "        return 1;" +
                "    end;" +
                "end;" +
                "return -100;";
        RECOVER_ORDER_TOKEN_LUA = "if (redis.call('exists', KEYS[1]) == 1) then" +
                "   redis.call('incrby', KEYS[1], 1);" +
                "   return 1;" +
                "end;" +
                "return -100;";
    }

    @Autowired
    private RedisCacheService redisCacheService;
    @Autowired
    private DistributedLockFactoryService lockFactoryService;
    @Autowired
    private ItemStockCacheService itemStockCacheService;
    @Autowired
    private OrderTaskPostService orderTaskPostService;

    @Override
    public OrderTaskSubmitResult submit(PlaceOrderTask placeOrderTask) {
        log.info("submitOrderTask|??????????????????|{}", JSONUtil.toJSONString(placeOrderTask));
        if (placeOrderTask == null) {
            return OrderTaskSubmitResult.failed(INVALID_PARAMS);
        }
        String taskKey = getOrderTaskKey(placeOrderTask.getPlaceOrderTaskId());

        //???user - item???????????????????????????
        Integer taskIdSubmittedResult = redisCacheService.getObject(taskKey, Integer.class);
        if (taskIdSubmittedResult != null) {
            return OrderTaskSubmitResult.failed(REDUNDANT_SUBMIT);
        }
        // ??????????????????????????????1.5 * stock
        Integer availableOrderTokens = getAvailableOrderTokens(placeOrderTask.getItemId());
        // ??????????????????????????????????????????MQ???
        if (availableOrderTokens == null || availableOrderTokens == 0) {
            return OrderTaskSubmitResult.failed(ORDER_TOKENS_NOT_AVAILABLE);
        }

        // ??????lua????????????????????????
        if (!takeOrRecoverToken(placeOrderTask, TAKE_ORDER_TOKEN_LUA)) {
            log.info("submitOrderTask|????????????????????????|{},{}", placeOrderTask.getUserId(), placeOrderTask.getPlaceOrderTaskId());
            return OrderTaskSubmitResult.failed(ORDER_TOKENS_NOT_AVAILABLE);
        }
        boolean postSuccess = orderTaskPostService.post(placeOrderTask);
        if (!postSuccess) {
            // ??????????????????????????????????????????????????????
            takeOrRecoverToken(placeOrderTask, RECOVER_ORDER_TOKEN_LUA);
            log.info("submitOrderTask|????????????????????????|{},{}", placeOrderTask.getUserId(), placeOrderTask.getPlaceOrderTaskId());
            return OrderTaskSubmitResult.failed(ORDER_TASK_SUBMIT_FAILED);
        }
        redisCacheService.put(taskKey, 0, HOURS_24);
        log.info("submitOrderTask|????????????????????????|{},{}", placeOrderTask.getUserId(), placeOrderTask.getPlaceOrderTaskId());
        return OrderTaskSubmitResult.ok();
    }

    @Override
    public void updateTaskHandleResult(String placeOrderTaskId, boolean result) {
        if (StringUtils.isEmpty(placeOrderTaskId)) {
            return;
        }
        String taskKey = getOrderTaskKey(placeOrderTaskId);
        Integer taskStatus = redisCacheService.getObject(taskKey, Integer.class);
        if (taskStatus == null || taskStatus != 0) {
            return;
        }
        redisCacheService.put(taskKey, result ? 1 : -1);
    }

    @Override
    public OrderTaskStatus getTaskStatus(String placeOrderTaskId) {
        String taskKey = getOrderTaskKey(placeOrderTaskId);
        Integer taskStatus = redisCacheService.getObject(taskKey, Integer.class);
        return OrderTaskStatus.findBy(taskStatus);
    }

    private boolean takeOrRecoverToken(PlaceOrderTask placeOrderTask, String script) {
        List<String> keys = new ArrayList<>();
        keys.add(getItemAvailableTokensKey(placeOrderTask.getItemId()));
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);

        for (int i = 0; i < 3; i++) {
            Long result = redisCacheService.getRedisTemplate().execute(redisScript, keys);
            if (result == null) {
                return false;
            }
            // ??????redis?????????????????????????????????????????????????????????
            if (result == -100) {
                refreshLatestAvailableTokens(placeOrderTask.getItemId());
                continue;
            }
            return result == 1L;
        }
        return false;
    }

    private Integer getAvailableOrderTokens(Long itemId) {
        Integer availableOrderTokens = availableOrderTokensLocalCache.getIfPresent(itemId);
        if (availableOrderTokens != null) {
            return availableOrderTokens;
        }
        return refreshLocalAvailableTokens(itemId);
    }

    /**
     * double check
     *
     * @param itemId
     * @return
     */
    private synchronized Integer refreshLocalAvailableTokens(Long itemId) {
        Integer availableOrderTokens = availableOrderTokensLocalCache.getIfPresent(itemId);
        if (availableOrderTokens != null) {
            return availableOrderTokens;
        }
        String availableTokensKey = getItemAvailableTokensKey(itemId);
        Integer latestAvailableOrderTokens = redisCacheService.getObject(availableTokensKey, Integer.class);
        if (latestAvailableOrderTokens != null) {
            availableOrderTokensLocalCache.put(itemId, latestAvailableOrderTokens);
            return latestAvailableOrderTokens;
        }
        return refreshLatestAvailableTokens(itemId);
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param itemId
     * @return
     */
    private Integer refreshLatestAvailableTokens(Long itemId) {
        DistributedLock refreshTokenLock = lockFactoryService.getDistributedLock(getRefreshTokensLockKey(itemId));
        try {
            boolean isLockSuccess = refreshTokenLock.tryLock(500, 1000, TimeUnit.MILLISECONDS);
            if (!isLockSuccess) {
                return null;
            }
            ItemStockCache itemStockCache = itemStockCacheService.getAvailableItemStock(null, itemId);
            // ????????????????????????????????????redis
            if (itemStockCache != null && itemStockCache.isSuccess() && itemStockCache.getAvailableStock() != null) {
                Integer latestAvailableOrderTokens = (int) Math.ceil(itemStockCache.getAvailableStock() * 1.5);
                redisCacheService.put(getItemAvailableTokensKey(itemId), latestAvailableOrderTokens, HOURS_24);
                availableOrderTokensLocalCache.put(itemId, latestAvailableOrderTokens);
                return latestAvailableOrderTokens;
            }
        } catch (Exception e) {
            log.error("refreshAvailableTokens|??????tokens??????|{}", itemId, e);
        } finally {
            refreshTokenLock.forceUnlock();
        }
        return null;
    }

    private String getRefreshTokensLockKey(Long itemId) {
        return LOCK_REFRESH_LATEST_AVAILABLE_TOKENS_KEY + itemId;
    }

    private String getItemAvailableTokensKey(Long itemId) {
        return PLACE_ORDER_TASK_AVAILABLE_TOKENS_KEY + itemId;
    }

    private String getOrderTaskKey(String placeOrderTaskId) {
        return PLACE_ORDER_TASK_ID_KEY + placeOrderTaskId;
    }
}
