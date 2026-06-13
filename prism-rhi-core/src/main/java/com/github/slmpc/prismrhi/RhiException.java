package com.github.slmpc.prismrhi;

public class RhiException extends RuntimeException {
    public RhiException(String message) {
        super(message);
    }

    public RhiException(String message, Throwable cause) {
        super(message, cause);
    }
}
