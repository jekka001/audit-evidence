package com.auditevidence.github;

import com.auditevidence.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class GithubClient {
    private static final String API_BASE = "https://api.github.com";
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;

    public GithubClient(String token) {
        this.token = token;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public boolean isOrganization(String name) {
        try {
            getOrganization(name);
            return true;
        } catch (GithubApiException e) {
            return false;
        }
    }

    public GithubOrg getOrganization(String orgName) throws GithubApiException {
        String url = API_BASE + "/orgs/" + orgName;
        return executeRequest(url, new TypeReference<GithubOrg>() {});
    }

    public List<GithubRepo> getUserRepos(String username) throws GithubApiException {
        String url = API_BASE + "/users/" + username + "/repos";
        return executePaginatedRequest(url, new TypeReference<List<GithubRepo>>() {});
    }

    public List<GithubMember> getOrganizationMembers(String orgName) throws GithubApiException {
        String url = API_BASE + "/orgs/" + orgName + "/members";
        return executePaginatedRequest(url, new TypeReference<List<GithubMember>>() {});
    }

    public List<GithubMember> getMembersWithoutMfa(String orgName) throws GithubApiException {
        String url = API_BASE + "/orgs/" + orgName + "/members?filter=2fa_disabled";
        return executePaginatedRequest(url, new TypeReference<List<GithubMember>>() {});
    }

    public List<GithubRepo> getOrganizationRepos(String orgName) throws GithubApiException {
        String url = API_BASE + "/orgs/" + orgName + "/repos";
        return executePaginatedRequest(url, new TypeReference<List<GithubRepo>>() {});
    }

    public GithubRepo getRepository(String owner, String repo) throws GithubApiException {
        String url = API_BASE + "/repos/" + owner + "/" + repo;
        return executeRequest(url, new TypeReference<GithubRepo>() {});
    }

    public List<RepoCollaborator> getRepoCollaborators(String owner, String repo) throws GithubApiException {
        String url = API_BASE + "/repos/" + owner + "/" + repo + "/collaborators";
        return executePaginatedRequest(url, new TypeReference<List<RepoCollaborator>>() {});
    }

    public Optional<BranchProtection> getBranchProtection(String owner, String repo, String branch)
            throws GithubApiException {
        String url = API_BASE + "/repos/" + owner + "/" + repo + "/branches/" + branch + "/protection";
        try {
            return Optional.of(executeRequest(url, new TypeReference<BranchProtection>() {}));
        } catch (GithubApiException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public boolean isAuditLogEnabled(String orgName) throws GithubApiException {
        String url = API_BASE + "/orgs/" + orgName + "/audit-log?per_page=1";
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            throw new GithubApiException("Failed to check audit log: " + e.getMessage(), 0, e);
        }
    }

    public String getApiEndpoint(String path) {
        return API_BASE + path;
    }

    private <T> T executeRequest(String url, TypeReference<T> typeRef) throws GithubApiException {
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new GithubApiException(
                        "GitHub API error: " + response.code() + " " + body,
                        response.code(),
                        null
                );
            }
            String body = response.body() != null ? response.body().string() : "";
            return objectMapper.readValue(body, typeRef);
        } catch (IOException e) {
            throw new GithubApiException("Failed to execute request: " + e.getMessage(), 0, e);
        }
    }

    private <T> List<T> executePaginatedRequest(String url, TypeReference<List<T>> typeRef)
            throws GithubApiException {
        List<T> allResults = new ArrayList<>();
        String nextUrl = url + (url.contains("?") ? "&" : "?") + "per_page=100";

        while (nextUrl != null) {
            Request request = new Request.Builder()
                    .url(nextUrl)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : "";
                    throw new GithubApiException(
                            "GitHub API error: " + response.code() + " " + body,
                            response.code(),
                            null
                    );
                }
                String body = response.body() != null ? response.body().string() : "";
                List<T> pageResults = objectMapper.readValue(body, typeRef);
                allResults.addAll(pageResults);

                nextUrl = parseLinkHeader(response.header("Link"));
            } catch (IOException e) {
                throw new GithubApiException("Failed to execute request: " + e.getMessage(), 0, e);
            }
        }
        return allResults;
    }

    private String parseLinkHeader(String linkHeader) {
        if (linkHeader == null) return null;
        String[] links = linkHeader.split(",");
        for (String link : links) {
            String[] parts = link.split(";");
            if (parts.length == 2 && parts[1].contains("rel=\"next\"")) {
                String url = parts[0].trim();
                if (url.startsWith("<") && url.endsWith(">")) {
                    return url.substring(1, url.length() - 1);
                }
            }
        }
        return null;
    }
}
