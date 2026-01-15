package com.auditevidence.checks;

import com.auditevidence.github.GithubApiException;
import com.auditevidence.github.GithubClient;
import com.auditevidence.model.CheckResult;
import com.auditevidence.model.GithubRepo;
import com.auditevidence.model.RepoCollaborator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepoAccessControlCheck implements Soc2Check {
    private static final String CLAUSE_ID = "CC6.2";
    private static final String CHECK_NAME = "Repository Access Control";
    private static final String STANDARD = "SOC2";
    private static final String DESCRIPTION = "List users with admin/write access to repository";

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
        String dataSource = client.getApiEndpoint("/repos/" + orgName + "/" + repoName + "/collaborators");

        GithubRepo repo = client.getRepository(orgName, repoName);
        List<RepoCollaborator> collaborators = client.getRepoCollaborators(orgName, repoName);

        List<RepoCollaborator> admins = collaborators.stream()
                .filter(RepoCollaborator::hasAdminAccess)
                .toList();

        List<RepoCollaborator> writers = collaborators.stream()
                .filter(c -> c.hasWriteAccess() && !c.hasAdminAccess())
                .toList();

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("repository", repo);
        rawData.put("totalCollaborators", collaborators.size());
        rawData.put("admins", admins);
        rawData.put("writers", writers);
        rawData.put("allCollaborators", collaborators);

        List<String> findings = new ArrayList<>();
        for (RepoCollaborator admin : admins) {
            findings.add("Admin access: " + admin.login());
        }
        for (RepoCollaborator writer : writers) {
            findings.add("Write access: " + writer.login());
        }

        return CheckResult.pass(STANDARD, CLAUSE_ID, CHECK_NAME, DESCRIPTION, dataSource, rawData);
    }
}
