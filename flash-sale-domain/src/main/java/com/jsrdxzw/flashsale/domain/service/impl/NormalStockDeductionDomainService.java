package com.jsrdxzw.flashsale.domain.service.impl;

import com.jsrdxzw.flashsale.domain.exception.DomainException;
import com.jsrdxzw.flashsale.domain.model.StockDeduction;
import com.jsrdxzw.flashsale.domain.repository.FlashItemRepository;
import com.jsrdxzw.flashsale.domain.service.StockDeductionDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import static com.jsrdxzw.flashsale.domain.exception.DomainErrorCode.PARAMS_INVALID;

/**
 * @author xuzhiwei
 * @date 2021/12/5 8:13 PM
 */
@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "normal", matchIfMissing = true)
public class NormalStockDeductionDomainService implements StockDeductionDomainService {
    @Autowired
    private FlashItemRepository flashItemRepository;

    @Override
    public boolean decreaseItemStock(StockDeduction stockDeduction) {
        precheck(stockDeduction);
        return flashItemRepository.decreaseItemStock(stockDeduction.getItemId(), stockDeduction.getQuantity());
    }

    @Override
    public boolean increaseItemStock(StockDeduction stockDeduction) {
        precheck(stockDeduction);
        return flashItemRepository.increaseItemStock(stockDeduction.getItemId(), stockDeduction.getQuantity());
    }

    private void precheck(StockDeduction stockDeduction) {
        if (stockDeduction == null || stockDeduction.getItemId() == null || stockDeduction.getQuantity() == null) {
            throw new DomainException(PARAMS_INVALID);
        }
    }
}
