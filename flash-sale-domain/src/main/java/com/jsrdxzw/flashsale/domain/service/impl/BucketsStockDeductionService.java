package com.jsrdxzw.flashsale.domain.service.impl;

import com.jsrdxzw.flashsale.domain.exception.DomainException;
import com.jsrdxzw.flashsale.domain.model.StockDeduction;
import com.jsrdxzw.flashsale.domain.repository.BucketsRepository;
import com.jsrdxzw.flashsale.domain.service.StockDeductionDomainService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.jsrdxzw.flashsale.domain.exception.DomainErrorCode.PARAMS_INVALID;

/**
 * @author xuzhiwei
 * @date 2021/12/13 4:45 PM
 */
@Slf4j
@ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
@Service
public class BucketsStockDeductionService implements StockDeductionDomainService {

    @Resource
    private BucketsRepository bucketsRepository;

    @Override
    public boolean decreaseItemStock(StockDeduction stockDeduction) {
        log.info("decreaseItemStock|扣减库存|{}", JSONUtil.toJSONString(stockDeduction));
        if (stockDeduction == null || stockDeduction.getItemId() == null || stockDeduction.getQuantity() == null || stockDeduction.getSerialNo() == null) {
            throw new DomainException(PARAMS_INVALID);
        }
        return bucketsRepository.decreaseItemStock(stockDeduction.getItemId(), stockDeduction.getQuantity(), stockDeduction.getSerialNo());
    }

    @Override
    public boolean increaseItemStock(StockDeduction stockDeduction) {
        log.info("increaseItemStock|恢复库存|{}", JSONUtil.toJSONString(stockDeduction));
        if (stockDeduction == null || stockDeduction.getItemId() == null || stockDeduction.getQuantity() == null || stockDeduction.getSerialNo() == null) {
            throw new DomainException(PARAMS_INVALID);
        }
        return bucketsRepository.increaseItemStock(stockDeduction.getItemId(), stockDeduction.getQuantity(), stockDeduction.getSerialNo());
    }
}
