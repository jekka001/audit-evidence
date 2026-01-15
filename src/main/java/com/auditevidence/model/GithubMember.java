package com.auditevidence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubMember(
    long id,
    String login,
    @JsonProperty("avatar_url")
    String avatarUrl,
    String type,
    @JsonProperty("site_admin")
    boolean siteAdmin
) {}
