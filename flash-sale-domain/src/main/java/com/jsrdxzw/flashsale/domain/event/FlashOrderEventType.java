package com.jsrdxzw.flashsale.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author xuzhiwei
 * @date 2021/12/5 10:58 AM
 */
@AllArgsConstructor
@Getter
public enum FlashOrderEventType {
    CREATED(0),
    CANCEL(1);

    private final Integer code;
}
