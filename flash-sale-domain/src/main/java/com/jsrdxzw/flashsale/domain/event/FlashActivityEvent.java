package com.jsrdxzw.flashsale.domain.event;

import com.alibaba.cola.event.DomainEventI;
import com.jsrdxzw.flashsale.domain.model.entity.FlashActivity;
import lombok.Data;

/**
 * @author xuzhiwei
 * @date 2021/12/2 3:35 PM
 */
@Data
public class FlashActivityEvent implements DomainEventI {
    private FlashActivityEventType eventType;
    private FlashActivity flashActivity;

    public Long getId() {
        if (flashActivity == null) {
            return null;
        }
        return flashActivity.getId();
    }
}
