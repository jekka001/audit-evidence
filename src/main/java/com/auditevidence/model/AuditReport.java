package com.auditevidence.model;

import java.time.Instant;
import java.util.List;

public record AuditReport(
    String organizationName,
    String standard,
    Instant generatedAt,
    List<CheckResult> results,
    LicenseInfo licenseInfo
) {
    public record LicenseInfo(
        boolean isPaid,
        String tier
    ) {}

    public long passCount() {
        return results.stream().filter(r -> r.status() == CheckResult.Status.PASS).count();
    }

    public long failCount() {
        return results.stream().filter(r -> r.status() == CheckResult.Status.FAIL).count();
    }

    public long partialCount() {
        return results.stream().filter(r -> r.status() == CheckResult.Status.PARTIAL).count();
    }
}
