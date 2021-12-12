package com.jsrdxzw.flashsale.app.model.converter;

import com.jsrdxzw.flashsale.app.model.PlaceOrderTask;
import com.jsrdxzw.flashsale.app.model.command.FlashPlaceOrderCommand;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author xuzhiwei
 * @date 2021/12/10 2:48 PM
 */
@Mapper
public interface PlaceOrderTaskMapping {
    PlaceOrderTaskMapping INSTANCE = Mappers.getMapper(PlaceOrderTaskMapping.class);

    PlaceOrderTask with(Long userId, FlashPlaceOrderCommand placeOrderCommand);
}
