package com.jsrdxzw.flashsale.controller.model.converter;

import com.alibaba.cola.dto.SingleResponse;
import com.jsrdxzw.flashsale.app.model.command.BucketsArrangementCommand;
import com.jsrdxzw.flashsale.app.model.dto.StockBucketSummaryDTO;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;
import com.jsrdxzw.flashsale.controller.model.request.BucketsArrangementRequest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author xuzhiwei
 * @date 2021/12/12 10:02 PM
 */
@Mapper
public interface BucketsControllerMapping {
    BucketsControllerMapping INSTANCE = Mappers.getMapper(BucketsControllerMapping.class);

    BucketsArrangementCommand toCommand(BucketsArrangementRequest bucketsArrangementRequest);

    SingleResponse<StockBucketSummaryDTO> withSingle(AppSimpleResult<StockBucketSummaryDTO> bucketsSummaryResult);
}
