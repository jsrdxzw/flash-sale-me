package com.jsrdxzw.flashsale.util;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;

/**
 * @author xuzhiwei
 * @date 2021/12/2 11:59 AM
 */
public class Base64Util {
    /**
     * Base64编码
     *
     * @param data 待编码数据
     * @return String 编码数据
     * @throws Exception
     */
    public static String encode(String data) throws Exception {
        // 执行编码
        byte[] b = Base64.encodeBase64(data.getBytes(StandardCharsets.UTF_8));
        return new String(b, StandardCharsets.UTF_8);
    }

    /**
     * Base64安全编码<br>
     * 遵循RFC 2045实现
     *
     * @param data 待编码数据
     * @return String 编码数据
     * @throws Exception
     */
    public static String encodeSafe(String data) throws Exception {

        // 执行编码
        byte[] b = Base64.encodeBase64(data.getBytes(StandardCharsets.UTF_8), true);

        return new String(b, StandardCharsets.UTF_8);
    }

    /**
     * Base64解码
     *
     * @param data 待解码数据
     * @return String 解码数据
     * @throws Exception
     */
    public static String decode(String data) throws Exception {

        // 执行解码
        byte[] b = Base64.decodeBase64(data.getBytes(StandardCharsets.UTF_8));

        return new String(b, StandardCharsets.UTF_8);
    }
}
