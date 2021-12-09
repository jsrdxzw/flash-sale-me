package com.jsrdxzw.flashsale.persistence.coverter;

import com.jsrdxzw.flashsale.domain.model.entity.FlashActivity;
import com.jsrdxzw.flashsale.persistence.model.FlashActivityDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author xuzhiwei
 * @date 2021/12/4 10:14 AM
 */
@Mapper
public interface FlashActivityMapping {
    FlashActivityMapping INSTANCE = Mappers.getMapper(FlashActivityMapping.class);

    FlashActivityDO toDataObjectForCreate(FlashActivity flashActivity);

    FlashActivity toDomainObject(FlashActivityDO flashActivityDO);
}
