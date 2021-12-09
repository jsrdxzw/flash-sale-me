package com.jsrdxzw.flashsale.domain.repository;

import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashActivity;

import java.util.List;
import java.util.Optional;

/**
 * @author xuzhiwei
 * @date 2021/12/2 2:45 PM
 */
public interface FlashActivityRepository {
    int save(FlashActivity flashActivity);

    Optional<FlashActivity> findById(Long activityId);

    List<FlashActivity> findFlashActivitiesByCondition(PagesQueryCondition pagesQueryCondition);

    Integer countFlashActivitiesByCondition(PagesQueryCondition pagesQueryCondition);
}
