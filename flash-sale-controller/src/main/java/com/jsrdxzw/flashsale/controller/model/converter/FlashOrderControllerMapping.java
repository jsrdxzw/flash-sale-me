package com.jsrdxzw.flashsale.controller.model.converter;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.jsrdxzw.flashsale.app.model.command.FlashPlaceOrderCommand;
import com.jsrdxzw.flashsale.app.model.dto.FlashOrderDTO;
import com.jsrdxzw.flashsale.app.model.result.*;
import com.jsrdxzw.flashsale.controller.model.request.FlashPlaceOrderRequest;
import com.jsrdxzw.flashsale.controller.model.response.FlashOrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.Collection;

/**
 * @author xuzhiwei
 * @date 2021/12/5 10:31 AM
 */
@Mapper
public interface FlashOrderControllerMapping {
    FlashOrderControllerMapping INSTANCE = Mappers.getMapper(FlashOrderControllerMapping.class);

    FlashPlaceOrderCommand toCommand(FlashPlaceOrderRequest flashPlaceOrderRequest);

    Collection<FlashOrderResponse> toFlashOrdersResponse(Collection<FlashOrderDTO> data);

    Response with(AppResult appResult);

    MultiResponse<FlashOrderResponse> withMulti(AppMultiResult<FlashOrderDTO> flashOrdersResult);

    SingleResponse<PlaceOrderResult> withSinglePlaceOrderResult(AppSimpleResult<PlaceOrderResult> placeOrderResult);

    SingleResponse<OrderTaskHandleResult> withSingleOrderTaskHandleResult(AppSimpleResult<OrderTaskHandleResult> placeOrderTaskResult);
}
