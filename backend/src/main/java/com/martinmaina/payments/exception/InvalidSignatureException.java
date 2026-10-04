package com.martinmaina.payments.exception;

public class InvalidSignatureException extends RuntimeException {

    public InvalidSignatureException() {
        super("Callback signature is missing or invalid");
    }
}
