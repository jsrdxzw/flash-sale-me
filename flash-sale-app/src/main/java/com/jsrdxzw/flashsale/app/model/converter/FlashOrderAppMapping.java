package com.jsrdxzw.flashsale.app.model.converter;

import com.jsrdxzw.flashsale.app.model.command.FlashPlaceOrderCommand;
import com.jsrdxzw.flashsale.domain.model.entity.FlashOrder;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author xuzhiwei
 * @date 2021/12/5 8:08 PM
 */
@Mapper
public interface FlashOrderAppMapping {
    FlashOrderAppMapping INSTANCE = Mappers.getMapper(FlashOrderAppMapping.class);

    FlashOrder toDomain(FlashPlaceOrderCommand placeOrderCommand);
}
