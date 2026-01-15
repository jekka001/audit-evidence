package com.auditevidence.cli;

import com.auditevidence.checks.*;
import com.auditevidence.exporter.json.JsonExporter;
import com.auditevidence.exporter.pdf.PdfExporter;
import com.auditevidence.exporter.zip.ZipExporter;
import com.auditevidence.github.GithubApiException;
import com.auditevidence.github.GithubClient;
import com.auditevidence.license.LicenseValidator;
import com.auditevidence.model.AuditReport;
import com.auditevidence.model.CheckResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "audit-evidence",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "GitHub SOC2 Audit Evidence Exporter - Generate auditor-ready evidence PDFs"
)
public class AuditEvidenceCli implements Callable<Integer> {

    @Option(names = {"--standard", "-s"}, description = "Compliance standard (default: SOC2)", defaultValue = "SOC2")
    private String standard;

    @Option(names = {"--provider", "-p"}, description = "Cloud provider (default: github)", defaultValue = "github")
    private String provider;

    @Option(names = {"--format", "-f"}, description = "Output format: pdf, json (default: pdf)", defaultValue = "pdf")
    private String format;

    @Option(names = {"--org", "-o"}, description = "GitHub organization name")
    private String orgName;

    @Option(names = {"--repo", "-r"}, description = "GitHub repository name (for repo-specific checks)")
    private String repoName;

    @Option(names = {"--output", "-O"}, description = "Output directory (default: ./audit-evidence)", defaultValue = "./audit-evidence")
    private String outputDir;

    @Option(names = {"--zip"}, description = "Create ZIP archive of all evidence files")
    private boolean createZip;

    private static final List<Soc2Check> ALL_CHECKS = List.of(
        new MfaEnabledCheck(),
        new RepoAccessControlCheck(),
        new BranchProtectionCheck(),
        new PrReviewsRequiredCheck(),
        new AuditLogCheck()
    );

    @Override
    public Integer call() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║     GitHub SOC2 Audit Evidence Exporter v1.0.0            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();

        if (!standard.equalsIgnoreCase("SOC2")) {
            System.err.println("Error: Only SOC2 standard is supported in this version.");
            return 1;
        }

        if (!provider.equalsIgnoreCase("github")) {
            System.err.println("Error: Only GitHub provider is supported in this version.");
            return 1;
        }

        String githubToken = System.getenv("GITHUB_TOKEN");
        if (githubToken == null || githubToken.isBlank()) {
            System.err.println("Error: GITHUB_TOKEN environment variable is required.");
            return 1;
        }

        if (orgName == null || orgName.isBlank()) {
            System.err.println("Error: Organization name is required. Use --org <name>");
            return 1;
        }

        String licenseKey = System.getenv("AUDIT_EVIDENCE_LICENSE_KEY");
        LicenseValidator validator = new LicenseValidator();
        LicenseValidator.LicenseInfo licenseInfo = validator.validate(licenseKey);

        System.out.println("License: " + licenseInfo.tier() + " - " + licenseInfo.message());
        System.out.println("Organization: " + orgName);
        if (repoName != null) {
            System.out.println("Repository: " + repoName);
        }
        System.out.println();

        GithubClient client = new GithubClient(githubToken);

        List<Soc2Check> checksToRun = selectChecks(licenseInfo);
        List<CheckResult> results = new ArrayList<>();
        List<Path> generatedFiles = new ArrayList<>();

        Path outputPath = Path.of(outputDir);

        System.out.println("Running " + checksToRun.size() + " checks...");
        System.out.println();

        for (Soc2Check check : checksToRun) {
            if (check.requiresRepo() && (repoName == null || repoName.isBlank())) {
                System.out.println("⏭  Skipping " + check.getCheckName() + " (requires --repo)");
                continue;
            }

            System.out.print("▶ Running: " + check.getCheckName() + "... ");

            try {
                CheckResult result = check.run(client, orgName, repoName);
                results.add(result);

                String statusIcon = switch (result.status()) {
                    case PASS -> "✓";
                    case FAIL -> "✗";
                    case PARTIAL -> "◐";
                };
                System.out.println(statusIcon + " " + result.status());

                Path filePath = exportResult(result, outputPath, licenseInfo);
                if (filePath != null) {
                    generatedFiles.add(filePath);
                }
            } catch (GithubApiException e) {
                System.out.println("✗ API Error: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("✗ Export error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("✗ Unexpected error: " + e.getMessage());
            }
        }

        try {
            createReadmeForAuditor(outputPath, results);
            generatedFiles.add(outputPath.resolve("README_FOR_AUDITOR.txt"));
        } catch (IOException e) {
            System.err.println("Warning: Could not create README_FOR_AUDITOR.txt: " + e.getMessage());
        }

        if (createZip) {
            if (!licenseInfo.canExportZip()) {
                System.out.println();
                System.out.println("⚠ ZIP export is only available in the paid tier.");
            } else {
                try {
                    ZipExporter zipExporter = new ZipExporter();
                    Path zipPath = outputPath.resolve("SOC2_Evidence_" + orgName + ".zip");
                    zipExporter.createZip(generatedFiles, zipPath);
                    System.out.println();
                    System.out.println("📦 Created ZIP archive: " + zipPath);
                } catch (IOException e) {
                    System.err.println("Error creating ZIP: " + e.getMessage());
                }
            }
        }

        System.out.println();
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("Summary: " + results.stream().filter(r -> r.status() == CheckResult.Status.PASS).count() + " passed, "
                + results.stream().filter(r -> r.status() == CheckResult.Status.FAIL).count() + " failed, "
                + results.stream().filter(r -> r.status() == CheckResult.Status.PARTIAL).count() + " partial");
        System.out.println("Output directory: " + outputPath.toAbsolutePath());
        System.out.println("════════════════════════════════════════════════════════════");

        return 0;
    }

    private List<Soc2Check> selectChecks(LicenseValidator.LicenseInfo licenseInfo) {
        int maxChecks = licenseInfo.maxChecks();
        if (maxChecks >= ALL_CHECKS.size()) {
            return ALL_CHECKS;
        }
        return ALL_CHECKS.subList(0, maxChecks);
    }

    private Path exportResult(CheckResult result, Path outputPath, LicenseValidator.LicenseInfo licenseInfo)
            throws IOException {
        String fileName = "SOC2_" + result.clauseId() + "_" + sanitizeFileName(result.checkName());

        if (format.equalsIgnoreCase("json")) {
            Path jsonPath = outputPath.resolve(fileName + ".json");
            JsonExporter jsonExporter = new JsonExporter();
            jsonExporter.export(result, jsonPath);
            return jsonPath;
        } else {
            Path pdfPath = outputPath.resolve(fileName + ".pdf");
            PdfExporter pdfExporter = new PdfExporter(licenseInfo.showWatermark());
            pdfExporter.export(result, pdfPath);
            return pdfPath;
        }
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private void createReadmeForAuditor(Path outputPath, List<CheckResult> results) throws IOException {
        Files.createDirectories(outputPath);

        StringBuilder sb = new StringBuilder();
        sb.append("SOC2 AUDIT EVIDENCE PACKAGE\n");
        sb.append("===========================\n\n");
        sb.append("Generated: ").append(Instant.now()).append("\n");
        sb.append("Standard: SOC2\n");
        sb.append("Provider: GitHub\n\n");

        sb.append("CONTENTS\n");
        sb.append("--------\n");
        for (CheckResult result : results) {
            sb.append("- ").append(result.clauseId()).append(": ").append(result.checkName());
            sb.append(" [").append(result.status()).append("]\n");
        }

        sb.append("\nNOTES FOR AUDITOR\n");
        sb.append("-----------------\n");
        sb.append("1. Each PDF/JSON file contains:\n");
        sb.append("   - The SOC2 clause being addressed\n");
        sb.append("   - Timestamp of data collection\n");
        sb.append("   - Raw API response data\n");
        sb.append("   - Pass/Fail determination with findings\n\n");

        sb.append("2. Data Source:\n");
        sb.append("   All evidence was collected directly from the GitHub REST API.\n");
        sb.append("   API endpoints used are documented in each evidence file.\n\n");

        sb.append("3. Verification:\n");
        sb.append("   The raw JSON data can be independently verified by calling\n");
        sb.append("   the documented GitHub API endpoints with appropriate authentication.\n\n");

        sb.append("Generated by: GitHub SOC2 Audit Evidence Exporter\n");
        sb.append("https://github.com/your-org/audit-evidence\n");

        Files.writeString(outputPath.resolve("README_FOR_AUDITOR.txt"), sb.toString());
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AuditEvidenceCli()).execute(args);
        System.exit(exitCode);
    }
}
