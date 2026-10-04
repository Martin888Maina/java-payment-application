package com.martinmaina.payments.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import com.martinmaina.payments.dto.CallbackRequest;
import com.martinmaina.payments.exception.InvalidSignatureException;
import com.martinmaina.payments.service.SignatureVerifier;

// Checks the signature against the exact bytes received, before the JSON is read
@ControllerAdvice
public class CallbackSignatureAdvice extends RequestBodyAdviceAdapter {

    public static final String SIGNATURE_HEADER = "X-Signature";

    private final SignatureVerifier signatureVerifier;

    public CallbackSignatureAdvice(SignatureVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }

    @Override
    public boolean supports(MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return parameter.getParameterType() == CallbackRequest.class;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        byte[] body = inputMessage.getBody().readAllBytes();
        String signature = inputMessage.getHeaders().getFirst(SIGNATURE_HEADER);

        if (!signatureVerifier.isValid(new String(body, StandardCharsets.UTF_8), signature)) {
            throw new InvalidSignatureException();
        }

        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(body);
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }
}
