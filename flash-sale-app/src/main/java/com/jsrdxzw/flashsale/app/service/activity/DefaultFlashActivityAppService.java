package com.jsrdxzw.flashsale.app.service.activity;

import com.jsrdxzw.flashsale.app.auth.AuthorizationService;
import com.jsrdxzw.flashsale.app.auth.model.AuthResult;
import com.jsrdxzw.flashsale.app.exception.BizException;
import com.jsrdxzw.flashsale.app.model.command.FlashActivityPublishCommand;
import com.jsrdxzw.flashsale.app.model.converter.FlashActivityAppMapping;
import com.jsrdxzw.flashsale.app.model.dto.FlashActivityDTO;
import com.jsrdxzw.flashsale.app.model.query.FlashActivitiesQuery;
import com.jsrdxzw.flashsale.app.model.result.AppMultiResult;
import com.jsrdxzw.flashsale.app.model.result.AppResult;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;
import com.jsrdxzw.flashsale.app.service.activity.cache.FlashActivitiesCacheService;
import com.jsrdxzw.flashsale.app.service.activity.cache.FlashActivityCacheService;
import com.jsrdxzw.flashsale.app.service.activity.cache.model.FlashActivitiesCache;
import com.jsrdxzw.flashsale.app.service.activity.cache.model.FlashActivityCache;
import com.jsrdxzw.flashsale.controller.AuthException;
import com.jsrdxzw.flashsale.domain.model.PageResult;
import com.jsrdxzw.flashsale.domain.model.entity.FlashActivity;
import com.jsrdxzw.flashsale.domain.service.FlashActivityDomainService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.jsrdxzw.flashsale.app.auth.model.ResourceEnum.FLASH_ITEM_CREATE;
import static com.jsrdxzw.flashsale.app.exception.AppErrorCode.ACTIVITY_NOT_FOUND;
import static com.jsrdxzw.flashsale.app.exception.AppErrorCode.INVALID_PARAMS;
import static com.jsrdxzw.flashsale.controller.ErrorCode.UNAUTHORIZED_ACCESS;

/**
 * @author xuzhiwei
 * @date 2021/12/2 10:22 AM
 */
@Slf4j
@Service
public class DefaultFlashActivityAppService implements FlashActivityAppService {

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private FlashActivityDomainService flashActivityDomainService;

    @Autowired
    private FlashActivitiesCacheService flashActivitiesCacheService;

    @Autowired
    private FlashActivityCacheService flashActivityCacheService;

    @Override
    public AppMultiResult<FlashActivityDTO> getFlashActivities(Long userId, FlashActivitiesQuery flashActivitiesQuery) {
        List<FlashActivity> activities;
        Integer total;
        if (flashActivitiesQuery.isFirstPureQuery()) {
            FlashActivitiesCache flashActivitiesCache = flashActivitiesCacheService.getCachedActivities(flashActivitiesQuery.getPageNumber(), flashActivitiesQuery.getVersion());
            if (flashActivitiesCache.isLater()) {
                return AppMultiResult.tryLater();
            }
            activities = flashActivitiesCache.getFlashActivities();
            total = flashActivitiesCache.getTotal();
        } else {
            PageResult<FlashActivity> flashActivityPageResult = flashActivityDomainService.getFlashActivities(
                    FlashActivityAppMapping.INSTANCE.toFlashActivitiesQuery(flashActivitiesQuery));
            activities = flashActivityPageResult.getData();
            total = flashActivityPageResult.getTotal();
        }

        List<FlashActivityDTO> flashActivityDTOList = activities.stream()
                .map(FlashActivityAppMapping.INSTANCE::toFlashActivityDTO)
                .collect(Collectors.toList());
        return AppMultiResult.of(flashActivityDTOList, total);
    }

    @Override
    public AppSimpleResult<FlashActivityDTO> getFlashActivity(Long userId, Long activityId, Long version) {
        if (userId == null || activityId == null) {
            throw new BizException(INVALID_PARAMS);
        }
        FlashActivityCache flashActivityCache = flashActivityCacheService.getCachedActivity(activityId, version);
        if (!flashActivityCache.isExist()) {
            throw new BizException(ACTIVITY_NOT_FOUND.getErrDesc());
        }
        if (flashActivityCache.isLater()) {
            return AppSimpleResult.tryLater();
        }
        FlashActivityDTO flashActivityDTO = FlashActivityAppMapping.INSTANCE.toFlashActivityDTO(flashActivityCache.getFlashActivity());
        flashActivityDTO.setVersion(flashActivityCache.getVersion());
        return AppSimpleResult.ok(flashActivityDTO);
    }

    @Override
    public AppResult publishFlashActivity(Long userId, FlashActivityPublishCommand flashActivityPublishCommand) {
        log.info("activityPublish|发布秒杀活动|{},{}", userId, JSONUtil.toJSONString(flashActivityPublishCommand));
        preCheckParams(userId, flashActivityPublishCommand);
        flashActivityDomainService.publishActivity(userId, FlashActivityAppMapping.INSTANCE.toDomain(flashActivityPublishCommand));
        log.info("activityPublish|活动已发布");
        return AppResult.buildSuccess();
    }

    @Override
    public AppResult modifyFlashActivity(Long userId, Long activityId, FlashActivityPublishCommand flashActivityPublishCommand) {
        log.info("activityModification|秒杀活动修改|{},{},{}", userId, activityId, JSONUtil.toJSONString(flashActivityPublishCommand));
        preCheckParams(userId, flashActivityPublishCommand);
        FlashActivity flashActivity = FlashActivityAppMapping.INSTANCE.toDomain(flashActivityPublishCommand);
        flashActivity.setId(activityId);
        flashActivityDomainService.modifyActivity(userId, flashActivity);
        log.info("activityModification|活动已修改");
        return AppResult.buildSuccess();
    }

    @Override
    public AppResult onlineFlashActivity(Long userId, Long activityId) {
        log.info("activityOnline|上线活动|{},{}", userId, activityId);
        if (userId == null || activityId == null) {
            throw new BizException(INVALID_PARAMS);
        }
        AuthResult authResult = authorizationService.auth(userId, FLASH_ITEM_CREATE);
        if (!authResult.isSuccess()) {
            throw new AuthException(UNAUTHORIZED_ACCESS);
        }
        flashActivityDomainService.onlineActivity(userId, activityId);
        log.info("activityOnline|活动已上线");
        return AppResult.buildSuccess();
    }

    @Override
    public AppResult offlineFlashActivity(Long userId, Long activityId) {
        log.info("activityOnline|下线活动|{},{}", userId, activityId);
        if (userId == null || activityId == null) {
            throw new BizException(INVALID_PARAMS);
        }
        AuthResult authResult = authorizationService.auth(userId, FLASH_ITEM_CREATE);
        if (!authResult.isSuccess()) {
            throw new AuthException(UNAUTHORIZED_ACCESS);
        }
        flashActivityDomainService.offlineActivity(userId, activityId);
        log.info("activityOnline|活动已下线");
        return AppResult.buildSuccess();
    }

    private void preCheckParams(Long userId, FlashActivityPublishCommand flashActivityPublishCommand) {
        if (userId == null || flashActivityPublishCommand == null || !flashActivityPublishCommand.validate()) {
            throw new BizException(INVALID_PARAMS);
        }
        AuthResult authResult = authorizationService.auth(userId, FLASH_ITEM_CREATE);
        if (!authResult.isSuccess()) {
            throw new AuthException(UNAUTHORIZED_ACCESS);
        }
    }
}
