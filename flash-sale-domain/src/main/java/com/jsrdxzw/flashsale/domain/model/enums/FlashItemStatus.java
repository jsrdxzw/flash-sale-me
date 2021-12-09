package com.jsrdxzw.flashsale.domain.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author xuzhiwei
 * @date 2021/12/3 5:24 PM
 */
@AllArgsConstructor
@Getter
public enum FlashItemStatus {
    PUBLISHED(0),
    ONLINE(1),
    OFFLINE(-1);

    private final Integer code;

    public static boolean isOffline(Integer status) {
        return OFFLINE.getCode().equals(status);
    }

    public static boolean isOnline(Integer status) {
        return ONLINE.getCode().equals(status);
    }
}
