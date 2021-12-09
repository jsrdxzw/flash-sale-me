package com.jsrdxzw.flashsale.persistence.mapper;

import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.persistence.model.FlashActivityDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author xuzhiwei
 * @date 2021/12/2 3:16 PM
 */
@Mapper
public interface FlashActivityMapper {
    int insert(FlashActivityDO flashActivityDO);

    int update(FlashActivityDO flashActivityDO);

    FlashActivityDO getById(@Param("activityId") Long activityId);

    List<FlashActivityDO> findFlashActivitiesByCondition(PagesQueryCondition pagesQueryCondition);

    Integer countFlashActivitiesByCondition(PagesQueryCondition pagesQueryCondition);
}
