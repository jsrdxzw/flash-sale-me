package com.jsrdxzw.flashsale.domain.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author xuzhiwei
 * @date 2021/12/5 10:57 AM
 */
@AllArgsConstructor
@Getter
public enum FlashOrderStatus {
    CREATED(1),
    PAID(2),
    CANCELED(0),
    DELETED(-1);

    private final Integer code;

    public static boolean isCanceled(Integer status) {
        return CANCELED.getCode().equals(status);
    }

    public static boolean isDeleted(Integer status) {
        return DELETED.getCode().equals(status);
    }
}
