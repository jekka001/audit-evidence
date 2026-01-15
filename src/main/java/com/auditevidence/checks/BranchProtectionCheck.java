package com.auditevidence.checks;

import com.auditevidence.github.GithubApiException;
import com.auditevidence.github.GithubClient;
import com.auditevidence.model.BranchProtection;
import com.auditevidence.model.CheckResult;
import com.auditevidence.model.GithubRepo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BranchProtectionCheck implements Soc2Check {
    private static final String CLAUSE_ID = "CC7.2";
    private static final String CHECK_NAME = "Branch Protection Rules";
    private static final String STANDARD = "SOC2";
    private static final String DESCRIPTION = "Verify protection exists for default branch";

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
        return true;
    }

    @Override
    public CheckResult run(GithubClient client, String orgName, String repoName) throws GithubApiException {
        GithubRepo repo = client.getRepository(orgName, repoName);
        String defaultBranch = repo.defaultBranch();
        String dataSource = client.getApiEndpoint("/repos/" + orgName + "/" + repoName +
                "/branches/" + defaultBranch + "/protection");

        Optional<BranchProtection> protection = client.getBranchProtection(orgName, repoName, defaultBranch);

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("repository", repo);
        rawData.put("defaultBranch", defaultBranch);
        rawData.put("branchProtection", protection.orElse(null));

        if (protection.isEmpty()) {
            return CheckResult.fail(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData,
                    List.of("No branch protection configured for default branch: " + defaultBranch));
        }

        BranchProtection bp = protection.get();
        rawData.put("enforceAdmins", bp.enforceAdmins() != null && bp.enforceAdmins().enabled());
        rawData.put("requireStatusChecks", bp.requiredStatusChecks() != null);
        rawData.put("allowForcePushes", bp.allowForcePushes() != null && bp.allowForcePushes().enabled());

        return CheckResult.pass(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData);
    }
}
