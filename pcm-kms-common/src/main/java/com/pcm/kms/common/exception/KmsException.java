package com.pcm.kms.common.exception;

/**
 * KMS 业务异常
 */
public class KmsException extends RuntimeException {

    private final int code;

    public KmsException(String message) {
        super(message);
        this.code = 400;
    }

    public KmsException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
