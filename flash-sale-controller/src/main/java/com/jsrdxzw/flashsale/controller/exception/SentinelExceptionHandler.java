package com.jsrdxzw.flashsale.controller.exception;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.fastjson.JSON;
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

import static com.jsrdxzw.flashsale.controller.constants.ExceptionCode.DEGRADE_BLOCK;
import static com.jsrdxzw.flashsale.controller.constants.ExceptionCode.LIMIT_BLOCK;

/**
 * @author xuzhiwei
 * @date 2021/12/9 4:51 PM
 */
@Slf4j
@ControllerAdvice
public class SentinelExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {UndeclaredThrowableException.class})
    protected ResponseEntity<Object> handleConflict(UndeclaredThrowableException ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        if (ex.getUndeclaredThrowable() instanceof FlowException) {
            exceptionResponse.setErrorCode(LIMIT_BLOCK.getCode());
            exceptionResponse.setErrorMessage(LIMIT_BLOCK.getDesc());
        }
        if (ex.getUndeclaredThrowable() instanceof DegradeException) {
            exceptionResponse.setErrorCode(DEGRADE_BLOCK.getCode());
            exceptionResponse.setErrorMessage(DEGRADE_BLOCK.getDesc());
        }
        log.info("expectedException|预期错误|{},{}", ex.getMessage(), ex);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return handleExceptionInternal(ex, JSONUtil.toJSONString(exceptionResponse), httpHeaders, HttpStatus.BAD_REQUEST, request);
    }
}
