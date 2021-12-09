package com.jsrdxzw.flashsale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author xuzhiwei
 * @date 2021/12/1 3:53 下午
 */
@SpringBootApplication(scanBasePackages = {"com.jsrdxzw.flashsale", "com.alibaba.cola"})
public class FlashSaleApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlashSaleApplication.class, args);
    }
}
