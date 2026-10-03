package com.martinmaina.payments.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.martinmaina.payments.config.CallbackProperties;

@Component
public class SignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public SignatureVerifier(CallbackProperties properties) {
        this.key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String sign(String body) {
        return HexFormat.of().formatHex(hmac(body));
    }

    public boolean isValid(String body, String signature) {
        if (signature == null) {
            return false;
        }
        byte[] given;
        try {
            given = HexFormat.of().parseHex(signature.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        // Constant time comparison so the check does not leak timing information
        return MessageDigest.isEqual(hmac(body), given);
    }

    private byte[] hmac(String body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
