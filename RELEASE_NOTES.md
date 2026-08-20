# LumaSift Android Release Notes

## 0.1.0 — Standalone Android Companion TEST

### Included

- Public standalone repository with the repository-wide engineering and Total Automation policies.
- Enforced Master Engineering Standard and automated governance check.
- Dedicated Kotlin/Jetpack Compose companion for an owner-configured HTTPS LumaSift Windows coordinator.
- Selected-category controls for video, MP3 audio, DOCX/PDF documents, and images; live progress; reviewable exact groups; and quarantine confirmation.
- Typed bearer-authenticated API client that does not accept or expose NAS credentials or raw coordinator paths.
- Automated Android TEST APK build, checksum, and prerelease publication workflow.

### Safety and Integration Boundary

The Android app is a review-and-approval companion. The Windows coordinator retains responsibility for source scanning, sampled candidate hashing, full SHA-256 proof, deterministic plan generation, revalidation, quarantine, and purge protection. The Android app requests status, starts a selected-category plan, displays its review data, and requests an approved quarantine application.

### Distribution Status

The workflow publishes a clearly labelled debug-signed TEST APK with SHA-256 checksum evidence. It is not a production-signed Play Store artifact.

### Production Signing Limitation

A production Android build requires `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. These signing secrets are intentionally absent from the repository and TEST workflow.

### Rollback

Before release publication, revert the standalone companion commit while retaining the governance baseline. After publication, install the preceding verified TEST artifact only for evaluation; production rollback procedures will be documented after signed Android distribution is configured.
