package com.auditevidence.github;

public class GithubApiException extends Exception {
    private final int statusCode;

    public GithubApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
