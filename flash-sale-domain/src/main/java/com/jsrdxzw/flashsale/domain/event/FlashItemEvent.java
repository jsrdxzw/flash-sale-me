package com.jsrdxzw.flashsale.domain.event;

import com.alibaba.cola.event.DomainEventI;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import lombok.Data;

/**
 * @author xuzhiwei
 * @date 2021/12/3 5:40 PM
 */
@Data
public class FlashItemEvent implements DomainEventI {
    private FlashItemEventType eventType;
    private FlashItem flashItem;

    public Long getId() {
        if (flashItem == null) {
            return null;
        }
        return flashItem.getId();
    }

    public Long getFlashActivityId() {
        if (flashItem == null) {
            return null;
        }
        return flashItem.getActivityId();
    }
}
