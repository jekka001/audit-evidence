package com.auditevidence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubRepo(
    long id,
    String name,
    @JsonProperty("full_name")
    String fullName,
    @JsonProperty("private")
    boolean isPrivate,
    @JsonProperty("default_branch")
    String defaultBranch,
    String visibility
) {}
