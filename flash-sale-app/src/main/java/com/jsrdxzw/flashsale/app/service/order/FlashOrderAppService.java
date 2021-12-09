package com.jsrdxzw.flashsale.app.service.order;

import com.jsrdxzw.flashsale.app.model.command.FlashPlaceOrderCommand;
import com.jsrdxzw.flashsale.app.model.dto.FlashOrderDTO;
import com.jsrdxzw.flashsale.app.model.query.FlashOrdersQuery;
import com.jsrdxzw.flashsale.app.model.result.*;

/**
 * @author xuzhiwei
 * @date 2021/12/4 11:11 PM
 */
public interface FlashOrderAppService {
    AppSimpleResult<PlaceOrderResult> placeOrder(Long userId, FlashPlaceOrderCommand placeOrderCommand);

    AppSimpleResult<OrderTaskHandleResult> getPlaceOrderTaskResult(Long userId, Long itemId, String placeOrderTaskId);

    AppMultiResult<FlashOrderDTO> getOrdersByUser(Long userId, FlashOrdersQuery flashOrdersQuery);

    AppResult cancelOrder(Long userId, Long orderId);
}
