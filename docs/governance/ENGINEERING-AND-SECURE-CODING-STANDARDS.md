# Engineering and Secure Coding Standards

**Authority for GATE-S08 criterion S08-G8**  
**Owner:** Amit / Engineering · **Security co-owner:** Deepali / Security  
**Status:** Published 2026-09-13 · **Layer:** L3 — this repository (builds on [ORG-STANDARDS.md](./ORG-STANDARDS.md) L2)  
**Adoption mechanism:** [PR-REVIEW-CHECKLIST.md](./PR-REVIEW-CHECKLIST.md) and `.github/pull_request_template.md`

This document is the engineering and secure-coding standard the S08-G8 criterion asked for.
ORG-STANDARDS.md remains organisational policy; this file is what reviewers and authors cite on
every change.

---

## 1. Engineering standards (ENG)

| ID | Rule | How it is enforced |
|----|------|--------------------|
| ENG-1 | Java 21 + Gradle multi-module layout; no new top-level language without an ADR | CI `application-ci.yml`; ArchUnit module boundaries |
| ENG-2 | Hexagonal / ports-and-adapters: vendor and 1SB types stay in adapter packages | ArchUnit service rules |
| ENG-3 | Shared behaviour lives in `libs/*`; services do not copy-paste platform concerns | Review checklist + ArchUnit |
| ENG-4 | Public HTTP contracts are OpenAPI-first; breaking changes need an ADR or version bump | Review checklist |
| ENG-5 | Every change ships with tests at the appropriate pyramid level; coverage gates must stay green | `jacocoTestCoverageVerification` on `check` |
| ENG-6 | Static analysis must stay green: Checkstyle + Spotless (ratchet from `origin/main`) | `checkstyleMain` / `spotlessCheck` on `check` |
| ENG-7 | Architecture fitness must stay green; empty ArchUnit suites fail CI | architecture-fitness CI step |
| ENG-8 | No floating versions for production dependencies; lockfiles / BOM where the build already uses them | Review checklist |
| ENG-9 | Feature work does not weaken S08 gates (coverage, scanning, ArchUnit, Checkstyle/Spotless) | Review checklist |
| ENG-10 | TODOs carry a work-item id; nothing is Done without evidence in GATE-EVIDENCE or the story | Review checklist |

## 2. Secure coding standards (SEC)

These tighten ORG-STANDARDS SEC-1…SEC-9 for day-to-day code review.

| ID | Rule | How it is enforced |
|----|------|--------------------|
| SEC-C1 | No secrets in source, tests, fixtures, or logs | gitleaks CI; review checklist |
| SEC-C2 | No PAN, Aadhaar, phone, email, or health attributes in logs; use platform scrubbing | `NoPiiInEmittedLogsTest` / LogPiiScrubber; SAST |
| SEC-C3 | Validate and sanitise all external input at the boundary; fail closed | Review checklist; SAST |
| SEC-C4 | Default-deny authorisation; never trust client-supplied roles alone | Review checklist; ArchUnit where applicable |
| SEC-C5 | Prefer parameterised queries / JPA; no string-concatenated SQL | Review checklist; SAST |
| SEC-C6 | Dependencies with reachable CRITICAL/HIGH CVEs block merge | Trivy SCA CI |
| SEC-C7 | Container images for Phase-1 deployables are scanned; CRITICAL/HIGH block | Trivy image-scan CI |
| SEC-C8 | Security-relevant actions emit auditable events with actor attribution | Review checklist |
| SEC-C9 | OAuth / IdP tokens never reach Flutter or browser storage; BFF holds them | Review checklist (WS-2) |
| SEC-C10 | New trust-boundary or crypto choices need Deepali review before merge | Review checklist |

## 3. Definition of done (change-level)

A change is Done only when:

1. Tests and coverage verification are green for touched modules.
2. Checkstyle, Spotless, and ArchUnit are green.
3. Security scanning jobs applicable to the change are green.
4. The PR checklist in `.github/pull_request_template.md` is completed and cites ENG/SEC ids where relevant.
5. GATE or story evidence is updated when the change closes a criterion or debt item.

## 4. Exceptions

Temporary exceptions require: owner, expiry date, compensating control, and a TECH-DEBT or RISK id.
An exception with no expiry is a scope change and is not permitted (see gate waiver rules).
