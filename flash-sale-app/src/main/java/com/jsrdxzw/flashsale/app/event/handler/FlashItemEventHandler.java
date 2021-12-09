package com.jsrdxzw.flashsale.app.event.handler;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.event.EventHandler;
import com.alibaba.cola.event.EventHandlerI;
import com.alibaba.fastjson.JSON;
import com.jsrdxzw.flashsale.app.service.item.cache.FlashItemCacheService;
import com.jsrdxzw.flashsale.app.service.item.cache.FlashItemsCacheService;
import com.jsrdxzw.flashsale.domain.event.FlashItemEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author xuzhiwei
 * @date 2021/12/4 1:01 PM
 */
@Slf4j
@EventHandler
public class FlashItemEventHandler implements EventHandlerI<Response, FlashItemEvent> {

    @Autowired
    private FlashItemCacheService flashItemCacheService;

    @Autowired
    private FlashItemsCacheService flashItemsCacheService;

    @Override
    public Response execute(FlashItemEvent flashItemEvent) {
        log.info("itemEvent|接收秒杀品事件|{}", JSON.toJSON(flashItemEvent));
        if (flashItemEvent.getId() == null) {
            log.info("itemEvent|秒杀品事件参数错误");
            return Response.buildSuccess();
        }
        flashItemCacheService.tryToUpdateItemCacheByLock(flashItemEvent.getId());
        flashItemsCacheService.tryToUpdateItemsCacheByLock(flashItemEvent.getFlashActivityId());
        return Response.buildSuccess();
    }
}
