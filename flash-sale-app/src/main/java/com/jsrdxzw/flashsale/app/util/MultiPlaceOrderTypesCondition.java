package com.jsrdxzw.flashsale.app.util;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * @author xuzhiwei
 * @date 2021/12/4 2:28 PM
 */
public class MultiPlaceOrderTypesCondition extends AnyNestedCondition {
    public MultiPlaceOrderTypesCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(name = "place_order_type", havingValue = "normal", matchIfMissing = true)
    static class NormalCondition {
    }

    @ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
    static class BucketsCondition {
    }
}
