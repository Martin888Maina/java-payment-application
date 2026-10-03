package com.martinmaina.payments.exception;

import com.martinmaina.payments.entity.PaymentStatus;

public class PaymentAlreadyFinalisedException extends RuntimeException {

    public PaymentAlreadyFinalisedException(String reference, PaymentStatus status) {
        super("Payment " + reference + " is already " + status + " and cannot change");
    }
}
