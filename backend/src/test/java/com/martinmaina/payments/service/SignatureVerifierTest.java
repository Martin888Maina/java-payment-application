package com.martinmaina.payments.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.martinmaina.payments.config.CallbackProperties;

class SignatureVerifierTest {

    private static final String BODY =
            "{\"reference\":\"PAY-7F3K9Q2M8XWD\",\"status\":\"SUCCESSFUL\",\"providerReference\":\"QJK3H2L9P0\"}";

    // Expected value produced with: openssl dgst -sha256 -hmac test-callback-secret
    private static final String EXPECTED_SIGNATURE =
            "798544bfb785a8892594ecdada32ec0f7082a33a541835c0743f23996efb8ca2";

    private final SignatureVerifier verifier = new SignatureVerifier(new CallbackProperties("test-callback-secret"));

    @Test
    void signsBodyWithHmacSha256() {
        assertThat(verifier.sign(BODY)).isEqualTo(EXPECTED_SIGNATURE);
    }

    @Test
    void acceptsValidSignature() {
        assertThat(verifier.isValid(BODY, EXPECTED_SIGNATURE)).isTrue();
    }

    @Test
    void acceptsUppercaseSignature() {
        assertThat(verifier.isValid(BODY, EXPECTED_SIGNATURE.toUpperCase())).isTrue();
    }

    @Test
    void rejectsTamperedBody() {
        String tampered = BODY.replace("SUCCESSFUL", "FAILED");

        assertThat(verifier.isValid(tampered, EXPECTED_SIGNATURE)).isFalse();
    }

    @Test
    void rejectsSignatureMadeWithAnotherSecret() {
        SignatureVerifier other = new SignatureVerifier(new CallbackProperties("another-secret"));

        assertThat(verifier.isValid(BODY, other.sign(BODY))).isFalse();
    }

    @Test
    void rejectsSignatureThatIsNotHex() {
        assertThat(verifier.isValid(BODY, "not-a-signature")).isFalse();
    }

    @Test
    void rejectsMissingSignature() {
        assertThat(verifier.isValid(BODY, null)).isFalse();
        assertThat(verifier.isValid(BODY, "")).isFalse();
    }
}
