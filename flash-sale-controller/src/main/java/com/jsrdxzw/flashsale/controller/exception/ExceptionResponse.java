package com.jsrdxzw.flashsale.controller.exception;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xuzhiwei
 * @date 2021/12/2 7:22 PM
 */
@Data
@Accessors(chain = true)
public class ExceptionResponse {
    private String errorCode;
    private String errorMessage;
}
