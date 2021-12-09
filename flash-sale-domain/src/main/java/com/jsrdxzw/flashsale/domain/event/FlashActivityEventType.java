package com.jsrdxzw.flashsale.domain.event;

import lombok.AllArgsConstructor;

/**
 * @author xuzhiwei
 * @date 2021/12/2 3:36 PM
 */
@AllArgsConstructor
public enum FlashActivityEventType {
    PUBLISHED(0),
    ONLINE(1),
    OFFLINE(2),
    MODIFIED(3);

    private final Integer code;
}
