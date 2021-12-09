package com.jsrdxzw.flashsale.persistence.mapper;

import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.persistence.model.FlashOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author xuzhiwei
 * @date 2021/12/5 12:09 PM
 */
@Mapper
public interface FlashOrderMapper {
    int insert(FlashOrderDO flashOrderDO);

    int updateStatus(FlashOrderDO flashOrderDO);

    FlashOrderDO getById(@Param("orderId") Long orderId);

    List<FlashOrderDO> findFlashOrdersByCondition(PagesQueryCondition pagesQueryCondition);

    Integer countFlashOrdersByCondition();
}
