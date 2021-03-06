package com.jsrdxzw.flashsale.controller;

public class AuthException extends RuntimeException {
    public AuthException(String errMessage) {
        super(errMessage);
    }

    public AuthException(ErrorCode errorCode) {
        super(errorCode.getErrDesc());
    }
}
