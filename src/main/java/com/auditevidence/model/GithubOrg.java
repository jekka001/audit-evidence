package com.auditevidence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubOrg(
    long id,
    String login,
    String name,
    String description,
    @JsonProperty("two_factor_requirement_enabled")
    Boolean twoFactorRequirementEnabled
) {}
