# Task 11AQ — Wallet Paid Animation: Investigation & Fix Report

**Date:** 2026-11-10  
**Staging base (pre-fix deploy):** `d4c65ce705dfa5dd1beece0bf857ffd60aabf8bd`  
**Backend fix branch:** `staging/task11aq-wallet-recovery-eventid` @ `48297cc0d09d8b03af8710fcfd03cb7da7a37cb4`  
**Kotlin tree:** `tipmanna_tv_app-phase2-recovery` / `feature/kotlin-tv-wallet-checkout` (local uncommitted 11AP+11AQ changes)  
**11AP APK (superseded):** `7B848ABF3F2B99F75C3F18F4A8A149387941D783CE3499B94DBAFE64E075C81F`  
**11AQ diagnostic APK:** `53B8E3729B19E2F1B408DEB87D1BDF99D3177E56EEB280E907E764C9B9705073`  
**APK path:** `app/build/outputs/apk/staging/debug/app-staging-debug.apk`

---

## A. Current state

| Item | Value |
|------|--------|
| Staging backend commit (user-reported) | `d4c65ce705dfa5dd1beece0bf857ffd60aabf8bd` |
| Backend fix commit (local, not on staging yet) | `48297cc0d09d8b03af8710fcfd03cb7da7a37cb4` |
| Backend branch | `staging/task11aq-wallet-recovery-eventid` (from 11AP `3744b70c…`) |
| Kotlin branch | `feature/kotlin-tv-wallet-checkout` |
| Kotlin HEAD (git) | `9475a82` (wallet/recovery files largely uncommitted / untracked on branch) |
| Working tree | Backend: clean on fix branch; Kotlin: modified + new recovery/diagnostic sources |

---

## B. Evidence — pipeline stages

| Stage | Status | Evidence |
|-------|--------|----------|
| 1. Wallet debit succeeds | **PASS** (prior staging) | Task 11AM/11AO operator reports; not re-run in 11AQ |
| 1b. Debit JSON includes `animationRecovery` | **FAIL** on `d4c65ce` when `body.eventId ≠ tx.eventId` | Code: `issueRecoverySessionForSucceededTipTransaction` required `tx.eventId === params.eventId`; `buildWalletDebitSuccessJson` passed `body.eventId` while debit uses `resolved.effectiveEventId` (`wallet.controller.ts` ~613, ~44–70). Confirmed on deploy commit via `git show d4c65ce:…payment-animation-recovery.service.ts` |
| 2. Kotlin parses / invokes recovery | **PASS** (mocked) | `LiveTvPaidAnimationRecoveryTest`, `LiveTvWalletCheckoutTest` |
| 2b. Recovery skipped when hints missing | **PASS** (by design + traced) | `wallet_animation_recovery_missing` in `WalletPaidAnimationTrace` |
| 3. Recovery claim | **NOT VERIFIED** on staging wallet path | No post-`d4c65ce` logcat with wallet debit; QR claim path PASS in 11AH |
| 4. Event publication (claim path) | **PASS** (mocked / QR reference) | Same `claimAnimationRecovery` as QR; backend spec covers wallet empty PI |
| 5. TV subscription (Pusher) | **NOT VERIFIED** for wallet | Emulator wallet flows used recovery, not Pusher alone |
| 6. Animation render (recovery) | **FAIL** on staging with 11AP APK | Debit + “Tip confirmed”, no animation; consistent with missing `animationRecovery` or guard `eventId` mismatch |
| 6b. Guard `eventId` (client) | **FIXED** in 11AQ Kotlin | `recoverAfterWalletPaid` uses `hints.eventId` for programme guard; test `recoverAfterWalletPaid_usesAuthoritativeHintsEventIdForGuard` |

### Available vs missing evidence

**Available:** Source on `d4c65ce`; 11AP implementation; unit tests; prior task reports (`task11ao`, `task11ap`); staging health at `d4c65ce`; diagnostic log tags added in 11AQ APK.

**Missing:** Staging debit response body capture (presence/absence of `animationRecovery` without logging tokens); wallet recovery claim HTTP status from device on `d4c65ce`; DB read of `TipAnimationDelivery` for last wallet tx; end-to-end animation on staging after **both** backend `48297cc0` deploy and 11AQ APK.

---

## C. Root cause (confirmed from code)

**Primary (backend):** On successful wallet debit, `animationRecovery` is omitted when the stored transaction `eventId` (`resolved.effectiveEventId` at debit time) does not exactly match the request `body.eventId` passed into `issueRecoverySessionForSucceededTipTransaction`. Catalogue / schedule binding can legitimately use the **effective** event id on the transaction while the client still sends a **surface** `eventId` on the debit JSON (same class of mismatch as catalog fetch reroute in `LiveTvTipCoordinator`).

**Effect:** Kotlin receives `200` with `newBalance` but **no** `animationRecovery` → `recoverAfterWalletPaid` returns `false` immediately (pre-11AQ: silent). This is **not** caused by debit failure and is **not** proven fixed by compilation alone.

**Secondary (client, when hints present):** Recovery claim used QR path with `activeEventId` from cart; guard could reject payloads whose authoritative `eventId` came from the server hint. Mitigated by using `hints.eventId` for `recoverAfterQrPaid` programme id.

**Not root cause (for typical wallet failure):** Missing 11AP wiring (present on APK `7B848…`); Stripe PI null check (addressed in 11AP for wallet).

---

## D. Corrective work

### Backend (`backend-wt-task11ap`)

| File | Change |
|------|--------|
| `payment-animation-recovery.service.ts` | Issue wallet recovery session from **DB `tx.eventId` only**; drop request `eventId` equality check; return `eventId` in session result |
| `wallet.controller.ts` | `buildWalletDebitSuccessJson` no longer passes client `eventId`; `animationRecovery.eventId` from issued session |
| `payment-animation-recovery.service.spec.ts` | Regression: issuance when stored event differs from hypothetical request surface id |

**Why it fixes the cause:** Eligible succeeded wallet transactions always get a recovery token when feature flag allows, independent of client `body.eventId` skew.

### Kotlin (`tipmanna_tv_app-phase2-recovery`)

| File | Change |
|------|--------|
| `WalletPaidAnimationTrace.kt` | Structured log stages (no secrets) |
| `LiveTvPaidAnimationRecovery.kt` | Diagnostics; authoritative `hints.eventId` for recovery guard |
| `LiveTvTipAnimationHost.kt` | Guard receive/reject/trigger traces |
| `LiveTvPaidAnimationRecoveryTest.kt` | Guard regression with mismatched cart vs hint `eventId` |

---

## E. Verification (no real wallet debit)

| Check | Result |
|-------|--------|
| `payment-animation-recovery.service.spec.ts` (vitest) | **20/20 PASS** |
| `npx tsc --noEmit` (backend) | **PASS** |
| `LiveTvPaidAnimationRecoveryTest` + `LiveTvWalletCheckoutTest` | **PASS** (`:app:testStagingDebugUnitTest`) |
| `:app:assembleStagingDebug` | **BUILD SUCCESSFUL** |
| Staging E2E wallet animation | **NOT VERIFIED** (requires deploy + one controlled payment) |
| QR/card regression (automated) | **NOT RUN** full suite; QR recovery tests unchanged and still PASS in targeted run |

---

## F. Retest readiness

**READY FOR ONE CONTROLLED STAGING PAYMENT TEST** — **only after**:

1. Deploy backend **`48297cc0`** (or merge `staging/task11aq-wallet-recovery-eventid` to staging and deploy).
2. Install APK SHA **`53B8E3729B19E2F1B408DEB87D1BDF99D3177E56EEB280E907E764C9B9705073`**.

### One-payment procedure (operator)

1. Confirm staging health reports commit ≥ `48297cc0`.
2. Confirm `PAYER_ORIGIN_RECOVERY_API` (or equivalent) **not** disabled.
3. Note wallet balance before test.
4. Install 11AQ APK; clear-logcat:  
   `adb logcat -c && adb logcat -s WalletPaidAnimationTrace LiveTvPaidAnimationRecovery LiveTvTipAnimationHost`
5. Single wallet tip on LIVE TV (same item/amount as prior tests).
6. **Stop if debit succeeds** — do not repeat payment.
7. Expected log sequence (happy path):  
   `wallet_debit_success_received` → `wallet_animation_recovery_present` → `wallet_recovery_callback_invoked` → `wallet_recovery_claim_started` → `wallet_recovery_claim_result` → `tv_paid_animation_triggered` → (optional) `tv_paid_animation_rendered`
8. If `wallet_animation_recovery_missing`: capture response shape server-side (redact token); verify backend commit.
9. If `wallet_recovery_claim_failed`: note HTTP status in log; check backend delivery lag / auth.
10. If `tv_paid_animation_event_rejected`: compare `payloadEvent` vs `active` prefixes in log.
11. Reconcile balance after test.

**Do not close 11AQ** until animation visibly plays **or** a post-fix diagnostic pinpoints the next broken link with staging evidence.

---

## G. Outstanding risks

- Staging still on `d4c65ce` until TAYE deploy; one-payment test on current staging **will likely still miss** `animationRecovery` when event ids diverge.
- Kotlin changes are not committed to git; APK is the artifact of record for 11AQ.
- Pusher-only delivery for wallet not revalidated; acceptance path matches 11AH (**recovery claim**).
- Idempotent debit **replay** responses: verify stored idempotency JSON includes `animationRecovery` after fix (same code path as fresh debit — not separately tested against staging DB).
- Full QR regression suite not executed in this task window.

---

## H. Comparison to QR/card (reference)

| Step | QR/card (11AH) | Wallet (pre-fix) | Wallet (11AQ fix) |
|------|----------------|------------------|-------------------|
| Recovery session source | Checkout status / exchange | Debit JSON `animationRecovery` | Same, issued from **tx.eventId** |
| Client recovery entry | `onQrCheckoutPaid` | `onWalletCheckoutPaid` | Same + traces |
| Claim API | `claimAnimationRecovery` | Shared | Shared |
| TV render | `acceptAuthoritativePaymentAnimation` | Shared | Shared + hint `eventId` for guard |

**Earliest divergence (pre-fix):** Backend omitted `animationRecovery` on debit response before Kotlin could claim.
