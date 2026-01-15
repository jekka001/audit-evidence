package com.auditevidence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RepoCollaborator(
    long id,
    String login,
    @JsonProperty("role_name")
    String roleName,
    Permissions permissions
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Permissions(
        boolean admin,
        boolean maintain,
        boolean push,
        boolean triage,
        boolean pull
    ) {}

    public boolean hasWriteAccess() {
        return permissions != null && (permissions.admin || permissions.push || permissions.maintain);
    }

    public boolean hasAdminAccess() {
        return permissions != null && permissions.admin;
    }
}
