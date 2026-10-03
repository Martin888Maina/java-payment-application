package com.martinmaina.payments.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.martinmaina.payments.dto.CallbackRequest;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.exception.PaymentAlreadyFinalisedException;
import com.martinmaina.payments.repository.PaymentRepository;

@SpringBootTest
class CallbackConcurrencyTest {

    private static final String REFERENCE = "PAY-CONCURRENT01";

    @Autowired
    private CallbackService callbackService;

    @Autowired
    private PaymentRepository paymentRepository;

    @AfterEach
    void clearPayments() {
        paymentRepository.deleteAll();
    }

    @RepeatedTest(10)
    void conflictingCallbacksAtTheSameTimeHaveOneWinner() throws Exception {
        paymentRepository.save(new Payment(REFERENCE, "INV-5001", new BigDecimal("1500.00"), "KES", "254712345678"));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<PaymentResponse> success = executor.submit(() -> {
                start.await();
                return callbackService.apply(new CallbackRequest(REFERENCE, "SUCCESSFUL", "QJK3H2L9P0"));
            });
            Future<PaymentResponse> failure = executor.submit(() -> {
                start.await();
                return callbackService.apply(new CallbackRequest(REFERENCE, "FAILED", "QJK3H2L9P1"));
            });
            start.countDown();

            List<Object> outcomes = List.of(outcome(success), outcome(failure));

            assertThat(outcomes).filteredOn(PaymentResponse.class::isInstance).hasSize(1);
            assertThat(outcomes).filteredOn(PaymentAlreadyFinalisedException.class::isInstance).hasSize(1);

            PaymentResponse winner = (PaymentResponse) outcomes.stream()
                    .filter(PaymentResponse.class::isInstance)
                    .findFirst()
                    .orElseThrow();
            Payment stored = paymentRepository.findByReference(REFERENCE).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(winner.status());
            assertThat(stored.getProviderReference()).isEqualTo(winner.providerReference());
        } finally {
            executor.shutdownNow();
        }
    }

    private Object outcome(Future<PaymentResponse> future) throws InterruptedException {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            return e.getCause();
        } catch (TimeoutException e) {
            return e;
        }
    }
}
