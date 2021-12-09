package com.jsrdxzw.flashsale.domain.service;

import com.jsrdxzw.flashsale.domain.model.StockDeduction;

/**
 * @author xuzhiwei
 * @date 2021/12/5 8:12 PM
 */
public interface StockDeductionDomainService {
    /**
     * 库存扣减
     *
     * @param stockDeduction 库存扣减信息
     */
    boolean decreaseItemStock(StockDeduction stockDeduction);

    /**
     * 库存恢复
     *
     * @param stockDeduction 库存恢复信息
     */
    boolean increaseItemStock(StockDeduction stockDeduction);
}
