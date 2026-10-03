package com.martinmaina.payments.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String reference) {
        super("Payment " + reference + " was not found");
    }
}
