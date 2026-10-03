package com.martinmaina.payments.service;

import java.security.SecureRandom;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.martinmaina.payments.dto.CreatePaymentRequest;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final String DEFAULT_CURRENCY = "KES";
    private static final String REFERENCE_PREFIX = "PAY-";
    private static final int REFERENCE_LENGTH = 12;

    // Leaves out characters that look alike, such as O and 0
    private static final String REFERENCE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final PaymentRepository paymentRepository;
    private final SecureRandom random = new SecureRandom();

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public CreateResult create(CreatePaymentRequest request) {
        String currency = request.currency() != null ? request.currency() : DEFAULT_CURRENCY;

        // A repeated request returns the first payment instead of creating a second one
        Optional<Payment> existing = paymentRepository.findByMerchantReference(request.merchantReference());
        if (existing.isPresent() && hasSameDetails(existing.get(), request, currency)) {
            return new CreateResult(PaymentResponse.from(existing.get()), false);
        }

        Payment payment = new Payment(
                newReference(),
                request.merchantReference(),
                request.amount(),
                currency,
                request.payerPhone());

        Payment saved = paymentRepository.save(payment);
        return new CreateResult(PaymentResponse.from(saved), true);
    }

    private boolean hasSameDetails(Payment payment, CreatePaymentRequest request, String currency) {
        return payment.getAmount().compareTo(request.amount()) == 0
                && payment.getCurrency().equals(currency)
                && payment.getPayerPhone().equals(request.payerPhone());
    }

    private String newReference() {
        StringBuilder reference = new StringBuilder(REFERENCE_PREFIX);
        for (int i = 0; i < REFERENCE_LENGTH; i++) {
            reference.append(REFERENCE_CHARACTERS.charAt(random.nextInt(REFERENCE_CHARACTERS.length())));
        }
        return reference.toString();
    }

    public record CreateResult(PaymentResponse payment, boolean created) {
    }
}
