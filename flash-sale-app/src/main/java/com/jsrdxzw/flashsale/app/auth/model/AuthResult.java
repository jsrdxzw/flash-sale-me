package com.jsrdxzw.flashsale.app.auth.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xuzhiwei
 * @date 2021/12/2 10:33 AM
 */
@Data
@Accessors(chain = true)
public class AuthResult {
    private Long userId;
    private boolean success;
    private String message;

    public AuthResult pass() {
        this.success = true;
        return this;
    }
}
