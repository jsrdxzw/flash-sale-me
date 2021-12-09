package com.jsrdxzw.flashsale.app.service.activity;


import com.jsrdxzw.flashsale.app.model.command.FlashActivityPublishCommand;
import com.jsrdxzw.flashsale.app.model.dto.FlashActivityDTO;
import com.jsrdxzw.flashsale.app.model.query.FlashActivitiesQuery;
import com.jsrdxzw.flashsale.app.model.result.AppMultiResult;
import com.jsrdxzw.flashsale.app.model.result.AppResult;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;

public interface FlashActivityAppService {
    AppMultiResult<FlashActivityDTO> getFlashActivities(Long userId, FlashActivitiesQuery flashActivitiesQuery);

    AppSimpleResult<FlashActivityDTO> getFlashActivity(Long userId, Long activityId, Long version);

    AppResult publishFlashActivity(Long userId, FlashActivityPublishCommand flashActivityPublishCommand);

    AppResult modifyFlashActivity(Long userId, Long activityId, FlashActivityPublishCommand flashActivityPublishCommand);

    AppResult onlineFlashActivity(Long userId, Long activityId);

    AppResult offlineFlashActivity(Long userId, Long activityId);
}
