package com.jsrdxzw.flashsale.persistence;

import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import com.jsrdxzw.flashsale.domain.repository.FlashItemRepository;
import com.jsrdxzw.flashsale.persistence.coverter.FlashItemMapping;
import com.jsrdxzw.flashsale.persistence.mapper.FlashItemMapper;
import com.jsrdxzw.flashsale.persistence.model.FlashItemDO;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author xuzhiwei
 * @date 2021/12/3 5:37 PM
 */
@Repository
public class FlashItemRepositoryImpl implements FlashItemRepository {
    @Resource
    private FlashItemMapper flashItemMapper;

    @Override
    public int save(FlashItem flashItem) {
        FlashItemDO flashItemDO = FlashItemMapping.INSTANCE.toDataObjectForCreate(flashItem);
        if (flashItem.getId() == null) {
            return flashItemMapper.insert(flashItemDO);
        }
        return flashItemMapper.update(flashItemDO);
    }

    @Override
    public Optional<FlashItem> findById(Long itemId) {
        FlashItemDO flashItemDO = flashItemMapper.getById(itemId);
        if (flashItemDO == null) {
            return Optional.empty();
        }
        FlashItem flashItem = FlashItemMapping.INSTANCE.toDomainObject(flashItemDO);
        return Optional.of(flashItem);
    }

    @Override
    public List<FlashItem> findFlashItemsByCondition(PagesQueryCondition pagesQueryCondition) {
        return flashItemMapper.findFlashItemsByCondition(pagesQueryCondition)
                .stream()
                .map(FlashItemMapping.INSTANCE::toDomainObject)
                .collect(Collectors.toList());
    }

    @Override
    public Integer countFlashItemsByCondition(PagesQueryCondition pagesQueryCondition) {
        return flashItemMapper.countFlashItemsByCondition(pagesQueryCondition);
    }

    @Override
    public boolean decreaseItemStock(Long itemId, Integer quantity) {
        return flashItemMapper.decreaseItemStock(itemId, quantity) == 1;
    }

    @Override
    public boolean increaseItemStock(Long itemId, Integer quantity) {
        return flashItemMapper.increaseItemStock(itemId, quantity) == 1;
    }
}
