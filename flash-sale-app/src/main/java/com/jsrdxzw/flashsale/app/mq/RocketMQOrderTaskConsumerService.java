package com.jsrdxzw.flashsale.app.mq;

import com.jsrdxzw.flashsale.app.model.PlaceOrderTask;
import com.jsrdxzw.flashsale.app.service.placeorder.queued.QueuedPlaceOrderService;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author xuzhiwei
 * @date 2021/12/10 4:34 PM
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "PLACE_ORDER_TASK_TOPIC", consumerGroup = "PLACE_ORDER_TASK_TOPIC_CONSUMER_GROUP")
@ConditionalOnProperty(name = "place_order_type", havingValue = "queued")
public class RocketMQOrderTaskConsumerService implements RocketMQListener<String> {

    @Autowired
    private QueuedPlaceOrderService queuedPlaceOrderService;

    @Override
    public void onMessage(String message) {
        log.info("handleOrderTask|接收下单任务消息|{}", message);
        if (StringUtils.isEmpty(message)) {
            log.info("handleOrderTask|接收下单任务消息为空|{}", message);
            return;
        }
        try {
            PlaceOrderTask placeOrderTask = JSONUtil.parseObject(message, PlaceOrderTask.class);
            if (Objects.isNull(placeOrderTask)) {
                log.error("receive message is null");
                return;
            }
            queuedPlaceOrderService.handlePlaceOrderTask(placeOrderTask);
            log.info("handleOrderTask|下单任务消息处理完成|{}", placeOrderTask.getPlaceOrderTaskId());
        } catch (Exception e) {
            log.error("handleOrderTask|下单任务消息处理失败|{}", message);
        }
    }
}
