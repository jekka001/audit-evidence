package com.auditevidence.checks;

import com.auditevidence.github.GithubApiException;
import com.auditevidence.github.GithubClient;
import com.auditevidence.model.CheckResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditLogCheck implements Soc2Check {
    private static final String CLAUSE_ID = "CC7.3";
    private static final String CHECK_NAME = "Audit Log Availability";
    private static final String STANDARD = "SOC2";
    private static final String DESCRIPTION = "Confirm audit log access is enabled for the organization";

    @Override
    public String getClauseId() {
        return CLAUSE_ID;
    }

    @Override
    public String getCheckName() {
        return CHECK_NAME;
    }

    @Override
    public boolean requiresRepo() {
        return false;
    }

    @Override
    public CheckResult run(GithubClient client, String orgName, String repoName) throws GithubApiException {
        String dataSource = client.getApiEndpoint("/orgs/" + orgName + "/audit-log");

        if (!client.isOrganization(orgName)) {
            Map<String, Object> rawData = new HashMap<>();
            rawData.put("owner", orgName);
            rawData.put("note", "Audit log is only available for GitHub Organizations, not personal accounts");
            return CheckResult.partial(STANDARD, CLAUSE_ID, CHECK_NAME,
                    "Audit log check requires a GitHub Organization (not personal account)",
                    dataSource, rawData,
                    List.of("'" + orgName + "' is a personal account, not an organization",
                            "Audit logs are only available for GitHub Enterprise organizations"));
        }

        boolean auditLogEnabled = client.isAuditLogEnabled(orgName);

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("organization", orgName);
        rawData.put("auditLogAccessible", auditLogEnabled);
        rawData.put("note", "Audit log API access requires GitHub Enterprise Cloud");

        if (auditLogEnabled) {
            return CheckResult.pass(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData);
        }

        return CheckResult.fail(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData,
                List.of("Audit log is not accessible. This may indicate:",
                        "- Organization does not have GitHub Enterprise Cloud",
                        "- Token lacks admin:org scope",
                        "- Audit log streaming is not configured"));
    }
}
