# Phase 3 — Task register (testing and acceptance)

**Branch:** `feature/android-tv-pre-mvp`
**Gate 3 report:** `GATE3-EXECUTION-REPORT.md`
**Matrix:** `PHASE3-FINAL-ACCEPTANCE-MATRIX.md`
**Manual session:** `PHASE3-CONSOLIDATED-MANUAL-RUNBOOK.md`
**Defects:** `PHASE3-DEFECT-REGISTER.md`
**Gate decisions:** `PHASE3-GATE3-DECISION-SHEET.md`

| ID | Acceptance item | Status | Evidence |
|----|-----------------|--------|----------|
| P3-001 | Full `testStagingDebugUnitTest` suite | **PASSED** | **303** tests, 0 failures (2026-11-11) |
| P3-002 | Pre-MVP navigation / payment exclusion (automated) | **PASSED** | `PreMvpNavigationTest`, hub wiring |
| P3-003 | QR URL generation (automated) | **PASSED** | `TipmannaShareUrlTest`, `LiveTvShareIdsTest` |
| P3-004 | Animation guard / dedup (automated) | **PASSED** | `TipAnimationGuardTest`, recovery guard tests |
| P3-004b | Staging Pusher flavor config (automated) | **PASSED** | `PusherConfigFlavorTest` (local, uncommitted) |
| P3-005 | Payment exclusion (manual device) | **NOT RUN — ACCEPTED RISK (D1)** | C4–C5 on emulator/UT; no physical TV |
| P3-006 | LIVE playback on device | **PASS (emulator) / D1 hardware** | Emulator yes; physical C10 deferred Phase 4/5 |
| P3-007 | Web → passive TV animation E2E (post-fix) | **PASSED** | Session 3 — txn `STG-P3-TXN-ID` |
| P3-008 | QR scan LIVE + Explore (mobile) | **PASSED** | Manual 2026-11-10 staging |
| P3-009 | Physical Android TV regression | **NOT RUN — ACCEPTED RISK (D1)** | C1–C10 not on hardware |
| P3-010 | Auth signed-in / signed-out / expiry | **NOT RUN — ACCEPTED RISK (D1)** | C7–C9 not on physical TV |
| P3-015 | Gate 3 decision | **PASS WITH ACCEPTED RISKS** | PO 2026-11-11; matrix + decision sheet |
| P3-011 | Anonymous Explore backend capability | **NOT RUN** (product) | Backend `optionalAuth`; TV auth-gated |
| P3-012 | Manual QA playbook + consolidated runbook | **PASSED** | Checklist + consolidated runbook published |
| P3-013 | Staging Pusher LIVE runtime | **PASSED** | A-pre P1–P5, 2026-11-11 |
| P3-014 | Pusher 10 min stability soak | **PASSED** | A-pre P6, 2026-11-11; P3-DEF-003 resolved |
