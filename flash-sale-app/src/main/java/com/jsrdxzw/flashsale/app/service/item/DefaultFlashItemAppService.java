package com.jsrdxzw.flashsale.app.service.item;

import com.jsrdxzw.flashsale.app.auth.AuthorizationService;
import com.jsrdxzw.flashsale.app.auth.model.AuthResult;
import com.jsrdxzw.flashsale.app.exception.BizException;
import com.jsrdxzw.flashsale.app.model.command.FlashItemPublishCommand;
import com.jsrdxzw.flashsale.app.model.converter.FlashItemAppMapping;
import com.jsrdxzw.flashsale.app.model.dto.FlashItemDTO;
import com.jsrdxzw.flashsale.app.model.query.FlashItemsQuery;
import com.jsrdxzw.flashsale.app.model.result.AppMultiResult;
import com.jsrdxzw.flashsale.app.model.result.AppResult;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;
import com.jsrdxzw.flashsale.app.service.item.cache.FlashItemCacheService;
import com.jsrdxzw.flashsale.app.service.item.cache.FlashItemsCacheService;
import com.jsrdxzw.flashsale.app.service.item.cache.model.FlashItemCache;
import com.jsrdxzw.flashsale.app.service.item.cache.model.FlashItemsCache;
import com.jsrdxzw.flashsale.app.service.stock.ItemStockCacheService;
import com.jsrdxzw.flashsale.app.service.stock.model.ItemStockCache;
import com.jsrdxzw.flashsale.controller.AuthException;
import com.jsrdxzw.flashsale.domain.model.PageResult;
import com.jsrdxzw.flashsale.domain.model.entity.FlashActivity;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import com.jsrdxzw.flashsale.domain.service.FlashActivityDomainService;
import com.jsrdxzw.flashsale.domain.service.FlashItemDomainService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.jsrdxzw.flashsale.app.auth.model.ResourceEnum.FLASH_ITEM_CREATE;
import static com.jsrdxzw.flashsale.app.auth.model.ResourceEnum.FLASH_ITEM_OFFLINE;
import static com.jsrdxzw.flashsale.app.exception.AppErrorCode.*;
import static com.jsrdxzw.flashsale.controller.ErrorCode.UNAUTHORIZED_ACCESS;

/**
 * @author xuzhiwei
 * @date 2021/12/4 11:20 AM
 */
@Slf4j
@Service
public class DefaultFlashItemAppService implements FlashItemAppService {

    @Autowired
    private FlashItemDomainService flashItemDomainService;

    @Autowired
    private FlashActivityDomainService flashActivityDomainService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private FlashItemCacheService flashItemCacheService;

    @Autowired
    private FlashItemsCacheService flashItemsCacheService;

    @Autowired
    private ItemStockCacheService itemStockCacheService;

    @Override
    public AppResult publishFlashItem(Long userId, Long activityId, FlashItemPublishCommand itemPublishCommand) {
        log.info("itemPublish|发布秒杀品|{},{},{}", userId, activityId, JSONUtil.toJSONString(itemPublishCommand));
        AuthResult authResult = authorizationService.auth(userId, FLASH_ITEM_CREATE);
        if (!authResult.isSuccess()) {
            throw new AuthException(UNAUTHORIZED_ACCESS);
        }
        if (userId == null || activityId == null || itemPublishCommand == null || !itemPublishCommand.validate()) {
            throw new BizException(INVALID_PARAMS);
        }
        FlashActivity flashActivity = flashActivityDomainService.getFlashActivity(activityId);
        if (flashActivity == null) {
            throw new BizException(ACTIVITY_NOT_FOUND);
        }
        FlashItem flashItem = FlashItemAppMapping.INSTANCE.toDomain(itemPublishCommand);
        flashItem.setActivityId(activityId);
        flashItem.setStockWarmUp(0);
        flashItemDomainService.publishFlashItem(flashItem);
        log.info("itemPublish|秒杀品已发布");
        return AppResult.buildSuccess();
    }

    @Override
    public AppResult onlineFlashItem(Long userId, Long activityId, Long itemId) {
        log.info("itemOnline|上线秒杀品|{},{},{}", userId, activityId, itemId);
        AuthResult authResult = authorizationService.auth(userId, FLASH_ITEM_OFFLINE);
        if (!authResult.isSuccess()) {
            throw new AuthException(UNAUTHORIZED_ACCESS);
        }
        if (userId == null || activityId == null || itemId == null) {
            throw new BizException(INVALID_PARAMS);
        }
        flashItemDomainService.onlineFlashItem(itemId);
        log.info("itemOnline|秒杀品已上线");
        return AppResult.buildSuccess();
    }

    @Override
    public AppResult offlineFlashItem(Long userId, Long activityId, Long itemId) {
        log.info("itemOnline|下线秒杀品|{},{},{}", userId, activityId, itemId);
        AuthResult authResult = authorizationService.auth(userId, FLASH_ITEM_OFFLINE);
        if (!authResult.isSuccess()) {
            throw new AuthException(UNAUTHORIZED_ACCESS);
        }
        if (userId == null || activityId == null || itemId == null) {
            throw new BizException(INVALID_PARAMS);
        }
        flashItemDomainService.offlineFlashItem(itemId);
        log.info("itemOnline|秒杀品已下线");
        return AppResult.buildSuccess();
    }

    @Override
    public AppMultiResult<FlashItemDTO> getFlashItems(Long userId, Long activityId, FlashItemsQuery flashItemsQuery) {
        if (flashItemsQuery == null) {
            return AppMultiResult.empty();
        }
        flashItemsQuery.setActivityId(activityId);
        List<FlashItem> flashItems;
        Integer total;
        // 只缓存第一页
        if (flashItemsQuery.isOnlineFirstPageQuery()) {
            FlashItemsCache flashItemsCache = flashItemsCacheService.getCachedItems(activityId, flashItemsQuery.getVersion());
            if (flashItemsCache.isLater()) {
                return AppMultiResult.tryLater();
            }
            flashItems = flashItemsCache.getFlashItems();
            total = flashItemsCache.getTotal();
        } else {
            PageResult<FlashItem> flashItemPageResult = flashItemDomainService.getFlashItems(
                    FlashItemAppMapping.INSTANCE.toFlashItemsQuery(flashItemsQuery));
            flashItems = flashItemPageResult.getData();
            total = flashItemPageResult.getTotal();
        }
        List<FlashItemDTO> flashItemDTOList = flashItems.stream()
                .map(FlashItemAppMapping.INSTANCE::toFlashItemDTO)
                .collect(Collectors.toList());
        return AppMultiResult.of(flashItemDTOList, total);
    }

    /**
     * 秒杀和库存是分开存入缓存的
     */
    @Override
    public AppSimpleResult<FlashItemDTO> getFlashItem(Long userId, Long activityId, Long itemId, Long version) {
        log.info("itemGet|读取秒杀品|{},{},{},{}", userId, activityId, itemId, version);
        FlashItemCache flashItemCache = flashItemCacheService.getCachedItem(itemId, version);
        if (!flashItemCache.isExist()) {
            throw new BizException(ITEM_NOT_FOUND.getErrDesc());
        }
        if (flashItemCache.isLater()) {
            return AppSimpleResult.tryLater();
        }
        updateLatestItemStock(userId, flashItemCache.getFlashItem());
        FlashItemDTO flashItemDTO = FlashItemAppMapping.INSTANCE.toFlashItemDTO(flashItemCache.getFlashItem());
        flashItemDTO.setVersion(flashItemCache.getVersion());
        return AppSimpleResult.ok(flashItemDTO);
    }

    @Override
    public AppSimpleResult<FlashItemDTO> getFlashItem(Long itemId) {
        FlashItemCache flashItemCache = flashItemCacheService.getCachedItem(itemId, null);
        if (!flashItemCache.isExist()) {
            throw new BizException(ACTIVITY_NOT_FOUND.getErrDesc());
        }
        if (flashItemCache.isLater()) {
            return AppSimpleResult.tryLater();
        }
        updateLatestItemStock(null, flashItemCache.getFlashItem());
        FlashItemDTO flashItemDTO = FlashItemAppMapping.INSTANCE.toFlashItemDTO(flashItemCache.getFlashItem());
        flashItemDTO.setVersion(flashItemCache.getVersion());
        return AppSimpleResult.ok(flashItemDTO);
    }

    @Override
    public boolean isAllowPlaceOrderOrNot(Long itemId) {
        try {
            FlashItemCache flashItemCache = null;
            for (int i = 0; i < 3; i++) {
                flashItemCache = flashItemCacheService.getCachedItem(itemId, null);
                if (!flashItemCache.isExist()) {
                    return false;
                }
                if (flashItemCache.isLater()) {
                    Thread.sleep(20);
                }
                if (flashItemCache.getFlashItem() != null) {
                    break;
                }
            }
            if (flashItemCache.getFlashItem() != null) {
                if (!flashItemCache.getFlashItem().isOnline()) {
                    log.info("isAllowPlaceOrderOrNot|秒杀品尚未上线|{}", itemId);
                    return false;
                }
                if (!flashItemCache.getFlashItem().isInProgress()) {
                    log.info("isAllowPlaceOrderOrNot|当前非秒杀时段|{}", itemId);
                    return false;
                }
                return true;
            }
        } catch (InterruptedException e) {
            return false;
        }
        return false;
    }

    private void updateLatestItemStock(Long userId, FlashItem flashItem) {
        if (flashItem == null) {
            return;
        }
        ItemStockCache itemStockCache = itemStockCacheService.getAvailableItemStock(userId, flashItem.getId());
        if (itemStockCache != null && itemStockCache.isSuccess() && itemStockCache.getAvailableStock() != null) {
            flashItem.setAvailableStock(itemStockCache.getAvailableStock());
        }
    }
}
