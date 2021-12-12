package com.jsrdxzw.flashsale.controller.resource;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.jsrdxzw.flashsale.app.model.dto.FlashItemDTO;
import com.jsrdxzw.flashsale.app.model.query.FlashItemsQuery;
import com.jsrdxzw.flashsale.app.model.result.AppMultiResult;
import com.jsrdxzw.flashsale.app.model.result.AppResult;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;
import com.jsrdxzw.flashsale.app.schedule.FlashItemWarmUpScheduler;
import com.jsrdxzw.flashsale.app.service.item.FlashItemAppService;
import com.jsrdxzw.flashsale.controller.model.converter.FlashItemControllerMapping;
import com.jsrdxzw.flashsale.controller.model.request.FlashItemPublishRequest;
import com.jsrdxzw.flashsale.controller.model.response.FlashItemResponse;
import com.jsrdxzw.flashsale.domain.model.enums.FlashItemStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author xuzhiwei
 * @date 2021/12/4 11:13 AM
 */
@RestController
public class FlashItemController {
    @Autowired
    private FlashItemAppService flashItemAppService;
    @Autowired
    private FlashItemWarmUpScheduler flashItemWarmUpScheduler;

    @PostMapping(value = "/activities/{activityId}/flash-items")
    public Response publishFlashItem(@RequestAttribute Long userId,
                                     @PathVariable Long activityId,
                                     @RequestBody FlashItemPublishRequest flashItemPublishRequest) {
        AppResult publishResult = flashItemAppService.publishFlashItem(
                userId, activityId, FlashItemControllerMapping.INSTANCE.toCommand(flashItemPublishRequest));
        return FlashItemControllerMapping.INSTANCE.with(publishResult);

    }

    @GetMapping(value = "/activities/{activityId}/flash-items")
    @SentinelResource("GetFlashItems")
    public MultiResponse<FlashItemDTO> getFlashItems(@RequestAttribute Long userId,
                                                     @PathVariable Long activityId,
                                                     @RequestParam Integer pageSize,
                                                     @RequestParam Integer pageNumber,
                                                     @RequestParam(required = false) String keyword) {
        FlashItemsQuery flashItemsQuery = new FlashItemsQuery()
                .setKeyword(keyword)
                .setPageSize(pageSize)
                .setPageNumber(pageNumber);
        AppMultiResult<FlashItemDTO> flashItemsResult = flashItemAppService.getFlashItems(userId, activityId, flashItemsQuery);
        return FlashItemControllerMapping.INSTANCE.withMulti(flashItemsResult);
    }

    @GetMapping(value = "/activities/{activityId}/flash-items/online")
    @SentinelResource("GetOnlineFlashItems")
    public MultiResponse<FlashItemResponse> getOnlineFlashItems(@RequestAttribute Long userId,
                                                                @PathVariable Long activityId,
                                                                @RequestParam Integer pageSize,
                                                                @RequestParam Integer pageNumber,
                                                                @RequestParam(required = false) String keyword) {
        FlashItemsQuery flashItemsQuery = new FlashItemsQuery()
                .setKeyword(keyword)
                .setPageSize(pageSize)
                .setPageNumber(pageNumber)
                .setStatus(FlashItemStatus.ONLINE.getCode());
        AppMultiResult<FlashItemDTO> flashItemsResult = flashItemAppService.getFlashItems(userId, activityId, flashItemsQuery);
        return FlashItemControllerMapping.INSTANCE.withMultiResponse(flashItemsResult);
    }

    @GetMapping(value = "/activities/{activityId}/flash-items/{itemId}")
    @SentinelResource("GetFlashItem")
    public SingleResponse<FlashItemResponse> getFlashItem(@RequestAttribute Long userId,
                                                          @PathVariable Long activityId,
                                                          @PathVariable Long itemId,
                                                          @RequestParam(required = false) Long version) {
        AppSimpleResult<FlashItemDTO> flashItemResult = flashItemAppService.getFlashItem(userId, activityId, itemId, version);
        return FlashItemControllerMapping.INSTANCE.withSingle(flashItemResult);
    }

    @PutMapping(value = "/activities/{activityId}/flash-items/{itemId}/online")
    public Response onlineFlashItem(@RequestAttribute Long userId, @PathVariable Long activityId, @PathVariable Long itemId) {
        AppResult onlineResult = flashItemAppService.onlineFlashItem(userId, activityId, itemId);
        return FlashItemControllerMapping.INSTANCE.with(onlineResult);
    }

    @PutMapping(value = "/activities/{activityId}/flash-items/{itemId}/offline")
    public Response offlineFlashItem(@RequestAttribute Long userId, @PathVariable Long activityId, @PathVariable Long itemId) {
        AppResult onlineResult = flashItemAppService.onlineFlashItem(userId, activityId, itemId);
        return FlashItemControllerMapping.INSTANCE.with(onlineResult);
    }

    /**
     * 手动预热库存
     *
     * @return
     */
    @PostMapping("/activities/flash-items/warmUp")
    public Response warmUp() {
        flashItemWarmUpScheduler.warmUpFlashItemTask();
        return Response.buildSuccess();
    }
}
