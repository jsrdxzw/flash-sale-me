package com.jsrdxzw.flashsale.app.service.order;

import com.alibaba.fastjson.JSON;
import com.jsrdxzw.flashsale.app.exception.BizException;
import com.jsrdxzw.flashsale.app.model.command.FlashPlaceOrderCommand;
import com.jsrdxzw.flashsale.app.model.converter.FlashOrderAppMapping;
import com.jsrdxzw.flashsale.app.model.dto.FlashOrderDTO;
import com.jsrdxzw.flashsale.app.model.query.FlashOrdersQuery;
import com.jsrdxzw.flashsale.app.model.result.*;
import com.jsrdxzw.flashsale.app.security.SecurityService;
import com.jsrdxzw.flashsale.app.service.placeorder.PlaceOrderService;
import com.jsrdxzw.flashsale.app.service.placeorder.queued.QueuedPlaceOrderService;
import com.jsrdxzw.flashsale.app.service.stock.ItemStockCacheService;
import com.jsrdxzw.flashsale.domain.model.PageResult;
import com.jsrdxzw.flashsale.domain.model.StockDeduction;
import com.jsrdxzw.flashsale.domain.model.entity.FlashOrder;
import com.jsrdxzw.flashsale.domain.service.FlashOrderDomainService;
import com.jsrdxzw.flashsale.domain.service.StockDeductionDomainService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import com.jsrdxzw.flashsale.lock.DistributedLock;
import com.jsrdxzw.flashsale.lock.DistributedLockFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Resource
    private FlashOrderDomainService flashOrderDomainService;

    @Resource
    private DistributedLockFactoryService lockFactoryService;

    @Resource
    private PlaceOrderService placeOrderService;

    @Resource
    private SecurityService securityService;

    @Resource
    private StockDeductionDomainService stockDeductionDomainService;

    @Resource
    private ItemStockCacheService itemStockCacheService;

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
        if (userId == null || itemId == null || StringUtils.isEmpty(placeOrderTaskId)) {
            throw new BizException(INVALID_PARAMS);
        }
        if (placeOrderService instanceof QueuedPlaceOrderService) {
            QueuedPlaceOrderService queuedPlaceOrderService = (QueuedPlaceOrderService) placeOrderService;
            OrderTaskHandleResult orderTaskHandleResult = queuedPlaceOrderService.getPlaceOrderResult(userId, itemId, placeOrderTaskId);
            if (!orderTaskHandleResult.isSuccess()) {
                return AppSimpleResult.failed(orderTaskHandleResult.getCode(), orderTaskHandleResult.getMessage(), orderTaskHandleResult);
            }
            return AppSimpleResult.ok(orderTaskHandleResult);
        } else {
            return AppSimpleResult.failed(ORDER_TYPE_NOT_SUPPORT);
        }
    }

    @Override
    public AppMultiResult<FlashOrderDTO> getOrdersByUser(Long userId, FlashOrdersQuery flashOrdersQuery) {
        PageResult<FlashOrder> flashOrderPageResult = flashOrderDomainService.getOrdersByUser(
                userId, FlashOrderAppMapping.INSTANCE.toFlashOrdersQuery(flashOrdersQuery));

        List<FlashOrderDTO> flashOrderDTOList = flashOrderPageResult.getData().stream()
                .map(FlashOrderAppMapping.INSTANCE::toFlashOrderDTO)
                .collect(Collectors.toList());
        return AppMultiResult.of(flashOrderDTOList, flashOrderPageResult.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AppResult cancelOrder(Long userId, Long orderId) {
        log.info("cancelOrder|取消订单|{},{}", userId, orderId);
        if (userId == null || orderId == null) {
            throw new BizException(INVALID_PARAMS);
        }
        FlashOrder flashOrder = flashOrderDomainService.getOrder(userId, orderId);
        if (flashOrder == null) {
            throw new BizException(ORDER_NOT_FOUND);
        }
        boolean cancelSuccess = flashOrderDomainService.cancelOrder(userId, orderId);
        if (!cancelSuccess) {
            log.info("cancelOrder|订单取消失败|{}", orderId);
            return AppResult.buildFailure(ORDER_CANCEL_FAILED);
        }
        StockDeduction stockDeduction = new StockDeduction()
                .setItemId(flashOrder.getItemId())
                .setQuantity(flashOrder.getQuantity());

        boolean stockRecoverSuccess = stockDeductionDomainService.increaseItemStock(stockDeduction);
        if (!stockRecoverSuccess) {
            log.info("cancelOrder|库存恢复失败|{}", orderId);
            throw new BizException(ORDER_CANCEL_FAILED);
        }
        boolean stockInRedisRecoverSuccess = itemStockCacheService.increaseItemStock(stockDeduction);
        if (!stockInRedisRecoverSuccess) {
            log.info("cancelOrder|Redis库存恢复失败|{}", orderId);
            throw new BizException(ORDER_CANCEL_FAILED);
        }
        log.info("cancelOrder|订单取消成功|{}", orderId);
        return AppResult.buildSuccess();
    }

    private String getPlaceOrderLockKey(Long userId) {
        return link(PLACE_ORDER_LOCK_KEY, userId);
    }
}
