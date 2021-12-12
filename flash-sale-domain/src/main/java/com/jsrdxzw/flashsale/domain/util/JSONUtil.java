package com.jsrdxzw.flashsale.domain.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xuzhiwei
 * @date 2021/12/2 1:18 PM
 */
@Slf4j
public class JSONUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    }

    public static <T> String toJSONString(T object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("JSONUtil|json序列化异常|{},{}", e.getMessage(), e);
        }
        return "";
    }

    public static <T> T parseObject(String json, Class<T> tClass) {
        try {
            return mapper.readValue(json, tClass);
        } catch (JsonProcessingException e) {
            log.error("JSONUtil|json反序列化异常|{},{}", e.getMessage(), e);
        }
        return null;
    }
}
