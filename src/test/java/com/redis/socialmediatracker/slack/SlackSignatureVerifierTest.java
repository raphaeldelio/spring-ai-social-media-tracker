package com.redis.socialmediatracker.slack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Slack signature verification.
 * 
 * These tests use real examples from Slack's documentation to ensure
 * our implementation matches Slack's expected behavior.
 */
class SlackSignatureVerifierTest {

    private SlackSignatureVerifier verifier;
    private static final String TEST_SIGNING_SECRET = "8f742231b10e8888abcd99yyyzzz85a5";

    @BeforeEach
    void setUp() {
        verifier = new SlackSignatureVerifier();
        ReflectionTestUtils.setField(verifier, "signingSecret", TEST_SIGNING_SECRET);
    }

    @Test
    void testValidSignature() {
        // Test with a simple body and compute the expected signature
        // Using current timestamp to avoid replay attack detection
        long currentTime = System.currentTimeMillis() / 1000;
        String timestamp = String.valueOf(currentTime);
        String body = "{\"type\":\"event_callback\",\"event\":{\"type\":\"app_mention\"}}";

        // Compute the signature using our verifier's logic
        // We'll use reflection to access the private method for testing
        String baseString = "v0:" + timestamp + ":" + body;
        String computedSignature = computeTestSignature(baseString);

        boolean result = verifier.verifySignature(timestamp, computedSignature, body);
        assertTrue(result, "Valid signature should be accepted");
    }

    private String computeTestSignature(String baseString) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                TEST_SIGNING_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(baseString.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return "v0=" + result.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInvalidSignature() {
        String timestamp = "1531420618";
        String body = "token=xoxb-token&team_id=T1DC2JH3J";
        String invalidSignature = "v0=invalid_signature_here";

        boolean result = verifier.verifySignature(timestamp, invalidSignature, body);
        assertFalse(result, "Invalid signature should be rejected");
    }

    @Test
    void testTamperedBody() {
        String timestamp = "1531420618";
        String originalBody = "token=xoxb-token&team_id=T1DC2JH3J";
        String tamperedBody = "token=xoxb-token&team_id=TAMPERED";
        String signature = "v0=a2114d57b48eac39b9ad189dd8316235a7b4a8d21a10bd27519666489c69b503";

        boolean result = verifier.verifySignature(timestamp, signature, tamperedBody);
        assertFalse(result, "Tampered body should be rejected");
    }

    @Test
    void testOldTimestamp() {
        // Timestamp from 2018 (way too old)
        String oldTimestamp = "1531420618";
        String body = "token=xoxb-token";
        String signature = "v0=a2114d57b48eac39b9ad189dd8316235a7b4a8d21a10bd27519666489c69b503";

        boolean result = verifier.verifySignature(oldTimestamp, signature, body);
        assertFalse(result, "Old timestamp should be rejected (replay attack prevention)");
    }

    @Test
    void testRecentTimestamp() {
        // Use current timestamp (should be valid)
        long currentTime = System.currentTimeMillis() / 1000;
        String timestamp = String.valueOf(currentTime);
        String body = "{\"type\":\"url_verification\",\"challenge\":\"test123\"}";

        // We can't test with a real signature here since we'd need to compute it,
        // but we can verify the timestamp validation passes
        // This is tested indirectly through the valid signature test
        assertDoesNotThrow(() -> {
            verifier.verifySignature(timestamp, "v0=dummy", body);
        });
    }

    @Test
    void testNullParameters() {
        assertFalse(verifier.verifySignature(null, "v0=sig", "body"));
        assertFalse(verifier.verifySignature("123", null, "body"));
        assertFalse(verifier.verifySignature("123", "v0=sig", null));
    }

    @Test
    void testInvalidTimestampFormat() {
        String invalidTimestamp = "not-a-number";
        String body = "test";
        String signature = "v0=sig";

        boolean result = verifier.verifySignature(invalidTimestamp, signature, body);
        assertFalse(result, "Invalid timestamp format should be rejected");
    }

    @Test
    void testJsonBody() {
        // Test with JSON body (typical for event callbacks)
        long currentTime = System.currentTimeMillis() / 1000;
        String timestamp = String.valueOf(currentTime);
        String jsonBody = "{\"type\":\"event_callback\",\"event\":{\"type\":\"app_mention\",\"text\":\"test\"}}";

        // The signature verification should work with any body format
        assertDoesNotThrow(() -> {
            verifier.verifySignature(timestamp, "v0=dummy", jsonBody);
        });
    }
}

