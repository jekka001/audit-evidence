package com.auditevidence.checks;

import com.auditevidence.github.GithubApiException;
import com.auditevidence.github.GithubClient;
import com.auditevidence.model.CheckResult;

public interface Soc2Check {
    String getClauseId();
    String getCheckName();
    CheckResult run(GithubClient client, String orgName, String repoName) throws GithubApiException;
    boolean requiresRepo();
}
