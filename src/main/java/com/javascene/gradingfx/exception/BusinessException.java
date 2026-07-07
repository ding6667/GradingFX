package com.javascene.gradingfx.exception;

import com.javascene.gradingfx.constant.ErrorCodeConstant;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(String message) {
        this(ErrorCodeConstant.BUSINESS_ERROR, message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        this(ErrorCodeConstant.BUSINESS_ERROR, message, cause);
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
