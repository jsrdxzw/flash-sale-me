package com.jsrdxzw.flashsale.controller.resource;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.jsrdxzw.flashsale.app.model.command.FlashPlaceOrderCommand;
import com.jsrdxzw.flashsale.app.model.dto.FlashOrderDTO;
import com.jsrdxzw.flashsale.app.model.query.FlashOrdersQuery;
import com.jsrdxzw.flashsale.app.model.result.*;
import com.jsrdxzw.flashsale.app.service.order.FlashOrderAppService;
import com.jsrdxzw.flashsale.controller.model.converter.FlashOrderControllerMapping;
import com.jsrdxzw.flashsale.controller.model.request.FlashPlaceOrderRequest;
import com.jsrdxzw.flashsale.controller.model.response.FlashOrderResponse;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author xuzhiwei
 * @date 2021/12/4 11:10 PM
 */
@RestController
public class FlashOrderController {
    @Resource
    private FlashOrderAppService flashOrderAppService;

    @PostMapping(value = "/flash-orders")
    @SentinelResource("PlaceOrderResource")
    public SingleResponse<PlaceOrderResult> placeOrder(@RequestAttribute Long userId, @RequestBody FlashPlaceOrderRequest flashPlaceOrderRequest) {
        FlashPlaceOrderCommand placeOrderCommand = FlashOrderControllerMapping.INSTANCE.toCommand(flashPlaceOrderRequest);
        AppSimpleResult<PlaceOrderResult> placeOrderResult = flashOrderAppService.placeOrder(userId, placeOrderCommand);
        if (!placeOrderResult.isSuccess() || placeOrderResult.getData() == null) {
            return FlashOrderControllerMapping.INSTANCE.withSinglePlaceOrderResult(placeOrderResult);
        }
        return SingleResponse.of(placeOrderResult.getData());
    }

    @GetMapping(value = "/items/{itemId}/flash-orders/{placeOrderTaskId}")
    @SentinelResource("PlaceOrderTask")
    public SingleResponse<OrderTaskHandleResult> getPlaceOrderTaskResult(@RequestAttribute Long userId, @PathVariable Long itemId, @PathVariable String placeOrderTaskId) {
        AppSimpleResult<OrderTaskHandleResult> placeOrderTaskResult = flashOrderAppService.getPlaceOrderTaskResult(userId, itemId, placeOrderTaskId);
        if (!placeOrderTaskResult.isSuccess() || placeOrderTaskResult.getData() == null) {
            return FlashOrderControllerMapping.INSTANCE.withSingleOrderTaskHandleResult(placeOrderTaskResult);
        }
        return SingleResponse.of(placeOrderTaskResult.getData());
    }

    @GetMapping(value = "/flash-orders/my")
    public MultiResponse<FlashOrderResponse> myOrders(@RequestAttribute Long userId,
                                                      @RequestParam Integer pageSize,
                                                      @RequestParam Integer pageNumber,
                                                      @RequestParam(required = false) String keyword) {
        FlashOrdersQuery flashOrdersQuery = new FlashOrdersQuery()
                .setKeyword(keyword)
                .setPageSize(pageSize)
                .setPageNumber(pageNumber);

        AppMultiResult<FlashOrderDTO> flashOrdersResult = flashOrderAppService.getOrdersByUser(userId, flashOrdersQuery);
        if (!flashOrdersResult.isSuccess() || flashOrdersResult.getData() == null) {
            return FlashOrderControllerMapping.INSTANCE.withMulti(flashOrdersResult);
        }
        return MultiResponse.of(FlashOrderControllerMapping.INSTANCE.toFlashOrdersResponse(flashOrdersResult.getData()), flashOrdersResult.getTotal());
    }

    @PutMapping(value = "/flash-orders/{orderId}/cancel")
    public Response cancelOrder(@RequestAttribute Long userId, @PathVariable Long orderId) {
        AppResult appResult = flashOrderAppService.cancelOrder(userId, orderId);
        return FlashOrderControllerMapping.INSTANCE.with(appResult);
    }
}
