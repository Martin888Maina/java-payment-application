package com.martinmaina.payments.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.martinmaina.payments.entity.Payment;
import com.martinmaina.payments.entity.PaymentStatus;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReference(String reference);

    Optional<Payment> findByMerchantReference(String merchantReference);

    // Holds the row until the transaction ends so callbacks for one payment run one at a time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findForUpdateByReference(String reference);

    List<Payment> findAllByOrderByCreatedAtDescIdDesc();

    List<Payment> findAllByStatusOrderByCreatedAtDescIdDesc(PaymentStatus status);
}
