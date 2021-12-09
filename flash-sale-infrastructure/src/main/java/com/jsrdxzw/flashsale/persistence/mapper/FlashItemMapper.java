package com.jsrdxzw.flashsale.persistence.mapper;

import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.persistence.model.FlashItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author xuzhiwei
 * @date 2021/12/3 5:38 PM
 */
@Mapper
public interface FlashItemMapper {
    int insert(FlashItemDO flashItemDO);

    int update(FlashItemDO flashItemDO);

    FlashItemDO getById(@Param("itemId") Long itemId);

    List<FlashItemDO> findFlashItemsByCondition(PagesQueryCondition pagesQueryCondition);

    Integer countFlashItemsByCondition(PagesQueryCondition pagesQueryCondition);

    int decreaseItemStock(@Param("itemId") Long itemId, @Param("quantity") Integer quantity);

    int increaseItemStock(@Param("itemId") Long itemId, @Param("quantity") Integer quantity);
}
