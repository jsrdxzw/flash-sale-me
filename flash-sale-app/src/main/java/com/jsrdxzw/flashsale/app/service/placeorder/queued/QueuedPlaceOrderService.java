package com.jsrdxzw.flashsale.app.service.placeorder.queued;

import com.alibaba.fastjson.JSON;
import com.jsrdxzw.flashsale.app.exception.BizException;
import com.jsrdxzw.flashsale.app.model.PlaceOrderTask;
import com.jsrdxzw.flashsale.app.model.command.FlashPlaceOrderCommand;
import com.jsrdxzw.flashsale.app.model.converter.FlashOrderAppMapping;
import com.jsrdxzw.flashsale.app.model.converter.PlaceOrderTaskMapping;
import com.jsrdxzw.flashsale.app.model.dto.FlashItemDTO;
import com.jsrdxzw.flashsale.app.model.enums.OrderTaskStatus;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;
import com.jsrdxzw.flashsale.app.model.result.OrderTaskHandleResult;
import com.jsrdxzw.flashsale.app.model.result.OrderTaskSubmitResult;
import com.jsrdxzw.flashsale.app.model.result.PlaceOrderResult;
import com.jsrdxzw.flashsale.app.service.item.FlashItemAppService;
import com.jsrdxzw.flashsale.app.service.placeorder.PlaceOrderService;
import com.jsrdxzw.flashsale.app.util.OrderNoGenerateContext;
import com.jsrdxzw.flashsale.app.util.OrderNoGenerateService;
import com.jsrdxzw.flashsale.app.util.OrderTaskIdGenerateService;
import com.jsrdxzw.flashsale.cache.redis.RedisCacheService;
import com.jsrdxzw.flashsale.domain.model.StockDeduction;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import com.jsrdxzw.flashsale.domain.model.entity.FlashOrder;
import com.jsrdxzw.flashsale.domain.service.FlashActivityDomainService;
import com.jsrdxzw.flashsale.domain.service.FlashItemDomainService;
import com.jsrdxzw.flashsale.domain.service.FlashOrderDomainService;
import com.jsrdxzw.flashsale.domain.service.StockDeductionDomainService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import static com.jsrdxzw.flashsale.app.exception.AppErrorCode.*;
import static com.jsrdxzw.flashsale.app.model.constants.CacheConstants.HOURS_24;

/**
 * @author xuzhiwei
 * @date 2021/12/10 2:33 PM
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "queued")
public class QueuedPlaceOrderService implements PlaceOrderService {
    private static final String PLACE_ORDER_TASK_ORDER_ID_KEY = "PLACE_ORDER_TASK_ORDER_ID_KEY_";

    @Autowired
    private FlashItemAppService flashItemAppService;

    @Autowired
    private OrderTaskIdGenerateService orderTaskIdGenerateService;

    @Autowired
    private PlaceOrderTaskService placeOrderTaskService;

    @Autowired
    private FlashActivityDomainService flashActivityDomainService;

    @Autowired
    private FlashOrderDomainService flashOrderDomainService;

    @Autowired
    private FlashItemDomainService flashItemDomainService;

    @Autowired
    private StockDeductionDomainService stockDeductionDomainService;

    @Autowired
    private OrderNoGenerateService orderNoGenerateService;

    @Autowired
    private RedisCacheService redisCacheService;

    @PostConstruct
    public void init() {
        log.info("initPlaceOrderService|异步队列下单服务已经初始化");
    }

    @Override
    public PlaceOrderResult doPlaceOrder(Long userId, FlashPlaceOrderCommand placeOrderCommand) {
        log.info("placeOrder|开始下单|{},{}", userId, JSONUtil.toJSONString(placeOrderCommand));
        if (placeOrderCommand == null || !placeOrderCommand.validateParams()) {
            return PlaceOrderResult.failed(INVALID_PARAMS);
        }
        AppSimpleResult<FlashItemDTO> flashItemResult = flashItemAppService.getFlashItem(placeOrderCommand.getItemId());
        if (!flashItemResult.isSuccess() || flashItemResult.getData() == null) {
            log.info("placeOrder|获取秒杀品失败|{},{}", userId, placeOrderCommand.getActivityId());
            return PlaceOrderResult.failed(GET_ITEM_FAILED);
        }

        FlashItemDTO flashItemDTO = flashItemResult.getData();
        if (!flashItemDTO.isOnSale()) {
            log.info("placeOrder|当前非在售时间|{},{}", userId, placeOrderCommand.getActivityId());
            return PlaceOrderResult.failed(ITEM_NOT_ON_SALE);
        }

        // userId_itemId, 放到mq中
        String placeOrderTaskId = orderTaskIdGenerateService.generatePlaceOrderTaskId(userId, placeOrderCommand.getItemId());
        PlaceOrderTask placeOrderTask = PlaceOrderTaskMapping.INSTANCE.with(userId, placeOrderCommand);
        placeOrderTask.setPlaceOrderTaskId(placeOrderTaskId);
        OrderTaskSubmitResult submitResult = placeOrderTaskService.submit(placeOrderTask);

        log.info("placeOrder|任务提交结果|{},{},{}", userId, placeOrderTaskId, JSONUtil.toJSONString(placeOrderTask));

        if (!submitResult.isSuccess()) {
            log.info("placeOrder|下单任务提交失败|{},{}", userId, placeOrderCommand.getActivityId());
            return PlaceOrderResult.failed(submitResult.getCode(), submitResult.getMessage());
        }
        log.info("placeOrder|下单任务提交完成|{},{}", userId, placeOrderTaskId);
        return PlaceOrderResult.ok(placeOrderTaskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handlePlaceOrderTask(PlaceOrderTask placeOrderTask) {
        try {
            Long userId = placeOrderTask.getUserId();
            boolean isActivityAllowPlaceOrder = flashActivityDomainService.isAllowPlaceOrderOrNot(placeOrderTask.getActivityId());
            if (!isActivityAllowPlaceOrder) {
                log.info("handleOrderTask|秒杀活动下单规则校验未通过|{},{}", placeOrderTask.getPlaceOrderTaskId(), placeOrderTask.getActivityId());
                placeOrderTaskService.updateTaskHandleResult(placeOrderTask.getPlaceOrderTaskId(), false);
                return;
            }
            boolean isItemAllowPlaceOrder = flashItemAppService.isAllowPlaceOrderOrNot(placeOrderTask.getItemId());
            if (!isItemAllowPlaceOrder) {
                log.info("handleOrderTask|秒杀品下单规则校验未通过|{},{}", placeOrderTask.getPlaceOrderTaskId(), placeOrderTask.getActivityId());
                placeOrderTaskService.updateTaskHandleResult(placeOrderTask.getPlaceOrderTaskId(), false);
                return;
            }
            FlashItem flashItem = flashItemDomainService.getFlashItem(placeOrderTask.getItemId());
            Long orderId = orderNoGenerateService.generateOrderNo(new OrderNoGenerateContext());
            FlashOrder flashOrderToPlace = FlashOrderAppMapping.INSTANCE.PlaceOrderTaskToDomain(placeOrderTask);
            flashOrderToPlace.setItemTitle(flashItem.getItemTitle());
            flashOrderToPlace.setFlashPrice(flashItem.getFlashPrice());
            flashOrderToPlace.setUserId(userId);
            flashOrderToPlace.setId(orderId);

            StockDeduction stockDeduction = new StockDeduction()
                    .setItemId(placeOrderTask.getItemId())
                    .setQuantity(placeOrderTask.getQuantity());
            boolean decreaseStockSuccess = stockDeductionDomainService.decreaseItemStock(stockDeduction);
            if (!decreaseStockSuccess) {
                log.info("handleOrderTask|库存扣减失败|{},{}", placeOrderTask.getPlaceOrderTaskId(), JSON.toJSONString(placeOrderTask));
                return;
            }
            boolean placeOrderSuccess = flashOrderDomainService.placeOrder(userId, flashOrderToPlace);
            if (!placeOrderSuccess) {
                throw new BizException(PLACE_ORDER_FAILED.getErrDesc());
            }
            placeOrderTaskService.updateTaskHandleResult(placeOrderTask.getPlaceOrderTaskId(), true);
            redisCacheService.put(PLACE_ORDER_TASK_ORDER_ID_KEY + placeOrderTask.getPlaceOrderTaskId(), orderId, HOURS_24);
            log.info("handleOrderTask|下单任务处理完成|{},{}", placeOrderTask.getPlaceOrderTaskId(), JSON.toJSONString(placeOrderTask));
        } catch (Exception e) {
            placeOrderTaskService.updateTaskHandleResult(placeOrderTask.getPlaceOrderTaskId(), false);
            log.error("handleOrderTask|下单任务处理错误|{},{}", placeOrderTask.getPlaceOrderTaskId(), JSON.toJSONString(placeOrderTask), e);
            throw new BizException(e.getMessage());
        }
    }

    public OrderTaskHandleResult getPlaceOrderResult(Long userId, Long itemId, String placeOrderTaskId) {
        String generatedPlaceOrderTaskId = orderTaskIdGenerateService.generatePlaceOrderTaskId(userId, itemId);
        if (!generatedPlaceOrderTaskId.equals(placeOrderTaskId)) {
            return OrderTaskHandleResult.failed(PLACE_ORDER_TASK_ID_INVALID);
        }
        OrderTaskStatus orderTaskStatus = placeOrderTaskService.getTaskStatus(placeOrderTaskId);
        if (orderTaskStatus == null) {
            return OrderTaskHandleResult.failed(PLACE_ORDER_TASK_ID_INVALID);
        }
        if (!OrderTaskStatus.SUCCESS.equals(orderTaskStatus)) {
            return OrderTaskHandleResult.failed(orderTaskStatus);
        }
        Long orderId = redisCacheService.getObject(PLACE_ORDER_TASK_ORDER_ID_KEY + placeOrderTaskId, Long.class);
        return OrderTaskHandleResult.ok(orderId);
    }
}
