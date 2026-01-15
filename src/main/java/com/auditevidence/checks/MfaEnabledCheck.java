package com.auditevidence.checks;

import com.auditevidence.github.GithubApiException;
import com.auditevidence.github.GithubClient;
import com.auditevidence.model.CheckResult;
import com.auditevidence.model.GithubMember;
import com.auditevidence.model.GithubOrg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MfaEnabledCheck implements Soc2Check {
    private static final String CLAUSE_ID = "CC6.1";
    private static final String CHECK_NAME = "MFA Enforcement";
    private static final String STANDARD = "SOC2";
    private static final String DESCRIPTION = "Verify that all organization members have MFA enabled";

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
        String dataSource = client.getApiEndpoint("/orgs/" + orgName + "/members?filter=2fa_disabled");

        if (!client.isOrganization(orgName)) {
            Map<String, Object> rawData = new HashMap<>();
            rawData.put("owner", orgName);
            rawData.put("note", "MFA check is only available for GitHub Organizations, not personal accounts");
            return CheckResult.partial(STANDARD, CLAUSE_ID, CHECK_NAME,
                    "MFA check requires a GitHub Organization (not personal account)",
                    dataSource, rawData,
                    List.of("'" + orgName + "' is a personal account, not an organization",
                            "MFA enforcement can only be verified for organizations"));
        }

        GithubOrg org = client.getOrganization(orgName);
        List<GithubMember> membersWithoutMfa = client.getMembersWithoutMfa(orgName);
        List<GithubMember> allMembers = client.getOrganizationMembers(orgName);

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("organization", org);
        rawData.put("totalMembers", allMembers.size());
        rawData.put("membersWithoutMfa", membersWithoutMfa);
        rawData.put("twoFactorRequirementEnabled", org.twoFactorRequirementEnabled());

        if (membersWithoutMfa.isEmpty()) {
            return CheckResult.pass(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData);
        }

        List<String> findings = membersWithoutMfa.stream()
                .map(m -> "User without MFA: " + m.login())
                .toList();

        if (membersWithoutMfa.size() == allMembers.size()) {
            return CheckResult.fail(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData, findings);
        }

        return CheckResult.partial(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData, findings);
    }
}
