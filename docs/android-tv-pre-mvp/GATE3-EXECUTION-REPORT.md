# Gate 3 — Execution report (Phase 3 testing and acceptance)

**Date:** 2026-11-11 (Phase 3 closeout audit)
**Branch:** `feature/android-tv-pre-mvp`
**Base SHA:** `281fe26be890bbea435a42edf0235f40538d5d12` (`origin/main`)
**Phase 1 docs commit:** `644d7df433d6bcd74dbb6ba4957e9baff3007e55`
**Phase 2 implementation commit:** `cdb73e2e2d3b6d3d7e9c30aa5cb252a91fb2a980`
**Phase 3 docs commit:** `f16faa1911d839d64f4c526dac5bdec5bebfcbfd`
**Manual QA playbook:** `PHASE3-MANUAL-QA-CHECKLIST.md`
**Push / PR:** None (per authorisation).

### Repository state (verified)

| Item | Value |
|------|--------|
| Branch | `feature/android-tv-pre-mvp` |
| `git log` (top) | `f16faa1` → `cdb73e2` → `644d7df` → `281fe26` |
| vs `origin/main` | **Ahead 5** (not pushed) |
| Working tree | Uncommitted: Pusher alignment, QR/share fixes, Phase 3 docs, `PusherConfigFlavorTest.kt` |
| PR #2 | OPEN — `feature/android-tv-mvp-focused` — not merged or modified |
| PR #154 | Not touched (no changes in backend worktree for Pre-MVP) |

## Gate 3 recommendation

**PASS WITH ACCEPTED RISKS** (Authorized Product Owner 2026-11-11 — `PHASE3-GATE3-DECISION-SHEET.md` D1–D3)

**Authoritative matrix:** `PHASE3-FINAL-ACCEPTANCE-MATRIX.md`
**Session 3 passive E2E:** **PASSED** — evidence in `test-evidence/session3-passive-e2e/SESSION3-SANITIZED-LOG-SUMMARY.md` (full log: private archive).
**Accepted risks:** R20 (physical TV), R11 (manual A9), R18 (runtime replay); R02/R19/R21 physical device portions under D1.

Automated regression is green (**303** tests, 0 failures, 2026-11-11 closeout re-run + `assembleStagingDebug` **BUILD SUCCESSFUL**).

### Manual acceptance matrix

| Area | Key cases | Status | Evidence obtained |
|------|-----------|--------|-------------------|
| A. Web → passive TV animation | A5, A8 (Session 3) | **PASSED** | Txn `STG-P3-TXN-ID`; passive accept + overlay; A9/A6 manual optional |
| A-pre. Staging Pusher LIVE runtime + soak | P1–P6 | **PASSED** (emulator, 2026-11-11) | Identity + connect + subscribe + bind; 10 min soak no DNS/disconnect; programme `STG-P3-EVENT-ID` |
| B. QR → mobile (LIVE + Explore) | B1–B8 | **PASSED** (staging, manual 2026-11-10) | Explore + LIVE scan to staging Vercel URL |
| C. Physical Android TV | C1–C10 | **NOT RUN — ACCEPTED RISK (D1)** | Emulator only; no physical hardware |
| Automated regression | 301 unit tests | **PASSED** | Gradle XML 0 failures (2026-11-10) |
| Payment exclusion (static) | Code review | **NOT RUN** (device) | Architecture only — device walkthrough pending C4–C5 |
| Authentication | C7–C9 | **NOT RUN — ACCEPTED RISK (D1)** | Hub UT; device auth not on physical TV |

**Gate 3 closeout:** All mandatory rows satisfied via PASS, PASS (UT), or **NOT RUN — ACCEPTED RISK**. **P3-DEF-002** closed with Session 3 log artifact.

---

## 1. Phase 2 preservation (Part A)

| Check | Result |
|-------|--------|
| Branch | `feature/android-tv-pre-mvp` |
| Phase 1 baseline commit | `644d7df` on same branch |
| Financial / wallet / Stripe / web-mobile payment code in commit | **None** (Android TV UI/navigation/share only) |
| PR #2 | OPEN — not merged or modified (`feature/android-tv-mvp-focused`) |
| PR #154 | Not touched (no backend repo changes) |
| Unrelated / sensitive files in commit | **Excluded** (`local.properties` gitignored) |

**Working tree after Phase 2 commit:** clean (`ahead 2` of `origin/main`, no push).

---

## 2. Automated regression

**Command:**

```text
.\gradlew.bat :app:testStagingDebugUnitTest --no-daemon
```

**Result (2026-11-10):** `BUILD SUCCESSFUL` (~2m 9s; tasks UP-TO-DATE after prior full run).

**Counts** (from `app/build/test-results/testStagingDebugUnitTest/*.xml`):

| Metric | Value |
|--------|-------|
| Test classes | 78 |
| Test methods | **303** (includes `PusherConfigFlavorTest`) |
| Failures | **0** |
| Errors | **0** |

**Pre-MVP–relevant classes (non-exhaustive):**

- `PreMvpNavigationTest`, `TelepseStartupNavigationTest`, `NavigationBackResolverTest`
- `TipmannaShareUrlTest`, `LiveTvShareIdsTest`
- `TipAnimationGuardTest`, `LiveTvTipAnimationRecoveryGuardTest`, `LiveTvPaidAnimationRecoveryTest`, `LiveTvPaidAnimationRecoveryRetryTest`
- `ExploreFeedDeserializationTest`, `JourneyHubViewModelTest`, `TvModeEngineTest`, `LiveTvFoundationViewModelTipLifecycleTest`

Log artefact: `tipmanna_tv_app/build-phase3-unit-test.log` (Gradle console).

---

## 3. Payment exclusion evidence

**Static / architectural (not device-recorded):**

| Surface | Pre-MVP behaviour |
|---------|-------------------|
| Hub (`PreMvpNavigation`, `JourneyHubScreen`) | LIVE, Explore, sign-in/out only — no Wallet, Screen Display, spikes |
| `MainActivity` hub callbacks | No navigation to `TipsWallet`; Screen Display entry removed from hub |
| `PreMvpFeatureFlags.enableTvPaymentUx` | `false` |
| `LiveTvFoundationScreen` | Tip drawer, preview strip, Tips chip, wallet/QR checkout **not rendered** when flag false |
| `TelepseStartupNavigation` + flag | Screen Display **auto-restore disabled** on resume |
| Deep screens | `TipsWallet`, `CodeInput`, `FundraisingMilestone` composables remain in graph but **no hub path** in Pre-MVP |

**Infrastructure preserved:** `WalletRepository`, tip coordinators, `LiveTvTipDrawer`, Pusher, `LiveTvPaidAnimationRecovery` unchanged in codebase.

---

## 4. LIVE TV (manual)

| Criterion | Status |
|-----------|--------|
| Opens / plays / remote / Back | **Partial** — emulator LIVE playback **yes** (2026-11-11); hardware not run |
| No payment controls | **Code-gated** (see §3) |
| Share QR only with valid UUIDs | **Implemented** (`VideoShareQrOverlay` + `TipmannaShareUrl`; null → no QR) |
| Animation host active | **Code unchanged** (`TipAnimationOverlay`, ViewModel `onEnter` Pusher paths) |
| Staging Pusher identity on LIVE | **PASSED** (emulator, 2026-11-11) — see §4a |

### 4a. Staging Pusher LIVE runtime (2026-11-11)

**Build:** `stagingDebug` after flavor `BuildConfig` Pusher alignment (public key/cluster only; not committed in this doc pass).
**Device:** `emulator-5554`, package `com.example.telepse.staging`.
**Historical note:** pre-fix run did not prove passive animation; Session 3 post-fix **PASSED** — **P3-DEF-002 RESOLVED** (log artifact in repo).

| # | Check | Result |
|---|--------|--------|
| P1 | Staging cluster `eu` in runtime credential log | **PASS** |
| P2 | Masked public key `1492…b418` | **PASS** |
| P3 | `matchesEmbeddedKey=true` | **PASS** |
| P4 | Pusher connection established | **PASS** (two process starts; both connected) |
| P5 | Subscribed to `global-live-tv` | **PASS** |
| P6 | `Bound tip-animation on global-live-tv` | **PASS** |
| P7 | Active programme `eventId` populated on LIVE | **PASS** — `STG-P3-EVENT-ID` |

**Notes:** First observer log showed empty `activeEventId=` before schedule bind; later log showed the programme UUID above (expected startup ordering). LIVE video playing confirmed.

### 4b. Pusher stability soak (2026-11-11)

**Duration:** 10 minutes on LIVE (emulator). **Result:** **PASS** — playback continued; no unexpected Pusher disconnect; no DNS errors; no subscription/listener errors on `eu`. **P3-DEF-003:** **RESOLVED** for Gate 3 (residual long-haul monitoring → Phase 6).

---

## 5. Web → passive TV animation E2E

**Status: PASSED** (Session 3, 2026-11-11, emulator `Television_1080p`)

| Field | Value |
|-------|--------|
| Transaction ID | `STG-P3-TXN-ID` |
| Event ID | `STG-P3-EVENT-ID` |
| Stripe | Payments owner; **livemode=false** |
| TV logs | `Received tip-animation` → `tv_paid_animation_event_received` → **`tip-animation accepted`**; animation queued and **visible** |
| Financial safety | No TV wallet UI; no TV debit/checkout |
| Artifacts | Log + recording saved by tester — **paths not in repo** (see `PHASE3-GATE3-DECISION-SHEET.md` D5) |

Pre-fix txn `STG-P3-TXN-INVALID` remains invalid evidence (wrong Pusher app).

**Policy:** Payer-owned TV recovery (`LiveTvPaidAnimationRecovery`) is **not** acceptable evidence for passive catch-up. No replay API was added.

**Expected path (for QA when unblocked):** web test payment → backend success → Pusher emit → TV `LiveTvTipAnimationHost` → `TipAnimationGuard` once → overlay only; **no** `submitWalletCheckout` / debit on TV.

---

## 6. QR scanning (LIVE + Explore)

**Status: PASSED** (manual staging, 2026-11-10; staging share origin after fix)

**Automated contract checks:** `TipmannaShareUrlTest`, `LiveTvShareIdsTest`.

**Manual:** LIVE + Explore scan opens staging content URL; not `/c/pay/`.

---

## 7. Physical Android TV

**Status: NOT EXECUTED**

No physical Android TV device was available. Emulator was **not** used as a substitute for acceptance per Phase 3 instructions.

**Outstanding:** model, Android version, cold launch, D-pad, QR legibility, animation on glass, session expiry.

---

## 8. Authentication

| Item | Result |
|------|--------|
| Pre-MVP policy | Explore + LIVE remain **auth-gated** in `MainActivity.requireAuth` |
| Weakened auth for anonymous Explore | **Not done** |
| Backend anonymous Explore | `POST /api/v1/contents/feed/batch` registered with `optionalAuthMiddleware` in backend (`content.route.ts`) — **future PO decision** whether TV may call without tipper session; TV app does not today |
| Automated session hub tests | `JourneyHubViewModelTest` (existing suite) |

---

## 9. Defects

| ID | Severity | Status | Notes |
|----|----------|--------|-------|
| P3-DEF-001 | P0 | **RESOLVED (code)** | Flavor `SHARE_CONTENT_ORIGIN`; re-verify with manual QR scan on staging build |
| P3-DEF-002 | P0 | **RESOLVED** | Session 3 passive E2E — attach artifacts |
| P3-DEF-003 | P2 | **RESOLVED** | 10 min LIVE soak on `eu` — no DNS/disconnect (2026-11-11 emulator) |
| P3-DEF-004 | P0 | **FIXED (code)** | `channel.id` mapping — manual LIVE scan pending |

Detail: `PHASE3-DEFECT-REGISTER.md`, `PHASE3-INVESTIGATION-REPORT.md`.

Automated unit tests: 0 failures (303, 2026-11-11). Local uncommitted code: Pusher flavor config, QR/share fixes.

---

## 10. Phase 4+ (not started)

See `PHASE4-6-ROADMAP.md`. **Do not begin Phase 4** until Gate 3 **PASS**.

---

## 11. Dependencies / who is needed

Full setup table: **`PHASE3-MANUAL-QA-CHECKLIST.md` § QA setup**.

| Blocker | Needed from |
|---------|-------------|
| P3-007 Animation E2E | **Done** (Session 3) — attach artifacts |
| P3-008 QR routing | **Done** — spot-check only if regression suspected |
| P3-006 / P3-009 Device | Physical Android TV + `assembleStagingDebug` APK + adb |

---

## 12. Task register

See `PHASE3-TASK-REGISTER.md`.

---

## 13. Separation confirmation

- PR #2, PR #154: untouched
- No backend deploy, no Play upload, no Phase 4 work
- Branch not pushed
