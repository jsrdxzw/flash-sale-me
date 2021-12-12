package com.jsrdxzw.flashsale;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author xuzhiwei
 * @date 2021/12/1 3:53 下午
 */
@SpringBootApplication(scanBasePackages = {"com.jsrdxzw.flashsale", "com.alibaba.cola"})
public class FlashSaleApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlashSaleApplication.class, args);
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry>  configurer(@Value("${spring.application.name}") String applicationName) {
        return (registry) -> registry.config().commonTags("application", applicationName);
    }
}
