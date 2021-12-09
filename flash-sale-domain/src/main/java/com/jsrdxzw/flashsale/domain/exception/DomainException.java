package com.jsrdxzw.flashsale.domain.exception;

public class DomainException extends RuntimeException {
    public DomainException(DomainErrorCode domainErrorCode) {
        super(domainErrorCode.getErrDesc());
    }
}
