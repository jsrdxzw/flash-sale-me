package com.jsrdxzw.flashsale.app.service.bucket;

import com.alibaba.fastjson.JSON;
import com.jsrdxzw.flashsale.app.exception.AppException;
import com.jsrdxzw.flashsale.app.exception.BizException;
import com.jsrdxzw.flashsale.app.model.command.BucketsArrangementCommand;
import com.jsrdxzw.flashsale.app.model.dto.StockBucketSummaryDTO;
import com.jsrdxzw.flashsale.app.model.result.AppSimpleResult;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import com.jsrdxzw.flashsale.domain.service.FlashItemDomainService;
import com.jsrdxzw.flashsale.lock.DistributedLock;
import com.jsrdxzw.flashsale.lock.DistributedLockFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.jsrdxzw.flashsale.app.exception.AppErrorCode.*;
import static com.jsrdxzw.flashsale.util.StringHelper.link;

/**
 * @author xuzhiwei
 * @date 2021/12/12 10:07 PM
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
public class DefaultBucketsAPPService implements BucketsAPPService {
    private static final String STOCK_BUCKET_ARRANGEMENT_KEY = "STOCK_BUCKET_ARRANGEMENT_KEY";

    @Resource
    private DistributedLockFactoryService lockFactoryService;

    @Resource
    private FlashItemDomainService flashItemDomainService;

    @Resource
    private BucketsArrangementService bucketsArrangementService;

    @Override
    public AppSimpleResult<?> arrangeStockBuckets(Long userId, Long itemId, BucketsArrangementCommand arrangementCommand) {
        log.info("arrangeBuckets|编排库存分桶|{},{},{}", userId, itemId, JSON.toJSON(arrangementCommand));
        String arrangementKey = getArrangementKey(userId, itemId);
        DistributedLock arrangementLock = lockFactoryService.getDistributedLock(arrangementKey);
        try {
            boolean isLockSuccess = arrangementLock.tryLock(5, 5, TimeUnit.SECONDS);
            if (!isLockSuccess) {
                return AppSimpleResult.failed(FREQUENTLY_ERROR.getErrCode(), FREQUENTLY_ERROR.getErrDesc());
            }
            FlashItem flashItem = flashItemDomainService.getFlashItem(itemId);
            if (flashItem == null) {
                throw new BizException(ITEM_NOT_FOUND.getErrDesc());
            }
            bucketsArrangementService.arrangeStockBuckets(itemId, arrangementCommand.getTotalStocksAmount(),
                    arrangementCommand.getBucketsQuantity(), arrangementCommand.getArrangementMode());
            log.info("arrangeBuckets|库存编排完成|{}", itemId);
            return AppSimpleResult.ok(true);
        } catch (AppException e) {
            log.error("arrangeBuckets|库存编排失败|{}", itemId, e);
            return AppSimpleResult.failed(BUSINESS_ERROR.getErrCode(), e.getMessage());
        } catch (Exception e) {
            log.error("arrangeBuckets|库存编排错误|{}", itemId, e);
            return AppSimpleResult.failed(ARRANGE_STOCK_BUCKETS_FAILED);
        } finally {
            arrangementLock.forceUnlock();
        }
    }

    @Override
    public AppSimpleResult<StockBucketSummaryDTO> getStockBucketsSummary(Long userId, Long itemId) {
        log.info("stockBucketsSummary|获取库存分桶数据|{},{}", userId, itemId);
        try {
            StockBucketSummaryDTO stockBucketSummaryDTO = bucketsArrangementService.queryStockBucketsSummary(itemId);
            return AppSimpleResult.ok(stockBucketSummaryDTO);
        } catch (BizException e) {
            log.error("stockBucketsSummary|获取库存分桶数据失败|{}", itemId, e);
            return AppSimpleResult.failed(QUERY_STOCK_BUCKETS_FAILED);
        } catch (Exception e) {
            log.error("stockBucketsSummary|获取库存分桶数据错误|{}", itemId, e);
            return AppSimpleResult.failed(QUERY_STOCK_BUCKETS_FAILED);
        }
    }

    private String getArrangementKey(Long userId, Long itemId) {
        return link(STOCK_BUCKET_ARRANGEMENT_KEY, userId, itemId);
    }
}
