package com.jsrdxzw.flashsale.controller.resource;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.jsrdxzw.flashsale.app.model.command.FlashActivityPublishCommand;
import com.jsrdxzw.flashsale.app.model.dto.FlashActivityDTO;
import com.jsrdxzw.flashsale.app.model.query.FlashActivitiesQuery;
import com.jsrdxzw.flashsale.app.model.result.AppMultiResult;
import com.jsrdxzw.flashsale.app.model.result.AppResult;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;
import com.jsrdxzw.flashsale.app.service.activity.FlashActivityAppService;
import com.jsrdxzw.flashsale.controller.model.converter.FlashActivityControllerMapping;
import com.jsrdxzw.flashsale.controller.model.request.FlashActivityPublishRequest;
import com.jsrdxzw.flashsale.controller.model.response.FlashActivityResponse;
import com.jsrdxzw.flashsale.domain.model.enums.FlashActivityStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author xuzhiwei
 * @date 2021/12/1 5:55 下午
 */
@RestController
@RequestMapping("/flash-activities")
public class FlashActivityController {
    @Resource
    private FlashActivityAppService flashActivityAppService;

    @PostMapping
    public Response publishFlashActivity(@RequestAttribute Long userId, @RequestBody FlashActivityPublishRequest flashActivityPublishRequest) {
        FlashActivityPublishCommand activityPublishCommand = FlashActivityControllerMapping.INSTANCE.toCommand(flashActivityPublishRequest);
        AppResult appResult = flashActivityAppService.publishFlashActivity(userId, activityPublishCommand);
        return FlashActivityControllerMapping.INSTANCE.with(appResult);
    }

    @PutMapping(value = "/{activityId}")
    public Response modifyFlashActivity(@RequestAttribute Long userId, @PathVariable Long activityId, @RequestBody FlashActivityPublishRequest flashActivityPublishRequest) {
        FlashActivityPublishCommand activityPublishCommand = FlashActivityControllerMapping.INSTANCE.toCommand(flashActivityPublishRequest);
        AppResult appResult = flashActivityAppService.modifyFlashActivity(userId, activityId, activityPublishCommand);
        return FlashActivityControllerMapping.INSTANCE.with(appResult);
    }

    @GetMapping(value = "/online")
    @SentinelResource("GetOnlineActivitiesResource")
    public MultiResponse<FlashActivityResponse> getOnlineFlashActivities(@RequestAttribute Long userId,
                                                                         @RequestParam Integer pageSize,
                                                                         @RequestParam Integer pageNumber,
                                                                         @RequestParam(required = false) String keyword) {
        FlashActivitiesQuery flashActivitiesQuery = new FlashActivitiesQuery()
                .setKeyword(keyword)
                .setPageSize(pageSize)
                .setPageNumber(pageNumber)
                .setStatus(FlashActivityStatus.ONLINE.getCode());

        AppMultiResult<FlashActivityDTO> flashActivitiesResult = flashActivityAppService.getFlashActivities(userId, flashActivitiesQuery);
        return FlashActivityControllerMapping.INSTANCE.withMulti(flashActivitiesResult);
    }

    @GetMapping
    @SentinelResource("GetActivitiesResource")
    public MultiResponse<FlashActivityResponse> getFlashActivities(@RequestAttribute Long userId,
                                                                   @RequestParam Integer pageSize,
                                                                   @RequestParam Integer pageNumber,
                                                                   @RequestParam(required = false) String keyword) {
        FlashActivitiesQuery flashActivitiesQuery = new FlashActivitiesQuery()
                .setKeyword(keyword)
                .setPageSize(pageSize)
                .setPageNumber(pageNumber);
        AppMultiResult<FlashActivityDTO> flashActivitiesResult = flashActivityAppService.getFlashActivities(userId, flashActivitiesQuery);
        return FlashActivityControllerMapping.INSTANCE.withMulti(flashActivitiesResult);
    }

    @GetMapping(value = "/{activityId}")
    @SentinelResource("GetActivityResource")
    public SingleResponse<FlashActivityResponse> getFlashActivity(@RequestAttribute Long userId,
                                                                  @PathVariable Long activityId,
                                                                  @RequestParam(required = false) Long version) {
        AppSimpleResult<FlashActivityDTO> flashActivityResult = flashActivityAppService.getFlashActivity(userId, activityId, version);
        return FlashActivityControllerMapping.INSTANCE.withSingle(flashActivityResult);
    }

    @PutMapping(value = "/{activityId}/online")
    public Response onlineFlashActivity(@RequestAttribute Long userId, @PathVariable Long activityId) {
        AppResult appResult = flashActivityAppService.onlineFlashActivity(userId, activityId);
        return FlashActivityControllerMapping.INSTANCE.with(appResult);
    }

    @PutMapping(value = "/{activityId}/offline")
    public Response offlineFlashActivity(@RequestAttribute Long userId, @PathVariable Long activityId) {
        AppResult appResult = flashActivityAppService.offlineFlashActivity(userId, activityId);
        return FlashActivityControllerMapping.INSTANCE.with(appResult);
    }
}
