package com.jsrdxzw.flashsale.domain.event;

import com.alibaba.cola.event.DomainEventI;

/**
 * @author xuzhiwei
 * @date 2021/12/2 3:39 PM
 */
public interface DomainEventPublisher {
    void publish(DomainEventI domainEvent);
}
