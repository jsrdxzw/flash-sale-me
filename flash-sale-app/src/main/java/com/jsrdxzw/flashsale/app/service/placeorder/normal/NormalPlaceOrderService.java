package com.jsrdxzw.flashsale.app.service.placeorder.normal;

import com.jsrdxzw.flashsale.app.exception.BizException;
import com.jsrdxzw.flashsale.app.model.command.FlashPlaceOrderCommand;
import com.jsrdxzw.flashsale.app.model.converter.FlashOrderAppMapping;
import com.jsrdxzw.flashsale.app.model.result.PlaceOrderResult;
import com.jsrdxzw.flashsale.app.service.item.FlashItemAppService;
import com.jsrdxzw.flashsale.app.service.placeorder.PlaceOrderService;
import com.jsrdxzw.flashsale.app.service.stock.ItemStockCacheService;
import com.jsrdxzw.flashsale.app.util.MultiPlaceOrderTypesCondition;
import com.jsrdxzw.flashsale.app.util.OrderNoGenerateContext;
import com.jsrdxzw.flashsale.app.util.OrderNoGenerateService;
import com.jsrdxzw.flashsale.domain.model.StockDeduction;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import com.jsrdxzw.flashsale.domain.model.entity.FlashOrder;
import com.jsrdxzw.flashsale.domain.service.FlashActivityDomainService;
import com.jsrdxzw.flashsale.domain.service.FlashItemDomainService;
import com.jsrdxzw.flashsale.domain.service.FlashOrderDomainService;
import com.jsrdxzw.flashsale.domain.service.StockDeductionDomainService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static com.jsrdxzw.flashsale.app.exception.AppErrorCode.INVALID_PARAMS;
import static com.jsrdxzw.flashsale.app.exception.AppErrorCode.PLACE_ORDER_FAILED;

/**
 * @author xuzhiwei
 * @date 2021/12/5 5:27 PM
 */
@Slf4j
@Service
@Conditional(MultiPlaceOrderTypesCondition.class)
public class NormalPlaceOrderService implements PlaceOrderService {

    @Resource
    private FlashActivityDomainService flashActivityDomainService;

    @Resource
    private FlashItemAppService flashItemAppService;

    @Resource
    private FlashItemDomainService flashItemDomainService;

    @Resource
    private OrderNoGenerateService orderNoGenerateService;

    @Resource
    private ItemStockCacheService itemStockCacheService;

    @Resource
    private StockDeductionDomainService stockDeductionDomainService;

    @Resource
    private FlashOrderDomainService flashOrderDomainService;

    @PostConstruct
    public void init() {
        log.info("initPlaceOrderService|默认下单服务已经初始化");
    }

    @Override
    public PlaceOrderResult doPlaceOrder(Long userId, FlashPlaceOrderCommand placeOrderCommand) {
        log.info("placeOrder|开始下单|{},{}", userId, JSONUtil.toJSONString(placeOrderCommand));
        if (userId == null || placeOrderCommand == null || !placeOrderCommand.validateParams()) {
            throw new BizException(INVALID_PARAMS);
        }
        boolean isActivityAllowPlaceOrder = flashActivityDomainService.isAllowPlaceOrderOrNot(placeOrderCommand.getActivityId());
        if (!isActivityAllowPlaceOrder) {
            log.info("placeOrder|秒杀活动下单规则校验未通过|{},{}", userId, placeOrderCommand.getActivityId());
            return PlaceOrderResult.failed(PLACE_ORDER_FAILED);
        }
        boolean isItemAllowPlaceOrder = flashItemAppService.isAllowPlaceOrderOrNot(placeOrderCommand.getItemId());
        if (!isItemAllowPlaceOrder) {
            log.info("placeOrder|秒杀品下单规则校验未通过|{},{}", userId, placeOrderCommand.getActivityId());
            return PlaceOrderResult.failed(PLACE_ORDER_FAILED);
        }
        FlashItem flashItem = flashItemDomainService.getFlashItem(placeOrderCommand.getItemId());
        Long orderId = orderNoGenerateService.generateOrderNo(new OrderNoGenerateContext());
        FlashOrder flashOrderToPlace = FlashOrderAppMapping.INSTANCE.toDomain(placeOrderCommand);
        flashOrderToPlace.setItemTitle(flashItem.getItemTitle());
        flashOrderToPlace.setFlashPrice(flashItem.getFlashPrice());
        flashOrderToPlace.setUserId(userId);
        flashOrderToPlace.setId(orderId);
        StockDeduction stockDeduction = new StockDeduction()
                .setItemId(placeOrderCommand.getItemId())
                .setQuantity(placeOrderCommand.getQuantity())
                .setUserId(userId);

        boolean preDecreaseStockSuccess = false;
        try {
            preDecreaseStockSuccess = itemStockCacheService.decreaseItemStock(stockDeduction);
            if (!preDecreaseStockSuccess) {
                log.info("placeOrder|库存预扣减失败|{},{}", userId, JSONUtil.toJSONString(placeOrderCommand));
                return PlaceOrderResult.failed(PLACE_ORDER_FAILED.getErrCode(), PLACE_ORDER_FAILED.getErrDesc());
            }
            boolean decreaseStockSuccess = stockDeductionDomainService.decreaseItemStock(stockDeduction);
            if (!decreaseStockSuccess) {
                log.info("placeOrder|库存扣减失败|{},{}", userId, JSONUtil.toJSONString(placeOrderCommand));
                return PlaceOrderResult.failed(PLACE_ORDER_FAILED.getErrCode(), PLACE_ORDER_FAILED.getErrDesc());
            }
            boolean placeOrderSuccess = flashOrderDomainService.placeOrder(userId, flashOrderToPlace);
            if (!placeOrderSuccess) {
                throw new BizException(PLACE_ORDER_FAILED.getErrDesc());
            }
        } catch (Exception e) {
            if (preDecreaseStockSuccess) {
                boolean recoverStockSuccess = itemStockCacheService.increaseItemStock(stockDeduction);
                if (!recoverStockSuccess) {
                    log.error("placeOrder|预扣库存恢复失败|{},{}", userId, JSONUtil.toJSONString(placeOrderCommand), e);
                }
            }
            log.error("placeOrder|下单失败|{},{}", userId, JSONUtil.toJSONString(placeOrderCommand), e);
            throw new BizException(PLACE_ORDER_FAILED.getErrDesc());
        }
        log.info("placeOrder|下单成功|{},{}", userId, orderId);
        return PlaceOrderResult.ok(orderId);
    }
}
