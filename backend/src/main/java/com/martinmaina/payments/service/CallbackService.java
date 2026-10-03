package com.martinmaina.payments.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.martinmaina.payments.dto.CallbackRequest;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.exception.PaymentNotFoundException;
import com.martinmaina.payments.repository.PaymentRepository;

@Service
public class CallbackService {

    private final PaymentRepository paymentRepository;

    public CallbackService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponse apply(CallbackRequest callback) {
        Payment payment = paymentRepository.findForUpdateByReference(callback.reference())
                .orElseThrow(() -> new PaymentNotFoundException(callback.reference()));

        // Providers resend callbacks, so a repeat of the same result changes nothing
        if (payment.getStatus() == callback.result()) {
            return PaymentResponse.from(payment);
        }

        payment.complete(callback.result(), callback.providerReference());

        // Flushing runs the update callback so the response has the new updatedAt
        Payment saved = paymentRepository.saveAndFlush(payment);
        return PaymentResponse.from(saved);
    }
}
