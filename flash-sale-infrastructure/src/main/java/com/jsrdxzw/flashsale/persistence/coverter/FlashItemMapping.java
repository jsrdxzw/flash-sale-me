package com.jsrdxzw.flashsale.persistence.coverter;

import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import com.jsrdxzw.flashsale.domain.model.entity.FlashOrder;
import com.jsrdxzw.flashsale.persistence.model.FlashItemDO;
import com.jsrdxzw.flashsale.persistence.model.FlashOrderDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author xuzhiwei
 * @date 2021/12/4 5:26 PM
 */
@Mapper
public interface FlashItemMapping {
    FlashItemMapping INSTANCE = Mappers.getMapper(FlashItemMapping.class);

    FlashItemDO toDataObjectForCreate(FlashItem flashItem);

    FlashItem toDomainObject(FlashItemDO flashItemDO);

    List<FlashOrder> toDomainList(List<FlashOrderDO> orders);
}
