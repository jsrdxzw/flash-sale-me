package com.jsrdxzw.flashsale.app.model.converter;

import com.jsrdxzw.flashsale.app.model.command.FlashItemPublishCommand;
import com.jsrdxzw.flashsale.app.model.dto.FlashItemDTO;
import com.jsrdxzw.flashsale.app.model.query.FlashItemsQuery;
import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author xuzhiwei
 * @date 2021/12/4 12:59 PM
 */
@Mapper
public interface FlashItemAppMapping {
    FlashItemAppMapping INSTANCE = Mappers.getMapper(FlashItemAppMapping.class);

    FlashItem toDomain(FlashItemPublishCommand itemPublishCommand);

    FlashItemDTO toFlashItemDTO(FlashItem flashItem);

    PagesQueryCondition toFlashItemsQuery(FlashItemsQuery flashItemsQuery);
}
