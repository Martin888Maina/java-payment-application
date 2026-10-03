package com.martinmaina.payments.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.martinmaina.payments.dto.CallbackRequest;
import com.martinmaina.payments.dto.PaymentResponse;
import com.martinmaina.payments.service.CallbackService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments/callback")
public class CallbackController {

    private final CallbackService callbackService;

    public CallbackController(CallbackService callbackService) {
        this.callbackService = callbackService;
    }

    @PostMapping
    public PaymentResponse receive(@Valid @RequestBody CallbackRequest callback) {
        return callbackService.apply(callback);
    }
}
