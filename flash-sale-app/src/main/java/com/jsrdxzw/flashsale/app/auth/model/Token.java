package com.jsrdxzw.flashsale.app.auth.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xuzhiwei
 * @date 2021/12/2 12:13 PM
 */
@Data
@Accessors(chain = true)
public class Token {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 令牌失效时间
     */
    private String expireDate;
}
