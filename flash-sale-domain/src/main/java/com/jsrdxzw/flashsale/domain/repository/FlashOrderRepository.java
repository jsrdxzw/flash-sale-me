package com.jsrdxzw.flashsale.domain.repository;

import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashOrder;

import java.util.List;
import java.util.Optional;

/**
 * @author xuzhiwei
 * @date 2021/12/5 12:06 PM
 */
public interface FlashOrderRepository {
    boolean save(FlashOrder flashOrder);

    boolean updateStatus(FlashOrder flashOrder);

    Optional<FlashOrder> findById(Long orderId);

    List<FlashOrder> findFlashOrdersByCondition(PagesQueryCondition pagesQueryCondition);

    int countFlashOrdersByCondition(PagesQueryCondition buildParams);
}
