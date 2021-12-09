package com.jsrdxzw.flashsale.app.service.placeorder;

import com.jsrdxzw.flashsale.app.model.command.FlashPlaceOrderCommand;
import com.jsrdxzw.flashsale.app.model.result.PlaceOrderResult;

/**
 * @author xuzhiwei
 * @date 2021/12/5 5:26 PM
 */
public interface PlaceOrderService {
    PlaceOrderResult doPlaceOrder(Long userId, FlashPlaceOrderCommand placeOrderCommand);
}
