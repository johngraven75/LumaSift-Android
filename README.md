# LumaSift Android

LumaSift for Android is an authenticated Android companion to a trusted LumaSift Windows coordinator. It helps an owner identify exact duplicate media and document files safely: videos, MP3 audio, DOCX documents, PDFs, and images.

## Safety Contract

LumaSift may use a sampled hash to discover collision candidates, but it must calculate a **full SHA-256 hash** before a duplicate group is actionable. It retains the highest-ranked exact copy, proposes lower-ranked copies for **recoverable quarantine**, and requires a separate explicit purge action.

## Engineering Governance

This repository is governed by [AGENTS.md](AGENTS.md), the [Total Automation Policy](.github/AUTOMATION_POLICY.md), and the [Master Engineering Standard](.github/MASTER_ENGINEER_STANDARD.md). The automated governance workflow fails if the required engineering and release artifacts are missing.

## Current Delivery Scope

The repository targets a clearly labelled TEST APK unless production Android signing secrets are configured. See [RELEASE_NOTES.md](RELEASE_NOTES.md) for current limitations and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the platform boundary.
