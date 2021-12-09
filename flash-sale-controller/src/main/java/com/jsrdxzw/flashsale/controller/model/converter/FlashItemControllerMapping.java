package com.jsrdxzw.flashsale.controller.model.converter;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.jsrdxzw.flashsale.app.model.command.FlashItemPublishCommand;
import com.jsrdxzw.flashsale.app.model.dto.FlashItemDTO;
import com.jsrdxzw.flashsale.app.model.result.AppMultiResult;
import com.jsrdxzw.flashsale.app.model.result.AppResult;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;
import com.jsrdxzw.flashsale.controller.model.request.FlashItemPublishRequest;
import com.jsrdxzw.flashsale.controller.model.response.FlashItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author xuzhiwei
 * @date 2021/12/4 11:15 AM
 */
@Mapper
public interface FlashItemControllerMapping {
    FlashItemControllerMapping INSTANCE = Mappers.getMapper(FlashItemControllerMapping.class);

    FlashItemPublishCommand toCommand(FlashItemPublishRequest flashItemPublishRequest);

    Response with(AppResult publishResult);

    MultiResponse<FlashItemDTO> withMulti(AppMultiResult<FlashItemDTO> flashItemsResult);

    MultiResponse<FlashItemResponse> withMultiResponse(AppMultiResult<FlashItemDTO> flashItemsResult);

    SingleResponse<FlashItemResponse> withSingle(AppSimpleResult<FlashItemDTO> flashItemResult);
}
