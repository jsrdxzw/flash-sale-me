package com.jsrdxzw.flashsale.controller.constants;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ExceptionCode {
    LIMIT_BLOCK("01", "操作频繁，请稍后再试"),
    INTERNAL_ERROR("02", "服务器开小差"),
    BIZ_ERROR("03", "客户端参数或操作错误"),
    AUTH_ERROR("04", "鉴权错误");
    private final String code;
    private final String desc;
}
