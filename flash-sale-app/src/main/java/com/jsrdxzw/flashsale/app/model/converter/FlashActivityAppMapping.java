package com.jsrdxzw.flashsale.app.model.converter;

import com.jsrdxzw.flashsale.app.model.command.FlashActivityPublishCommand;
import com.jsrdxzw.flashsale.app.model.dto.FlashActivityDTO;
import com.jsrdxzw.flashsale.app.model.query.FlashActivitiesQuery;
import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashActivity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author xuzhiwei
 * @date 2021/12/4 10:05 AM
 */
@Mapper
public interface FlashActivityAppMapping {
    FlashActivityAppMapping INSTANCE = Mappers.getMapper(FlashActivityAppMapping.class);

    FlashActivity toDomain(FlashActivityPublishCommand flashActivityPublishCommand);

    PagesQueryCondition toFlashActivitiesQuery(FlashActivitiesQuery flashActivitiesQuery);

    FlashActivityDTO toFlashActivityDTO(FlashActivity flashActivity);
}
