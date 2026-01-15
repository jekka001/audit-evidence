package com.auditevidence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BranchProtection(
    String url,
    @JsonProperty("required_status_checks")
    RequiredStatusChecks requiredStatusChecks,
    @JsonProperty("enforce_admins")
    EnforceAdmins enforceAdmins,
    @JsonProperty("required_pull_request_reviews")
    RequiredPullRequestReviews requiredPullRequestReviews,
    @JsonProperty("required_signatures")
    RequiredSignatures requiredSignatures,
    @JsonProperty("allow_force_pushes")
    AllowForcePushes allowForcePushes,
    @JsonProperty("allow_deletions")
    AllowDeletions allowDeletions
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RequiredStatusChecks(
        boolean strict,
        String[] contexts
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnforceAdmins(
        String url,
        boolean enabled
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RequiredPullRequestReviews(
        String url,
        @JsonProperty("dismiss_stale_reviews")
        boolean dismissStaleReviews,
        @JsonProperty("require_code_owner_reviews")
        boolean requireCodeOwnerReviews,
        @JsonProperty("required_approving_review_count")
        int requiredApprovingReviewCount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RequiredSignatures(
        String url,
        boolean enabled
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AllowForcePushes(
        boolean enabled
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AllowDeletions(
        boolean enabled
    ) {}

    public boolean hasPrReviewsRequired() {
        return requiredPullRequestReviews != null &&
               requiredPullRequestReviews.requiredApprovingReviewCount > 0;
    }
}
