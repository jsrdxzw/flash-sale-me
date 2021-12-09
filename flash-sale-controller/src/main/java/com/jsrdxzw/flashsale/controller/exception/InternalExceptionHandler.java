package com.jsrdxzw.flashsale.controller.exception;

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

import static com.jsrdxzw.flashsale.controller.constants.ExceptionCode.INTERNAL_ERROR;

/**
 * @author xuzhiwei
 * @date 2021/12/2 7:33 PM
 */
@Slf4j
@ControllerAdvice
public class InternalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = {Exception.class, RuntimeException.class})
    protected ResponseEntity<Object> handleConflict(Exception ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setErrorCode(INTERNAL_ERROR.getCode());
        exceptionResponse.setErrorMessage(INTERNAL_ERROR.getDesc());
        log.error("unknownError|未知错误|{},{}", ex.getMessage(), ex);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return handleExceptionInternal(ex, JSONUtil.toJSONString(exceptionResponse), httpHeaders, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
