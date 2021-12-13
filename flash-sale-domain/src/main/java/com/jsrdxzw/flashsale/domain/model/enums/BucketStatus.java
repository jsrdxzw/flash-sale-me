package com.jsrdxzw.flashsale.domain.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BucketStatus {
    ENABLED(1),
    DISABLED(0);

    private final Integer code;
}
