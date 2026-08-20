# LumaSift Android

LumaSift for Android is an authenticated Android companion to a trusted LumaSift Windows coordinator. It helps an owner identify exact duplicate media and document files safely: videos, MP3 audio, DOCX documents, PDFs, and images.

## Safety Contract

LumaSift may use a sampled hash to discover collision candidates, but it must calculate a **full SHA-256 hash** before a duplicate group is actionable. It retains the highest-ranked exact copy, proposes lower-ranked copies for **recoverable quarantine**, and requires a separate explicit purge action.

## Engineering Governance

This repository is governed by [AGENTS.md](AGENTS.md), the [Total Automation Policy](.github/AUTOMATION_POLICY.md), and the [Master Engineering Standard](.github/MASTER_ENGINEER_STANDARD.md). The automated governance workflow fails if the required engineering and release artifacts are missing.

## Coordinator Connection

The Android application is a **companion**, not a device-side deletion engine. It connects only to an owner-configured HTTPS LumaSift Windows coordinator, presents selected file-type controls, polls live plan status, displays exact-group evidence, and asks the coordinator to apply an approved quarantine plan. It never accepts, stores, or transmits NAS credentials or raw coordinator file paths.

The coordinator URL must use `https://` and the companion sends an owner-provided bearer token with each request. Production deployments should place the coordinator behind a trusted TLS endpoint on the owner’s network.

## Development and Validation

| Check | Command |
| --- | --- |
| Governance | `python3 scripts/verify_governance.py` |
| Android TEST APK | `gradle :app:assembleDebug` |

The hosted workflow installs Java 17 and Gradle, builds the debug APK, writes a SHA-256 checksum, and publishes a clearly labelled TEST prerelease.

## Current Delivery Scope

The repository targets a clearly labelled TEST APK unless production Android signing secrets are configured. A production Play Store or managed-distribution APK requires `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`; none are committed or required for the TEST workflow. See [RELEASE_NOTES.md](RELEASE_NOTES.md) for current limitations and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the platform boundary.
