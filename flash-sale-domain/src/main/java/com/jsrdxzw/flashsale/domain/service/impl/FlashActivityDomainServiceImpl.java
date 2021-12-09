package com.jsrdxzw.flashsale.domain.service.impl;

import com.jsrdxzw.flashsale.domain.model.PageResult;
import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.enums.FlashActivityStatus;
import com.jsrdxzw.flashsale.domain.event.DomainEventPublisher;
import com.jsrdxzw.flashsale.domain.event.FlashActivityEvent;
import com.jsrdxzw.flashsale.domain.event.FlashActivityEventType;
import com.jsrdxzw.flashsale.domain.exception.DomainException;
import com.jsrdxzw.flashsale.domain.model.entity.FlashActivity;
import com.jsrdxzw.flashsale.domain.repository.FlashActivityRepository;
import com.jsrdxzw.flashsale.domain.service.FlashActivityDomainService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.jsrdxzw.flashsale.domain.exception.DomainErrorCode.*;

/**
 * @author xuzhiwei
 * @date 2021/12/2 2:03 PM
 */
@Slf4j
@Service
public class FlashActivityDomainServiceImpl implements FlashActivityDomainService {

    @Autowired
    private FlashActivityRepository flashActivityRepository;

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Override
    public void publishActivity(Long userId, FlashActivity flashActivity) {
        log.info("activityPublish|发布秒杀活动|{},{}", userId, JSONUtil.toJSONString(flashActivity));
        if (flashActivity == null || !flashActivity.validateParamsForCreateOrUpdate()) {
            throw new DomainException(ONLINE_FLASH_ACTIVITY_PARAMS_INVALID);
        }
        flashActivity.setStatus(FlashActivityStatus.PUBLISHED.getCode());
        flashActivityRepository.save(flashActivity);
        log.info("activityPublish|活动已发布|{},{}", userId, flashActivity.getId());
        FlashActivityEvent flashActivityEvent = new FlashActivityEvent();
        flashActivityEvent.setEventType(FlashActivityEventType.PUBLISHED);
        flashActivityEvent.setFlashActivity(flashActivity);

        domainEventPublisher.publish(flashActivityEvent);
        log.info("activityPublish|活动发布事件已发布|{}", JSONUtil.toJSONString(flashActivityEvent));
    }

    @Override
    public void modifyActivity(Long userId, FlashActivity flashActivity) {
        log.info("activityModification|秒杀活动修改|{},{}", userId, JSONUtil.toJSONString(flashActivity));
        if (flashActivity == null || !flashActivity.validateParamsForCreateOrUpdate()) {
            throw new DomainException(ONLINE_FLASH_ACTIVITY_PARAMS_INVALID);
        }
        flashActivityRepository.save(flashActivity);
        log.info("activityModification|活动已修改|{},{}", userId, flashActivity.getId());

        FlashActivityEvent flashActivityEvent = new FlashActivityEvent();
        flashActivityEvent.setEventType(FlashActivityEventType.MODIFIED);
        flashActivityEvent.setFlashActivity(flashActivity);
        domainEventPublisher.publish(flashActivityEvent);
        log.info("activityModification|活动修改事件已发布|{}", JSONUtil.toJSONString(flashActivityEvent));
    }

    @Override
    public void onlineActivity(Long userId, Long activityId) {
        log.info("activityOnline|上线秒杀活动|{},{}", userId, activityId);
        FlashActivity flashActivity = preCheckActivity(userId, activityId);
        if (FlashActivityStatus.isOnline(flashActivity.getStatus())) {
            return;
        }
        flashActivity.setStatus(FlashActivityStatus.ONLINE.getCode());
        flashActivityRepository.save(flashActivity);
        log.info("activityOnline|活动已上线|{},{}", userId, flashActivity.getId());

        FlashActivityEvent flashActivityEvent = new FlashActivityEvent();
        flashActivityEvent.setEventType(FlashActivityEventType.ONLINE);
        flashActivityEvent.setFlashActivity(flashActivity);
        domainEventPublisher.publish(flashActivityEvent);
        log.info("activityOnline|活动上线事件已发布|{}", JSONUtil.toJSONString(flashActivityEvent));
    }

    @Override
    public void offlineActivity(Long userId, Long activityId) {
        log.info("activityOffline|下线秒杀活动|{},{}", userId, activityId);
        FlashActivity flashActivity = preCheckActivity(userId, activityId);
        if (FlashActivityStatus.isOffline(flashActivity.getStatus())) {
            return;
        }
        if (!FlashActivityStatus.isOnline(flashActivity.getStatus())) {
            throw new DomainException(FLASH_ACTIVITY_NOT_ONLINE);
        }
        flashActivity.setStatus(FlashActivityStatus.OFFLINE.getCode());
        flashActivityRepository.save(flashActivity);
        log.info("activityOffline|活动已下线|{},{}", userId, flashActivity.getId());

        FlashActivityEvent flashActivityEvent = new FlashActivityEvent();
        flashActivityEvent.setEventType(FlashActivityEventType.OFFLINE);
        flashActivityEvent.setFlashActivity(flashActivity);
        domainEventPublisher.publish(flashActivityEvent);
        log.info("activityOffline|活动下线事件已发布|{}", JSONUtil.toJSONString(flashActivityEvent));
    }

    @Override
    public PageResult<FlashActivity> getFlashActivities(PagesQueryCondition pagesQueryCondition) {
        if (pagesQueryCondition == null) {
            pagesQueryCondition = new PagesQueryCondition();
        }
        List<FlashActivity> flashActivities = flashActivityRepository.findFlashActivitiesByCondition(pagesQueryCondition.buildParams());
        Integer total = flashActivityRepository.countFlashActivitiesByCondition(pagesQueryCondition);
        return PageResult.with(flashActivities, total);
    }

    @Override
    public FlashActivity getFlashActivity(Long activityId) {
        if (activityId == null) {
            throw new DomainException(PARAMS_INVALID);
        }
        Optional<FlashActivity> flashActivityOptional = flashActivityRepository.findById(activityId);
        return flashActivityOptional.orElse(null);
    }

    @Override
    public boolean isAllowPlaceOrderOrNot(Long activityId) {
        Optional<FlashActivity> flashActivityOptional = flashActivityRepository.findById(activityId);
        if (flashActivityOptional.isEmpty()) {
            log.info("isAllowPlaceOrderOrNot|活动不存在|{}", activityId);
            return false;
        }
        FlashActivity flashActivity = flashActivityOptional.get();
        if (!flashActivity.isOnline()) {
            log.info("isAllowPlaceOrderOrNot|活动尚未上线|{}", activityId);
            return false;
        }
        if (!flashActivity.isInProgress()) {
            log.info("isAllowPlaceOrderOrNot|活动非秒杀时段|{}", activityId);
            return false;
        }
        return true;
    }

    private FlashActivity preCheckActivity(Long userId, Long activityId) {
        if (userId == null || activityId == null) {
            throw new DomainException(PARAMS_INVALID);
        }
        Optional<FlashActivity> flashActivityOptional = flashActivityRepository.findById(activityId);
        if (flashActivityOptional.isEmpty()) {
            throw new DomainException(FLASH_ACTIVITY_DOES_NOT_EXIST);
        }
        return flashActivityOptional.get();
    }
}
