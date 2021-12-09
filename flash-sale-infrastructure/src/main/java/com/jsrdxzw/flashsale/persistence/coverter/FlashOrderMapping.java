package com.jsrdxzw.flashsale.persistence.coverter;

import com.jsrdxzw.flashsale.domain.model.entity.FlashOrder;
import com.jsrdxzw.flashsale.persistence.model.FlashOrderDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author xuzhiwei
 * @date 2021/12/5 12:12 PM
 */
@Mapper
public interface FlashOrderMapping {
    FlashOrderMapping INSTANCE = Mappers.getMapper(FlashOrderMapping.class);

    FlashOrderDO toDataObjectForCreate(FlashOrder flashOrder);

    FlashOrder toDomainObject(FlashOrderDO flashOrderDO);
}
