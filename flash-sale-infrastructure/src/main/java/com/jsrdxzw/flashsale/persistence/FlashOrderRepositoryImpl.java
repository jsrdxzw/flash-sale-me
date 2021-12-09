package com.jsrdxzw.flashsale.persistence;

import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashOrder;
import com.jsrdxzw.flashsale.domain.repository.FlashOrderRepository;
import com.jsrdxzw.flashsale.persistence.coverter.FlashItemMapping;
import com.jsrdxzw.flashsale.persistence.coverter.FlashOrderMapping;
import com.jsrdxzw.flashsale.persistence.mapper.FlashOrderMapper;
import com.jsrdxzw.flashsale.persistence.model.FlashOrderDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author xuzhiwei
 * @date 2021/12/5 12:08 PM
 */
@Repository
public class FlashOrderRepositoryImpl implements FlashOrderRepository {

    @Autowired
    private FlashOrderMapper flashOrderMapper;

    @Override
    public boolean save(FlashOrder flashOrder) {
        FlashOrderDO flashOrderDO = FlashOrderMapping.INSTANCE.toDataObjectForCreate(flashOrder);
        int effectedRows = flashOrderMapper.insert(flashOrderDO);
        return effectedRows == 1;
    }

    @Override
    public boolean updateStatus(FlashOrder flashOrder) {
        FlashOrderDO flashOrderDO = FlashOrderMapping.INSTANCE.toDataObjectForCreate(flashOrder);
        int effectedRows = flashOrderMapper.updateStatus(flashOrderDO);
        return effectedRows == 1;
    }

    @Override
    public Optional<FlashOrder> findById(Long orderId) {
        FlashOrderDO flashOrderDO = flashOrderMapper.getById(orderId);
        if (flashOrderDO == null) {
            return Optional.empty();
        }
        FlashOrder flashOrder = FlashOrderMapping.INSTANCE.toDomainObject(flashOrderDO);
        return Optional.of(flashOrder);
    }

    @Override
    public List<FlashOrder> findFlashOrdersByCondition(PagesQueryCondition pagesQueryCondition) {
        List<FlashOrderDO> orders = flashOrderMapper.findFlashOrdersByCondition(pagesQueryCondition);
        return FlashItemMapping.INSTANCE.toDomainList(orders);
    }

    @Override
    public int countFlashOrdersByCondition(PagesQueryCondition buildParams) {
        return flashOrderMapper.countFlashOrdersByCondition();
    }
}
