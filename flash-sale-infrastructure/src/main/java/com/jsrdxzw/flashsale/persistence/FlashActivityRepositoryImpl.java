package com.jsrdxzw.flashsale.persistence;

import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashActivity;
import com.jsrdxzw.flashsale.domain.repository.FlashActivityRepository;
import com.jsrdxzw.flashsale.persistence.coverter.FlashActivityMapping;
import com.jsrdxzw.flashsale.persistence.mapper.FlashActivityMapper;
import com.jsrdxzw.flashsale.persistence.model.FlashActivityDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author xuzhiwei
 * @date 2021/12/2 2:54 PM
 */
@Repository
public class FlashActivityRepositoryImpl implements FlashActivityRepository {

    @Autowired
    private FlashActivityMapper flashActivityMapper;

    @Override
    public int save(FlashActivity flashActivity) {
        FlashActivityDO flashActivityDO = FlashActivityMapping.INSTANCE.toDataObjectForCreate(flashActivity);
        if (flashActivityDO.getId() == null) {
            int effectedRows = flashActivityMapper.insert(flashActivityDO);
            flashActivity.setId(flashActivityDO.getId());
            return effectedRows;
        }
        return flashActivityMapper.update(flashActivityDO);
    }

    @Override
    public Optional<FlashActivity> findById(Long activityId) {
        FlashActivityDO flashActivityDO = flashActivityMapper.getById(activityId);
        if (flashActivityDO == null) {
            return Optional.empty();
        }
        FlashActivity flashActivity = FlashActivityMapping.INSTANCE.toDomainObject(flashActivityDO);
        return Optional.of(flashActivity);
    }

    @Override
    public List<FlashActivity> findFlashActivitiesByCondition(PagesQueryCondition pagesQueryCondition) {
        return flashActivityMapper.findFlashActivitiesByCondition(pagesQueryCondition)
                .stream()
                .map(FlashActivityMapping.INSTANCE::toDomainObject)
                .collect(Collectors.toList());
    }

    @Override
    public Integer countFlashActivitiesByCondition(PagesQueryCondition pagesQueryCondition) {
        return flashActivityMapper.countFlashActivitiesByCondition(pagesQueryCondition);
    }
}
