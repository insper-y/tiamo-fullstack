package com.tiamo.common;

import lombok.Getter;

/**
 * 自定义业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = Code.BUSINESS_ERR;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
