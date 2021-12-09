package com.jsrdxzw.flashsale.util;

/**
 * @author xuzhiwei
 * @date 2021/12/3 2:47 PM
 */
public class StringHelper {
    public static String link(Object... items) {
        if (items == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            stringBuilder.append(items[i]);
            if (i < items.length - 1) {
                stringBuilder.append("_");
            }
        }
        return stringBuilder.toString();
    }
}
