package com.jsrdxzw.flashsale.domain.service.impl;

import com.jsrdxzw.flashsale.domain.event.DomainEventPublisher;
import com.jsrdxzw.flashsale.domain.event.FlashItemEvent;
import com.jsrdxzw.flashsale.domain.event.FlashItemEventType;
import com.jsrdxzw.flashsale.domain.exception.DomainException;
import com.jsrdxzw.flashsale.domain.model.PageResult;
import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import com.jsrdxzw.flashsale.domain.model.enums.FlashItemStatus;
import com.jsrdxzw.flashsale.domain.repository.FlashItemRepository;
import com.jsrdxzw.flashsale.domain.service.FlashItemDomainService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.jsrdxzw.flashsale.domain.exception.DomainErrorCode.*;


@Slf4j
@Service
public class FlashItemDomainServiceImpl implements FlashItemDomainService {

    @Autowired
    private FlashItemRepository flashItemRepository;

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Override
    public void publishFlashItem(FlashItem flashItem) {
        log.info("itemPublish|发布秒杀品|{}", JSONUtil.toJSONString(flashItem));
        if (flashItem == null || !flashItem.validateParamsForCreate()) {
            throw new DomainException(ONLINE_FLASH_ITEM_PARAMS_INVALID);
        }
        flashItem.setStatus(FlashItemStatus.PUBLISHED.getCode());
        flashItemRepository.save(flashItem);
        log.info("itemPublish|秒杀品已发布|{}", flashItem.getId());

        FlashItemEvent flashItemEvent = new FlashItemEvent();
        flashItemEvent.setEventType(FlashItemEventType.PUBLISHED);
        flashItemEvent.setFlashItem(flashItem);
        domainEventPublisher.publish(flashItemEvent);
        log.info("itemPublish|秒杀品发布事件已发布|{}", flashItem.getId());
    }

    @Override
    public void onlineFlashItem(Long itemId) {
        log.info("itemOnline|上线秒杀品|{}", itemId);
        FlashItem flashItem = getFlashItemByItemId(itemId);
        if (FlashItemStatus.isOnline(flashItem.getStatus())) {
            return;
        }
        flashItem.setStatus(FlashItemStatus.ONLINE.getCode());
        flashItemRepository.save(flashItem);
        log.info("itemOnline|秒杀品已上线|{}", itemId);

        FlashItemEvent flashItemPublishEvent = new FlashItemEvent();
        flashItemPublishEvent.setEventType(FlashItemEventType.ONLINE);
        flashItemPublishEvent.setFlashItem(flashItem);
        domainEventPublisher.publish(flashItemPublishEvent);
        log.info("itemOnline|秒杀品上线事件已发布|{}", itemId);
    }

    @Override
    public void offlineFlashItem(Long itemId) {
        log.info("itemOffline|下线秒杀品|{}", itemId);
        FlashItem flashItem = getFlashItemByItemId(itemId);
        if (FlashItemStatus.isOffline(flashItem.getStatus())) {
            return;
        }
        flashItem.setStatus(FlashItemStatus.OFFLINE.getCode());
        flashItemRepository.save(flashItem);
        log.info("itemOffline|秒杀品已下线|{}", itemId);

        FlashItemEvent flashItemEvent = new FlashItemEvent();
        flashItemEvent.setEventType(FlashItemEventType.OFFLINE);
        flashItemEvent.setFlashItem(flashItem);
        domainEventPublisher.publish(flashItemEvent);
        log.info("itemOffline|秒杀品下线事件已发布|{}", itemId);
    }

    @Override
    public PageResult<FlashItem> getFlashItems(PagesQueryCondition pagesQueryCondition) {
        if (pagesQueryCondition == null) {
            pagesQueryCondition = new PagesQueryCondition();
        }
        List<FlashItem> flashItems = flashItemRepository.findFlashItemsByCondition(pagesQueryCondition.buildParams());
        Integer total = flashItemRepository.countFlashItemsByCondition(pagesQueryCondition);
        log.info("Get flash items:{}", flashItems.size());
        return PageResult.with(flashItems, total);
    }

    @Override
    public FlashItem getFlashItem(Long itemId) {
        if (itemId == null) {
            throw new DomainException(PARAMS_INVALID);
        }
        Optional<FlashItem> flashItemOptional = flashItemRepository.findById(itemId);
        if (flashItemOptional.isEmpty()) {
            throw new DomainException(FLASH_ITEM_DOES_NOT_EXIST);
        }
        return flashItemOptional.get();
    }

    private FlashItem getFlashItemByItemId(Long itemId) {
        if (itemId == null) {
            throw new DomainException(PARAMS_INVALID);
        }
        Optional<FlashItem> flashItemOptional = flashItemRepository.findById(itemId);
        if (flashItemOptional.isEmpty()) {
            throw new DomainException(FLASH_ITEM_DOES_NOT_EXIST);
        }
        return flashItemOptional.get();
    }
}
