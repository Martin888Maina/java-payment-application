package com.martinmaina.payments.exception;

public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(String merchantReference) {
        super("Merchant reference " + merchantReference + " is already used by a payment with different details");
    }
}
