package com.auditevidence.checks;

import com.auditevidence.github.GithubApiException;
import com.auditevidence.github.GithubClient;
import com.auditevidence.model.BranchProtection;
import com.auditevidence.model.CheckResult;
import com.auditevidence.model.GithubRepo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PrReviewsRequiredCheck implements Soc2Check {
    private static final String CLAUSE_ID = "CC7.2";
    private static final String CHECK_NAME = "Pull Request Reviews Required";
    private static final String STANDARD = "SOC2";
    private static final String DESCRIPTION = "Check if PR approvals are required before merging";

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
                    List.of("No branch protection configured - PR reviews cannot be enforced"));
        }

        BranchProtection bp = protection.get();
        BranchProtection.RequiredPullRequestReviews prReviews = bp.requiredPullRequestReviews();

        if (prReviews == null) {
            return CheckResult.fail(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData,
                    List.of("PR reviews are not required for merging"));
        }

        rawData.put("requiredApprovingReviewCount", prReviews.requiredApprovingReviewCount());
        rawData.put("dismissStaleReviews", prReviews.dismissStaleReviews());
        rawData.put("requireCodeOwnerReviews", prReviews.requireCodeOwnerReviews());

        List<String> findings = new ArrayList<>();
        if (prReviews.requiredApprovingReviewCount() < 1) {
            findings.add("Required approving review count is 0");
        }
        if (!prReviews.dismissStaleReviews()) {
            findings.add("Stale reviews are not dismissed on new commits");
        }

        if (prReviews.requiredApprovingReviewCount() >= 1) {
            if (findings.isEmpty()) {
                return CheckResult.pass(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData);
            }
            return CheckResult.partial(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData, findings);
        }

        return CheckResult.fail(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData, findings);
    }
}
