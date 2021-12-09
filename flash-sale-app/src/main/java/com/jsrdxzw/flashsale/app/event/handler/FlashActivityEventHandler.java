package com.jsrdxzw.flashsale.app.event.handler;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.event.EventHandler;
import com.alibaba.cola.event.EventHandlerI;
import com.jsrdxzw.flashsale.app.service.activity.cache.FlashActivitiesCacheService;
import com.jsrdxzw.flashsale.app.service.activity.cache.FlashActivityCacheService;
import com.jsrdxzw.flashsale.config.annotion.BetaTrace;
import com.jsrdxzw.flashsale.domain.event.FlashActivityEvent;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author xuzhiwei
 * @date 2021/12/3 11:03 AM
 */
@Slf4j
@EventHandler
public class FlashActivityEventHandler implements EventHandlerI<Response, FlashActivityEvent> {
    @Autowired
    private FlashActivityCacheService flashActivityCacheService;

    @Autowired
    private FlashActivitiesCacheService flashActivitiesCacheService;

    @BetaTrace
    @Override
    public Response execute(FlashActivityEvent flashActivityEvent) {
        log.info("activityEvent|接收活动事件|{}", JSONUtil.toJSONString(flashActivityEvent));
        if (flashActivityEvent.getId() == null) {
            log.info("activityEvent|事件参数错误|{}", flashActivityEvent);
            return Response.buildSuccess();
        }
        flashActivityCacheService.tryToUpdateActivityCacheByLock(flashActivityEvent.getId());
        flashActivitiesCacheService.tryToUpdateActivitiesCacheByLock(1);
        return Response.buildSuccess();
    }
}
