package com.jsrdxzw.flashsale.app.mq;

import com.jsrdxzw.flashsale.app.model.PlaceOrderTask;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @author xuzhiwei
 * @date 2021/12/10 4:21 PM
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "queued")
public class RocketMQOrderTaskPostService implements OrderTaskPostService {
    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.placeorder.producer.group}")
    private String producerGroup;

    @Value("${rocketmq.placeorder.topic}")
    private String placeOrderTopic;

    private DefaultMQProducer placeOrderMQProducer;

    @PostConstruct
    public void init() {
        try {
            placeOrderMQProducer = new DefaultMQProducer(producerGroup);
            placeOrderMQProducer.setNamesrvAddr(nameServer);
            placeOrderMQProducer.start();
            log.info("initOrderTaskProducer|下单任务生产者初始化成功|{},{},{}", nameServer, producerGroup, placeOrderTopic);
        } catch (Exception e) {
            log.error("initOrderTaskProducer|下单任务生产者初始化失败|{},{},{}", nameServer, producerGroup, placeOrderTopic, e);
        }
    }

    @Override
    public boolean post(PlaceOrderTask placeOrderTask) {
        log.info("postOrderTask|投递下单任务|{}", JSONUtil.toJSONString(placeOrderTask));
        if (placeOrderTask == null) {
            log.info("postOrderTask|投递下单任务参数错误");
            return false;
        }
        String placeOrderTaskString = JSONUtil.toJSONString(placeOrderTask);
        Message message = new Message();
        message.setTopic(placeOrderTopic);
        message.setBody(placeOrderTaskString.getBytes());
        try {
            SendResult sendResult = placeOrderMQProducer.send(message);
            log.info("postOrderTask|下单任务投递完成|{},{}", placeOrderTask.getPlaceOrderTaskId(), JSONUtil.toJSONString(sendResult));
            if (SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
                log.info("postOrderTask|下单任务投递成功|{}", placeOrderTask.getPlaceOrderTaskId());
                return true;
            } else {
                log.info("postOrderTask|下单任务投递失败|{}", placeOrderTask.getPlaceOrderTaskId());
                return false;
            }
        } catch (Exception e) {
            log.error("postOrderTask|下单任务投递错误|{}", placeOrderTask.getPlaceOrderTaskId(), e);
            return false;
        }
    }
}
