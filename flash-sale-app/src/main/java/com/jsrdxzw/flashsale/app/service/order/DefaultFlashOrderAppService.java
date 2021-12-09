package com.jsrdxzw.flashsale.app.service.order;

import com.alibaba.fastjson.JSON;
import com.jsrdxzw.flashsale.app.exception.BizException;
import com.jsrdxzw.flashsale.app.model.command.FlashPlaceOrderCommand;
import com.jsrdxzw.flashsale.app.model.dto.FlashOrderDTO;
import com.jsrdxzw.flashsale.app.model.query.FlashOrdersQuery;
import com.jsrdxzw.flashsale.app.model.result.*;
import com.jsrdxzw.flashsale.app.security.SecurityService;
import com.jsrdxzw.flashsale.app.service.placeorder.PlaceOrderService;
import com.jsrdxzw.flashsale.domain.service.FlashOrderDomainService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import com.jsrdxzw.flashsale.lock.DistributedLock;
import com.jsrdxzw.flashsale.lock.DistributedLockFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.jsrdxzw.flashsale.app.exception.AppErrorCode.*;
import static com.jsrdxzw.flashsale.util.StringHelper.link;

/**
 * @author xuzhiwei
 * @date 2021/12/5 10:02 AM
 */
@Slf4j
@Service
public class DefaultFlashOrderAppService implements FlashOrderAppService {
    private static final String PLACE_ORDER_LOCK_KEY = "PLACE_ORDER_LOCK_KEY";

    @Autowired
    private FlashOrderDomainService flashOrderDomainService;

    @Autowired
    private DistributedLockFactoryService lockFactoryService;

    @Autowired
    private PlaceOrderService placeOrderService;

    @Autowired
    private SecurityService securityService;

    @Override
    public AppSimpleResult<PlaceOrderResult> placeOrder(Long userId, FlashPlaceOrderCommand placeOrderCommand) {
        log.info("placeOrder|下单|{},{}", userId, JSONUtil.toJSONString(placeOrderCommand));
        if (userId == null || placeOrderCommand == null || !placeOrderCommand.validateParams()) {
            throw new BizException(INVALID_PARAMS);
        }
        // 根据userId设置分布式锁，防止抖动
        String placeOrderLockKey = getPlaceOrderLockKey(userId);
        DistributedLock placeOrderLock = lockFactoryService.getDistributedLock(placeOrderLockKey);
        try {
            boolean isLockSuccess = placeOrderLock.tryLock(5, 5, TimeUnit.SECONDS);
            if (!isLockSuccess) {
                return AppSimpleResult.failed(FREQUENTLY_ERROR.getErrCode(), FREQUENTLY_ERROR.getErrDesc());
            }
            boolean isPassRiskInspect = securityService.inspectRisksByPolicy(userId);
            if (!isPassRiskInspect) {
                log.info("placeOrder|综合风控检验未通过|{}", userId);
                return AppSimpleResult.failed(PLACE_ORDER_FAILED);
            }
            PlaceOrderResult placeOrderResult = placeOrderService.doPlaceOrder(userId, placeOrderCommand);
            if (!placeOrderResult.isSuccess()) {
                return AppSimpleResult.failed(placeOrderResult.getCode(), placeOrderResult.getMessage());
            }
            log.info("placeOrder|下单完成|{}", userId);
            return AppSimpleResult.ok(placeOrderResult);
        } catch (Exception e) {
            log.error("placeOrder|下单失败|{},{}", userId, JSON.toJSONString(placeOrderCommand), e);
            return AppSimpleResult.failed(PLACE_ORDER_FAILED);
        } finally {
            placeOrderLock.forceUnlock();
        }
    }

    @Override
    public AppSimpleResult<OrderTaskHandleResult> getPlaceOrderTaskResult(Long userId, Long itemId, String placeOrderTaskId) {
        return null;
    }

    @Override
    public AppMultiResult<FlashOrderDTO> getOrdersByUser(Long userId, FlashOrdersQuery flashOrdersQuery) {
        return null;
    }

    @Override
    public AppResult cancelOrder(Long userId, Long orderId) {
        return null;
    }

    private String getPlaceOrderLockKey(Long userId) {
        return link(PLACE_ORDER_LOCK_KEY, userId);
    }
}
