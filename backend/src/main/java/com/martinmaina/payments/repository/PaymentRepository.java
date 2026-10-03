package com.martinmaina.payments.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReference(String reference);

    Optional<Payment> findByMerchantReference(String merchantReference);

    List<Payment> findAllByOrderByCreatedAtDescIdDesc();

    List<Payment> findAllByStatusOrderByCreatedAtDescIdDesc(PaymentStatus status);
}
