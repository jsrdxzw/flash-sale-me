package com.jsrdxzw.flashsale.domain.event;

import com.alibaba.cola.event.DomainEventI;
import com.alibaba.cola.event.EventBusI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 发布领域事件，一般基于独立的消息中间件系统发布；
 * 为方便示例，本系统采用本地事件发布。
 *
 * @author xuzhiwei
 * @date 2021/12/2 3:39 PM
 */
@Component
public class LocalDomainEventPublisher implements DomainEventPublisher {

    @Autowired
    private EventBusI eventBus;

    @Override
    public void publish(DomainEventI domainEvent) {
        eventBus.fire(domainEvent);
    }
}
