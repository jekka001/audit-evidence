package com.auditevidence.model;

import java.time.Instant;
import java.util.List;

public record CheckResult(
    String standard,
    String clauseId,
    String checkName,
    Status status,
    String description,
    String dataSource,
    Instant timestamp,
    Object rawData,
    List<String> findings
) {
    public enum Status {
        PASS, FAIL, PARTIAL
    }

    public static CheckResult pass(String standard, String clauseId, String checkName,
                                   String description, String dataSource, Object rawData) {
        return new CheckResult(standard, clauseId, checkName, Status.PASS, description,
                dataSource, Instant.now(), rawData, List.of());
    }

    public static CheckResult fail(String standard, String clauseId, String checkName,
                                   String description, String dataSource, Object rawData,
                                   List<String> findings) {
        return new CheckResult(standard, clauseId, checkName, Status.FAIL, description,
                dataSource, Instant.now(), rawData, findings);
    }

    public static CheckResult partial(String standard, String clauseId, String checkName,
                                      String description, String dataSource, Object rawData,
                                      List<String> findings) {
        return new CheckResult(standard, clauseId, checkName, Status.PARTIAL, description,
                dataSource, Instant.now(), rawData, findings);
    }
}
