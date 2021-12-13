package com.jsrdxzw.flashsale.controller.resource;

import com.alibaba.cola.dto.SingleResponse;
import com.jsrdxzw.flashsale.app.model.command.BucketsArrangementCommand;
import com.jsrdxzw.flashsale.app.model.dto.StockBucketSummaryDTO;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;
import com.jsrdxzw.flashsale.app.service.bucket.BucketsAPPService;
import com.jsrdxzw.flashsale.controller.model.converter.BucketsControllerMapping;
import com.jsrdxzw.flashsale.controller.model.request.BucketsArrangementRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author xuzhiwei
 * @date 2021/12/12 9:58 PM
 */
@RestController
@ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
public class BucketsStockController {
    @Resource
    private BucketsAPPService bucketsAPPService;

    @PostMapping(value = "/items/{itemId}/buckets")
    public SingleResponse<?> arrangeStockBuckets(@RequestAttribute Long userId, @PathVariable Long itemId, @RequestBody BucketsArrangementRequest bucketsArrangementRequest) {
        BucketsArrangementCommand bucketsArrangementCommand = BucketsControllerMapping.INSTANCE.toCommand(bucketsArrangementRequest);
        AppSimpleResult<?> arrangementResult = bucketsAPPService.arrangeStockBuckets(userId, itemId, bucketsArrangementCommand);
        if (!arrangementResult.isSuccess()) {
            return SingleResponse.buildFailure(arrangementResult.getCode(), arrangementResult.getMessage());
        }
        return SingleResponse.buildSuccess();
    }

    @GetMapping(value = "/items/{itemId}/buckets")
    public SingleResponse<StockBucketSummaryDTO> getBuckets(@RequestAttribute Long userId, @PathVariable Long itemId) {
        AppSimpleResult<StockBucketSummaryDTO> bucketsSummaryResult = bucketsAPPService.getStockBucketsSummary(userId, itemId);
        if (!bucketsSummaryResult.isSuccess() || bucketsSummaryResult.getData() == null) {
            return BucketsControllerMapping.INSTANCE.withSingle(bucketsSummaryResult);
        }
        return SingleResponse.of(bucketsSummaryResult.getData());
    }
}
