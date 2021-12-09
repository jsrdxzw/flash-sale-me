package com.jsrdxzw.flashsale.controller;


import com.alibaba.cola.dto.ErrorCodeI;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode implements ErrorCodeI {

    INVALID_TOKEN("INVALID_TOKEN", "无效token"),
    UNAUTHORIZED_ACCESS("UNAUTHORIZED_ACCESS", "访问未授权");

    private final String errCode;
    private final String errDesc;
}
