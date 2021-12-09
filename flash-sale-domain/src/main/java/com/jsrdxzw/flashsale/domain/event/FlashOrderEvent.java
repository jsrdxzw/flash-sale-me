package com.jsrdxzw.flashsale.domain.event;

import com.alibaba.cola.event.DomainEventI;
import lombok.Data;

/**
 * @author xuzhiwei
 * @date 2021/12/5 10:58 AM
 */
@Data
public class FlashOrderEvent implements DomainEventI {
    private FlashOrderEventType eventType;
    private Long orderId;
}
