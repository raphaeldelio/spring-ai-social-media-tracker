package com.redis.socialmediatracker.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Verifies Slack request signatures to ensure requests are authentic.
 * 
 * Slack signs all requests with HMAC-SHA256 using your signing secret.
 * This prevents unauthorized parties from sending fake events to your endpoint.
 * 
 * @see <a href="https://api.slack.com/authentication/verifying-requests-from-slack">Slack Documentation</a>
 */
@Component
public class SlackSignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(SlackSignatureVerifier.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String VERSION = "v0";
    private static final long MAX_REQUEST_AGE_SECONDS = 300; // 5 minutes

    @Value("${slack.signing.secret}")
    private String signingSecret;

    /**
     * Verify that a request came from Slack.
     * 
     * @param timestamp The X-Slack-Request-Timestamp header value
     * @param signature The X-Slack-Signature header value
     * @param body The raw request body as a string
     * @return true if the signature is valid, false otherwise
     */
    public boolean verifySignature(String timestamp, String signature, String body) {
        if (timestamp == null || signature == null || body == null) {
            logger.warn("Missing required parameters for signature verification");
            return false;
        }

        // Step 1: Verify timestamp to prevent replay attacks
        if (!isTimestampValid(timestamp)) {
            logger.warn("Request timestamp is too old or invalid: {}", timestamp);
            return false;
        }

        // Step 2: Compute the expected signature
        String expectedSignature = computeSignature(timestamp, body);
        if (expectedSignature == null) {
            logger.error("Failed to compute signature");
            return false;
        }

        // Step 3: Compare signatures using constant-time comparison
        boolean isValid = MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8)
        );

        if (!isValid) {
            logger.warn("Invalid Slack signature. Expected: {}, Got: {}", expectedSignature, signature);
        } else {
            logger.debug("Slack signature verified successfully");
        }

        return isValid;
    }

    /**
     * Verify that the request timestamp is recent (within 5 minutes).
     * This prevents replay attacks.
     */
    private boolean isTimestampValid(String timestamp) {
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            long age = Math.abs(currentTime - requestTime);
            
            if (age > MAX_REQUEST_AGE_SECONDS) {
                logger.warn("Request is too old: {} seconds", age);
                return false;
            }
            
            return true;
        } catch (NumberFormatException e) {
            logger.error("Invalid timestamp format: {}", timestamp);
            return false;
        }
    }

    /**
     * Compute the expected signature for a request.
     * 
     * The signature is computed as:
     * HMAC-SHA256(signing_secret, "v0:{timestamp}:{body}")
     * 
     * @param timestamp The request timestamp
     * @param body The raw request body
     * @return The expected signature in the format "v0={hash}", or null if computation fails
     */
    private String computeSignature(String timestamp, String body) {
        try {
            // Build the base string: v0:timestamp:body
            String baseString = VERSION + ":" + timestamp + ":" + body;

            // Create HMAC-SHA256 instance with signing secret
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                signingSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
            );
            mac.init(secretKeySpec);

            // Compute the hash
            byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            String hexHash = bytesToHex(hash);

            // Return in Slack's format: v0={hash}
            return VERSION + "=" + hexHash;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error computing signature: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convert byte array to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}

