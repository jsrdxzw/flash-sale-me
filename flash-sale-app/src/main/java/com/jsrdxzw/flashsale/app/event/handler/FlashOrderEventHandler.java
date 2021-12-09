package com.jsrdxzw.flashsale.app.event.handler;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.event.EventHandler;
import com.alibaba.cola.event.EventHandlerI;
import com.jsrdxzw.flashsale.config.annotion.BetaTrace;
import com.jsrdxzw.flashsale.domain.event.FlashOrderEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xuzhiwei
 * @date 2021/12/5 12:19 PM
 */
@Slf4j
@EventHandler
public class FlashOrderEventHandler implements EventHandlerI<Response, FlashOrderEvent> {

    @BetaTrace
    @Override
    public Response execute(FlashOrderEvent flashOrderEvent) {
        log.info("orderEvent|接收订单事件|{}", flashOrderEvent);
        if (flashOrderEvent.getOrderId() == null) {
            log.info("orderEvent|订单事件参数错误");
            return Response.buildSuccess();
        }
        return Response.buildSuccess();
    }
}
