package com.auditevidence.license;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class LicenseValidator {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String LICENSE_PREFIX = "AE-";

    public enum Tier {
        FREE, PAID
    }

    public record LicenseInfo(
        Tier tier,
        boolean isValid,
        String message,
        LocalDate expiresAt
    ) {
        public boolean canExportZip() {
            return tier == Tier.PAID && isValid;
        }

        public boolean showWatermark() {
            return tier == Tier.FREE || !isValid;
        }

        public int maxChecks() {
            return tier == Tier.PAID && isValid ? Integer.MAX_VALUE : 2;
        }
    }

    public LicenseInfo validate(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return new LicenseInfo(Tier.FREE, true, "No license key provided - using free tier", null);
        }

        if (!licenseKey.startsWith(LICENSE_PREFIX)) {
            return new LicenseInfo(Tier.FREE, false, "Invalid license key format", null);
        }

        try {
            String[] parts = licenseKey.substring(LICENSE_PREFIX.length()).split("-");
            if (parts.length != 3) {
                return new LicenseInfo(Tier.FREE, false, "Invalid license key format", null);
            }

            String orgHash = parts[0];
            String expiryEncoded = parts[1];
            String signature = parts[2];

            String expiryDate = new String(Base64.getDecoder().decode(expiryEncoded), StandardCharsets.UTF_8);
            LocalDate expiry = LocalDate.parse(expiryDate, DateTimeFormatter.ISO_LOCAL_DATE);

            if (expiry.isBefore(LocalDate.now())) {
                return new LicenseInfo(Tier.FREE, false, "License has expired on " + expiryDate, expiry);
            }

            if (verifySignature(orgHash, expiryEncoded, signature)) {
                return new LicenseInfo(Tier.PAID, true, "Valid paid license", expiry);
            }

            return new LicenseInfo(Tier.FREE, false, "Invalid license signature", null);
        } catch (Exception e) {
            return new LicenseInfo(Tier.FREE, false, "Failed to validate license: " + e.getMessage(), null);
        }
    }

    private boolean verifySignature(String orgHash, String expiryEncoded, String signature) {
        try {
            String data = orgHash + ":" + expiryEncoded;
            String expectedSignature = computeSignature(data);
            return constantTimeEquals(signature, expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    private String computeSignature(String data) throws Exception {
        String secret = System.getenv("AUDIT_EVIDENCE_LICENSE_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = "audit-evidence-default-key-change-in-production";
        }

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 16);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    public static String generateLicenseKey(String orgName, LocalDate expiryDate) throws Exception {
        String orgHash = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(orgName.getBytes(StandardCharsets.UTF_8)).substring(0, 8);
        String expiryEncoded = Base64.getEncoder()
                .encodeToString(expiryDate.format(DateTimeFormatter.ISO_LOCAL_DATE).getBytes(StandardCharsets.UTF_8));

        String data = orgHash + ":" + expiryEncoded;

        String secret = System.getenv("AUDIT_EVIDENCE_LICENSE_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = "audit-evidence-default-key-change-in-production";
        }

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 16);

        return LICENSE_PREFIX + orgHash + "-" + expiryEncoded + "-" + signature;
    }
}
