# Phase 3 — Final acceptance matrix

**Branch:** `feature/android-tv-pre-mvp`
**Audit date:** 2026-11-11
**Gate 3 decision:** **PASS WITH ACCEPTED RISKS** — PO sign-off 2026-11-11 (`PHASE3-GATE3-DECISION-SHEET.md` D1–D3)
**Authority:** `PHASE3-MANUAL-QA-CHECKLIST.md` § Gate 3 essential acceptance; Phase 2 consumption-only scope (`PHASE2-TECHNICAL-DESIGN.md`).

Legend: **Req** = required for Gate 3 per existing checklist. **Opt** = documented optional / soak / product decision.

| ID | Criterion | Checklist | Req | Status | Evidence source | Remaining | Owner | Pass condition |
|----|-----------|-----------|-----|--------|-----------------|-----------|-------|----------------|
| R01 | Automated regression (`testStagingDebugUnitTest`) | P3-001 | YES | **PASS** | Gradle 2026-11-11: **303** tests, **0** failures (`app/build/test-results/testStagingDebugUnitTest/`) | None | Dev | `BUILD SUCCESSFUL`, failures=0 |
| R02 | Pre-MVP hub navigation (no wallet / Screen Display) | P3-002, C1–C5 | YES | **PASS** (UT) / **NOT RUN — ACCEPTED RISK** (physical C1–C5) | UT + emulator manual; **D1** | Physical TV walk before Play closed test (P4-016) | QA + device | Hub rows only; no payment UI on LIVE (C4–C5) |
| R03 | Payment UX hidden on LIVE (flag) | §3 Gate3, C4 | YES | **PASS** (static) | `PreMvpFeatureFlags.enableTvPaymentUx=false`; `LiveTvFoundationScreen` gating | Device confirm C4 | QA | No Tips chip / drawer on LIVE |
| R04 | QR URL contract (automated) | P3-003, B2 | YES | **PASS** | `TipmannaShareUrlTest`, `LiveTvShareIdsTest` | None | Dev | Staging origin + UUID paths; no `/c/pay/` |
| R05 | QR LIVE mobile open | B3, B7 | YES | **PASS** | Manual 2026-11-10 staging (user-reported); `GATE3-EXECUTION-REPORT.md` §6 | None | QA | Scan opens same content on staging web |
| R06 | QR Explore mobile open | B3, B7 | YES | **PASS** | Manual 2026-11-10 staging | None | QA | Same as R05 for Explore |
| R07 | Staging share origin (not prod URL) | P3-DEF-001, B2 | YES | **PASS** (code+manual) | `BuildConfig.SHARE_CONTENT_ORIGIN` (uncommitted); manual QR pass | Commit verified build | PO/Dev | QR encodes staging Vercel origin |
| R08 | LIVE QR visibility (`channel.id`) | P3-DEF-004 | YES | **PASS** (code+manual) | `LiveTvShareIds.kt`; manual LIVE QR pass | Commit verified build | Dev/QA | QR visible when channel UUID present |
| R09 | Animation guard / dedup (logic) | P3-004, A6 | YES | **PASS** (UT) | `TipAnimationGuardTest`, `LiveTvTipAnimationRecoveryGuardTest` | Manual replay **Opt** if tooling allows | Dev | Second duplicate `transactionId` rejected in UT; manual: one overlay per payment |
| R10 | Wrong-programme rejection | A6, guard | YES | **PASS** (UT) | `TipAnimationGuardTest` (eventId mismatch) | Manual N2 in consolidated runbook **Opt** | Dev/QA | `tip-animation rejected by guard`; no overlay |
| R11 | Failed payment → no animation | A9 | YES | **NOT RUN — ACCEPTED RISK** | **D2**; `TipAnimationGuardTest` + backend success-gated emit | No additional payment test | QA + payments | No `tip-animation accepted` / no overlay |
| R12 | TV passive path: no wallet debit | A8, A7 | YES | **PASS** | Session 3 log `test-evidence/session3-passive-e2e/`; no wallet UI / checkout in log for txn `STG-P3-TXN-ID` | Screen recording optional (not in repo) | QA | No TV financial API during web-only tip |
| R13 | Staging Pusher identity on LIVE | A-pre P1–P3 | YES | **PASS** | Manual 2026-11-11 emulator; logs `cluster=eu`, key `1492…b418`, `matchesEmbeddedKey=true` | None | QA | Matches staging bootstrap / backend emit app |
| R14 | Pusher connect + `global-live-tv` + bind | A-pre P2–P4 | YES | **PASS** | Manual 2026-11-11; two cold starts | None | QA | Connect + subscribe + `Bound tip-animation` |
| R15 | Active programme `eventId` on LIVE | A-pre P5, A3 | YES | **PASS** | Log `STG-P3-EVENT-ID`; playback yes | None | QA | Non-empty UUID before tip |
| R16 | Pusher stability soak (10 min LIVE) | A-pre P6, A10, P3-DEF-003 | YES | **PASS** | Manual 2026-11-11: LIVE playing 10 min; no disconnect/DNS/subscription errors on `eu` | None (emulator) | QA | No `PUSHER connection error` / DNS; subscribe sustained |
| R17 | **Passive web payment → TV animation** | A5, A8, P3-DEF-002 | YES | **PASS** | Session 3 emulator log verified: txn `STG-P3-TXN-ID`, event `STG-P3-EVENT-ID`; `tv_paid_animation_event_received` → `tip-animation accepted` → removed overlay | Visual recording not in repo (PO attestation) | QA | Full passive path + correlation |
| R18 | Duplicate delivery (runtime) | A6 | YES | **NOT RUN — ACCEPTED RISK** | **D3**; R09 `TipAnimationGuardTest` + Session 3 single overlay for primary txn | No runtime Pusher replay claimed | QA/Backend | ≤1 animation per payment |
| R19 | LIVE / Explore playback | C10, P3-006 | YES | **PASS** (emulator) / **NOT RUN — ACCEPTED RISK** (physical C10) | Emulator LIVE+Explore manual; **D1** | Physical TV C10 (Phase 4/5) | QA | Stable playback ≥10 min on target device |
| R20 | Physical Android TV smoke | C1–C8 | YES | **NOT RUN — ACCEPTED RISK** | **D1**; emulator + UT only — **not** physical hardware | Hardware before Play closed test | QA | D-pad, back, auth, no payment paths |
| R21 | Authentication device flows | C7–C9, P3-010 | YES | **NOT RUN — ACCEPTED RISK** | Hub UT; C7–C9 not on physical TV; **D1** | Hardware auth walk (Phase 4/5) | QA | C7–C8 pass on hardware |
| R22 | Pusher config flavor alignment | Build | YES | **PASS** (UT+build) | `PusherConfigFlavorTest`, `assembleStagingDebug` (local, uncommitted) | Commit after PO review | Dev | Staging `eu` / prod `us3` in `BuildConfig` only |
| R23 | Stripe test mode for E2E payment | Payments policy | YES | **PASS** | Session 3: authorised payments owner; **livemode=false** for test txn `STG-P3-TXN-ID` | None | Payments owner | Document `livemode=false` for test txn |
| O01 | Anonymous Explore API | P3-011 | Opt | **NOT RUN** (product) | Backend optional auth; TV auth-gated | PO decision | PO | Out of Pre-MVP gate |
| O02 | Pusher dashboard event proof | S10 | Opt | **NOT RUN** | — | Optional read-only dashboard at payment time | Backend | Corroborate R17 |

### Defect reconciliation

| Defect | Gate impact | Status | Notes |
|--------|-------------|--------|-------|
| **P3-DEF-001** | R07 | **RESOLVED** (code+manual) | Staging origin; B pass |
| **P3-DEF-002** | R17 | **RESOLVED** | Session 3 log in `test-evidence/session3-passive-e2e/`; recording optional |
| **P3-DEF-003** | R16 | **RESOLVED** (emulator) | 10 min soak on `eu` with no DNS/disconnect errors — satisfies checklist stability for Gate 3; production long-haul = monitoring (Phase 6) |
| **P3-DEF-004** | R08 | **RESOLVED** (code+manual) | `channel.id`; LIVE QR pass |

### Emulator vs physical TV

| Topic | Policy (existing checklist) | Current position |
|-------|----------------------------|------------------|
| Animation E2E R17 | Allowed on emulator for staging | **PASS** (Session 3, 2026-11-11) |
| QR B1–B8 | Manual pass obtained (emulator + phone) | **PASS** |
| C1–C8 | **Mandatory on physical Android TV** (S1) for Play | **NOT RUN — ACCEPTED RISK** (D1, 2026-11-11) — internal Gate 3 only |
| Pusher soak R16 | A-pre / A10 | **PASS** on emulator (documented) |

### Residual risks (accepted — PO 2026-11-11)

| Risk | Owner | Reason |
|------|-------|--------|
| Physical TV C1–C8 not run | **ACCEPTED (D1)** | Emulator evidence only; does not waive Play device/policy requirements |
| Manual A9 failed payment not run | **ACCEPTED (D2)** | Automated negative-path + architecture; no new payment |
| Runtime duplicate replay not run | **ACCEPTED (D3)** | `TipAnimationGuardTest` + Session 3 single E2E |
| Session 3 screen recording absent | QA/PO | Log corroborates passive path; overlay visibility = PO attestation |
