# GitHub SOC2 Audit Evidence Exporter

![GitHub Marketplace](https://img.shields.io/badge/Marketplace-Available-blue)
![License](https://img.shields.io/badge/License-Commercial-yellow)
![Java 17](https://img.shields.io/badge/Java-17-blue)

**One-click, auditor-ready SOC2 evidence pack for GitHub repositories.**

Stop spending hours manually collecting screenshots and exporting settings for SOC2 audits.  
This tool automatically gathers **verifiable GitHub evidence** and exports it as **professional PDFs ready to send to your auditor**.

---

## ⚡ Answer in 30 seconds

**❓ When do I need this?**  
Your auditor requested GitHub SOC2 evidence.

**❓ What do I get?**  
Auditor-ready PDFs with:
- SOC2 clause mapping
- Timestamps
- GitHub API endpoints
- Raw JSON for verification

**❓ How much does it cost?**
- **Free**: $0 — 2 checks, watermarked PDFs
- **Paid**: $99/month — all 5 checks, ZIP export, no watermark
> Cheaper than 1 hour of engineer time

---

## The Problem

Auditors typically request evidence for:

- MFA enforcement
- Repository access control
- Branch protection rules
- Pull request review requirements
- Audit log availability

Manual process:
- Navigate GitHub UI
- Take screenshots
- Export settings
- Format documents

⏱ **Time spent:** 4–8 hours per audit

---

## The Solution

Run **one command or workflow**.  
Get **auditor-ready PDFs** in **under 2 minutes**.

---

## 🚀 Quick Start — GitHub Action (Recommended)

```yaml
name: Generate SOC2 Evidence

on:
  workflow_dispatch:
  schedule:
    - cron: '0 0 1 * *' # Monthly

jobs:
  audit-evidence:
    runs-on: ubuntu-latest
    steps:
      - uses: jekka001/audit-evidence@v1
        with:
          standard: SOC2
          org: ${{ github.repository_owner }}
          repo: ${{ github.event.repository.name }}
          license-key: ${{ secrets.AUDIT_EVIDENCE_LICENSE_KEY }}
          create-zip: true

      - uses: actions/upload-artifact@v4
        with:
          name: soc2-evidence
          path: audit-evidence/
```

## 💻 CLI Usage

export GITHUB_TOKEN=ghp_xxxxxxxxxxxx

java -jar audit-evidence.jar \
  --standard SOC2 \
  --org my-org \
  --repo my-repo \
  --format pdf \
  --zip   # paid tier only

📌 --repo is optional.
If omitted, only organization-level checks will run.

## 📦 Output

audit-evidence/
├── SOC2_CC6.1_MFA_Enforcement.pdf
├── SOC2_CC6.2_Repository_Access_Control.pdf
├── SOC2_CC7.2_Branch_Protection_Rules.pdf
├── SOC2_CC7.2_Pull_Request_Reviews_Required.pdf
├── SOC2_CC7.3_Audit_Log_Availability.pdf  <- Enterprise only
└── README_FOR_AUDITOR.txt


Each PDF contains:

- SOC2 clause
- Timestamp of data collection
- GitHub API endpoint used
- PASS / FAIL / PARTIAL
- Raw JSON response for verification

⚠️ CC7.3 Audit Log check requires GitHub Enterprise Cloud.
For Free/Pro orgs this check may return FAIL — expected behavior.

## 🔍 SOC2 Checks Included

| Check                     | Clause | Description                                |
| ------------------------- | ------ | ------------------------------------------ |
| MFA Enforcement           | CC6.1  | Ensures MFA is enabled for all org members |
| Repository Access Control | CC6.2  | Lists admins & writers                     |
| Branch Protection         | CC7.2  | Verifies default branch protection rules   |
| PR Reviews Required       | CC7.2  | Checks if PR approvals are enforced        |
| Audit Log Availability    | CC7.3  | Enterprise-only, verifies audit log access |


## 💰 Pricing

Free Tier

- 2 checks (MFA + Repo Access Control)
- Watermarked PDFs
- Evaluation use

Paid Tier — $99/month

- All 5 checks
- No watermark
- ZIP archive export
- Priority support

- uses: jekka001/audit-evidence@v1
  with:
  standard: SOC2
  org: my-org
  license-key: ${{ secrets.AUDIT_EVIDENCE_LICENSE_KEY }}
  create-zip: true

## 🔐 Requirements

- GitHub organization (not personal account)
- Token scopes: read:org, repo, admin:org
- For CC7.3: GitHub Enterprise Cloud

## ❓ FAQ

Q: Does this tool make compliance decisions?
A: No. It only collects evidence. Final compliance judgment is made by your auditor.

Q: How often should I run this?
A: Before each audit. For SOC2 Type II — monthly or quarterly.

Q: Can I use this for multiple organizations?
A: Yes. Run the CLI or action once per organization.

## 🛠 Support

- GitHub Issues: https://github.com/jekka001/audit-evidence/issues
- Email: builtindays@gmail.com

Stop gathering evidence manually.
Start passing audits faster.