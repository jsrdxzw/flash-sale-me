package com.jsrdxzw.flashsale.controller.exception;

import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.jsrdxzw.flashsale.app.exception.BizException;
import com.jsrdxzw.flashsale.controller.AuthException;
import com.jsrdxzw.flashsale.domain.exception.DomainException;
import com.jsrdxzw.flashsale.domain.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.reflect.UndeclaredThrowableException;

import static com.jsrdxzw.flashsale.controller.constants.ExceptionCode.*;

/**
 * @author xuzhiwei
 * @date 2021/12/2 7:10 PM
 */
@Slf4j
@ControllerAdvice
public class BadRequestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {BizException.class, FlowException.class, AuthException.class, DomainException.class})
    protected ResponseEntity<Object> handleConflict(RuntimeException ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        // 动态代理或RPC调用的异常，这种情况也希望获得自定义的异常信息
        // 主要处理sentinel的异常
        if (ex instanceof UndeclaredThrowableException) {
            // 获取被包装的异常
            if (((UndeclaredThrowableException) ex).getUndeclaredThrowable() instanceof FlowException) {
                exceptionResponse.setErrorCode(LIMIT_BLOCK.getCode());
                exceptionResponse.setErrorMessage(LIMIT_BLOCK.getDesc());
            }
        } else if (ex instanceof BizException || ex instanceof DomainException) {
            exceptionResponse.setErrorCode(BIZ_ERROR.getCode());
            exceptionResponse.setErrorMessage(ex.getMessage());
        } else if (ex instanceof AuthException) {
            exceptionResponse.setErrorCode(AUTH_ERROR.getCode());
            exceptionResponse.setErrorMessage(AUTH_ERROR.getDesc());
        }
        log.error("expectedException|预期错误|{},{}", ex.getMessage(), ex);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return handleExceptionInternal(ex, JSONUtil.toJSONString(exceptionResponse), httpHeaders, HttpStatus.BAD_REQUEST, request);
    }
}
