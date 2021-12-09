package com.jsrdxzw.flashsale.domain.service.impl;

import com.jsrdxzw.flashsale.domain.event.DomainEventPublisher;
import com.jsrdxzw.flashsale.domain.event.FlashOrderEvent;
import com.jsrdxzw.flashsale.domain.event.FlashOrderEventType;
import com.jsrdxzw.flashsale.domain.exception.DomainException;
import com.jsrdxzw.flashsale.domain.model.PageResult;
import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashOrder;
import com.jsrdxzw.flashsale.domain.model.enums.FlashOrderStatus;
import com.jsrdxzw.flashsale.domain.repository.FlashOrderRepository;
import com.jsrdxzw.flashsale.domain.service.FlashOrderDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.jsrdxzw.flashsale.domain.exception.DomainErrorCode.FLASH_ITEM_DOES_NOT_EXIST;
import static com.jsrdxzw.flashsale.domain.exception.DomainErrorCode.PARAMS_INVALID;

/**
 * @author xuzhiwei
 * @date 2021/12/5 10:55 AM
 */
@Service
@Slf4j
public class FlashOrderDomainServiceImpl implements FlashOrderDomainService {

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private FlashOrderRepository flashOrderRepository;

    @Override
    public boolean placeOrder(Long userId, FlashOrder flashOrder) {
        log.info("placeOrder|下单|{},{}", userId, flashOrder);
        if (flashOrder == null || !flashOrder.validateParamsForCreate()) {
            throw new DomainException(PARAMS_INVALID);
        }
        flashOrder.setStatus(FlashOrderStatus.CREATED.getCode());
        boolean saveSuccess = flashOrderRepository.save(flashOrder);
        if (saveSuccess) {
            FlashOrderEvent flashOrderEvent = new FlashOrderEvent();
            flashOrderEvent.setEventType(FlashOrderEventType.CREATED);
            domainEventPublisher.publish(flashOrderEvent);
        }
        log.info("placeOrder|订单已创建成功|{},{}", userId, flashOrder);
        return saveSuccess;
    }

    @Override
    public PageResult<FlashOrder> getOrdersByUser(Long userId, PagesQueryCondition pagesQueryCondition) {
        if (pagesQueryCondition == null) {
            pagesQueryCondition = new PagesQueryCondition();
        }
        List<FlashOrder> flashOrders = flashOrderRepository.findFlashOrdersByCondition(pagesQueryCondition.buildParams());
        int total = flashOrderRepository.countFlashOrdersByCondition(pagesQueryCondition.buildParams());
        log.info("Get flash orders:{},{}", userId, flashOrders.size());
        return PageResult.with(flashOrders, total);
    }

    @Override
    public List<FlashOrder> getOrders(PagesQueryCondition pagesQueryCondition) {
        if (pagesQueryCondition == null) {
            pagesQueryCondition = new PagesQueryCondition();
        }
        List<FlashOrder> flashOrders = flashOrderRepository.findFlashOrdersByCondition(pagesQueryCondition.buildParams());
        log.info("Get flash orders:{},{}", flashOrders.size(), flashOrders);
        return flashOrders;
    }

    @Override
    public FlashOrder getOrder(Long userId, Long orderId) {
        if (StringUtils.isEmpty(userId) || orderId == null) {
            throw new DomainException(PARAMS_INVALID);
        }
        Optional<FlashOrder> flashOrderOptional = flashOrderRepository.findById(orderId);
        if (flashOrderOptional.isEmpty()) {
            throw new DomainException(FLASH_ITEM_DOES_NOT_EXIST);
        }
        return flashOrderOptional.get();
    }

    @Override
    public boolean cancelOrder(Long userId, Long orderId) {
        log.info("placeOrder|取消订单|{},{}", userId, orderId);
        if (userId == null || orderId == null) {
            throw new DomainException(PARAMS_INVALID);
        }
        Optional<FlashOrder> flashOrderOptional = flashOrderRepository.findById(orderId);
        if (flashOrderOptional.isEmpty()) {
            throw new DomainException(FLASH_ITEM_DOES_NOT_EXIST);
        }
        FlashOrder flashOrder = flashOrderOptional.get();
        if (!flashOrder.getUserId().equals(userId)) {
            throw new DomainException(FLASH_ITEM_DOES_NOT_EXIST);
        }
        if (FlashOrderStatus.isCanceled(flashOrder.getStatus())) {
            return false;
        }
        flashOrder.setStatus(FlashOrderStatus.CANCELED.getCode());
        boolean saveSuccess = flashOrderRepository.updateStatus(flashOrder);
        if (saveSuccess) {
            FlashOrderEvent flashOrderEvent = new FlashOrderEvent();
            flashOrderEvent.setEventType(FlashOrderEventType.CANCEL);
            domainEventPublisher.publish(flashOrderEvent);
        }
        log.info("placeOrder|订单已取消|{},{}", userId, orderId);
        return saveSuccess;
    }
}
