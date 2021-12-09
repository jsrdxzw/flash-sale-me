package com.jsrdxzw.flashsale.domain.repository;

import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;

import java.util.List;
import java.util.Optional;

/**
 * @author xuzhiwei
 * @date 2021/12/3 5:36 PM
 */
public interface FlashItemRepository {
    int save(FlashItem flashItem);

    Optional<FlashItem> findById(Long itemId);

    List<FlashItem> findFlashItemsByCondition(PagesQueryCondition pagesQueryCondition);

    Integer countFlashItemsByCondition(PagesQueryCondition pagesQueryCondition);

    boolean decreaseItemStock(Long itemId, Integer quantity);

    boolean increaseItemStock(Long itemId, Integer quantity);
}
