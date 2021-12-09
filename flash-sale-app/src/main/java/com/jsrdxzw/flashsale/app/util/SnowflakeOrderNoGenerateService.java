package com.jsrdxzw.flashsale.app.util;

import com.jsrdxzw.flashsale.util.SnowflakeIdWorker;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Random;

/**
 * @author xuzhiwei
 * @date 2021/12/5 6:46 PM
 */
@Service
public class SnowflakeOrderNoGenerateService implements OrderNoGenerateService {
    private SnowflakeIdWorker snowflakeIdWorker;

    /**
     * 分布式部署时需要动态获取机器的ID，此处为方便演示使用随机数作为机器ID
     */
    @PostConstruct
    public void initWorker() {
        Random random = new Random(1);
        snowflakeIdWorker = new SnowflakeIdWorker(random.nextInt(32), 1, 1);
    }

    @Override
    public Long generateOrderNo(OrderNoGenerateContext orderNoGenerateContext) {
        return snowflakeIdWorker.nextId();
    }
}
