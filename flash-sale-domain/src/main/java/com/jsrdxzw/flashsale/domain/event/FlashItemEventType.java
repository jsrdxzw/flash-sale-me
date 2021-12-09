package com.jsrdxzw.flashsale.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author xuzhiwei
 * @date 2021/12/3 5:41 PM
 */

@AllArgsConstructor
@Getter
public enum FlashItemEventType {
    PUBLISHED(0),
    ONLINE(1),
    OFFLINE(2);

    private final Integer code;
}
