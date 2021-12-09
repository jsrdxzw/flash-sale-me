package com.jsrdxzw.flashsale.controller.model.converter;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.jsrdxzw.flashsale.app.model.command.FlashActivityPublishCommand;
import com.jsrdxzw.flashsale.app.model.dto.FlashActivityDTO;
import com.jsrdxzw.flashsale.app.model.result.AppMultiResult;
import com.jsrdxzw.flashsale.app.model.result.AppResult;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;
import com.jsrdxzw.flashsale.controller.model.request.FlashActivityPublishRequest;
import com.jsrdxzw.flashsale.controller.model.response.FlashActivityResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author xuzhiwei
 * @date 2021/12/2 4:49 PM
 */
@Mapper
public interface FlashActivityControllerMapping {
    FlashActivityControllerMapping INSTANCE = Mappers.getMapper(FlashActivityControllerMapping.class);

    FlashActivityPublishCommand toCommand(FlashActivityPublishRequest flashActivityPublishRequest);

    Response with(AppResult appResult);

    MultiResponse<FlashActivityResponse> withMulti(AppMultiResult<FlashActivityDTO> flashActivitiesResult);

    SingleResponse<FlashActivityResponse> withSingle(AppSimpleResult<FlashActivityDTO> flashActivityResult);
}
