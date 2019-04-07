package com.springboot.exception;

public class ActivationCodeMismatchException extends RuntimeException {

    public ActivationCodeMismatchException() {
        super();
    }

    public ActivationCodeMismatchException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ActivationCodeMismatchException(final String message) {
        super(message);
    }

    public ActivationCodeMismatchException(final Throwable cause) {
        super(cause);
    }
}
