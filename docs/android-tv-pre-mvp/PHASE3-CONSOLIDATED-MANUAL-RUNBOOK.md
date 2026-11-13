# Phase 3 — Consolidated manual test session (single runbook)

**Purpose:** One ordered session for all **remaining mandatory** Gate 3 manual evidence.
**Do not split** into separate doc-only updates after each step — fill correlation tables once at end.

**Prerequisites (before Session day):**

- `assembleStagingDebug` APK with Pusher staging alignment + QR fixes (PO-approved commit).
- Staging tipper credentials (vault).
- **Payments owner** confirms Stripe staging **`livemode=false`** for the planned test window (no agent initiates payment without this).
- Device: emulator **and** physical Android TV (for section C).
- `adb`, filtered logcat, screen capture.

---

## Session 0 — Automated baseline (no device)

| Step | Action | Pass |
|------|--------|------|
| 0.1 | `.\gradlew.bat :app:testStagingDebugUnitTest` | 0 failures |
| 0.2 | `.\gradlew.bat :app:assembleStagingDebug` | APK produced |

Record test count from `testStagingDebugUnitTest` XML.

---

## Session 1 — Install and Pusher smoke (≈15 min)

| Step | Action | Pass |
|------|--------|------|
| 1.1 | `adb install -r app\build\outputs\apk\staging\debug\app-staging-debug.apk` | Success |
| 1.2 | Clear logcat; start capture (`PusherService`, `LiveTvTipAnimation`, `WalletPaidAnimationTrace`) | Running |
| 1.3 | Sign in → **Watch LIVE TV** → playback | Video plays |
| 1.4 | Verify logs: `cluster=eu`, key `1492…b418`, `matchesEmbeddedKey=true` | Match |
| 1.5 | Connect + `Subscribed to global-live-tv` + `Bound tip-animation` | All present |
| 1.6 | Note `Active programme eventId=` | UUID copied |

*If 1.4–1.5 fail, **stop** — do not pay; fix Pusher build.*

**Already passed 2026-11-11:** 1.4–1.6 may be abbreviated to quick re-smoke only.

---

## Session 2 — Pusher stability soak (≈10 min) — **COMPLETE 2026-11-11**

| Step | Action | Pass |
|------|--------|------|
| 2.1 | Stay on LIVE; keep logcat | — |
| 2.2 | 10 min playback; watch for disconnect/DNS/subscribe errors | No errors |

**Recorded:** PASS — closes P3-DEF-003 for Gate 3.

---

## Session 3 — Passive payment animation E2E — **COMPLETE 2026-11-11**

**Gate criterion R17 / P3-DEF-002:** **PASSED** (user-reported).

| Step | Result |
|------|--------|
| 3.0 | Payments owner; **livemode=false** — **PASS** |
| 3.1–3.2 | LIVE; event `STG-P3-EVENT-ID` — **PASS** |
| 3.3–3.4 | Test payment success; txn `STG-P3-TXN-ID` — **PASS** |
| 3.5–3.7 | Received → accepted → queued; txn/event match — **PASS** |
| 3.6 | Visible overlay — **PASS** |
| 3.8 | No TV wallet/debit — **PASS** |

**Follow-up:** attach log + recording paths to `test-evidence/session3-passive-e2e/` (not located in workspace at doc update).

---

## Session 4 — Negatives (minimize extra payments)

| Step | Scenario | How | Pass |
|------|----------|-----|------|
| 4.1 | Failed/cancelled payment (A9) | Decline/cancel **one** web attempt — **no** success charge | No TV animation |
| 4.2 | Wrong programme (N2) | **Prefer skip** if it needs extra success payment; rely on `TipAnimationGuardTest` + optional wrong-event inject | UT PASS documented |

Duplicate (A6): **no second payment** unless backend can replay same `transactionId`; cite `TipAnimationGuardTest` for dedup.

---

## Session 5 — Financial safety (during 3.x)

While Session 3 logcat is running:

- Confirm no tip drawer / checkout UI on TV (Pre-MVP).
- Same log file used for A8 evidence.

---

## Session 6 — Physical Android TV (mandatory C1–C8)

**Requires hardware per checklist S1.**

| Block | Cases | Pass |
|-------|-------|------|
| Navigation | C1–C3, C7–C8 | Hub, back, auth |
| Payment exclusion | C4–C5 | No wallet/checkout exposure |
| LIVE smoke | C6, C10 | Playback + QR |
| Optional | C9 | Session expiry if policy defined |

**PO 2026-11-11:** **ACCEPTED RISK (D1)** — emulator only for internal Gate 3; physical C1–C8 required before Play closed testing (P4-016).

---

## Session 7 — Closeout (same day)

1. Fill correlation table in `PHASE3-PASSIVE-ANIMATION-E2E.md`.
2. Update `PHASE3-FINAL-ACCEPTANCE-MATRIX.md` statuses.
3. Update `GATE3-EXECUTION-REPORT.md` → PASS only if R17 + C1–C8 satisfied.
4. Update `PHASE3-DEFECT-REGISTER.md` (close P3-DEF-002 on evidence).
5. **One commit** (or PO-approved split): code + docs + evidence paths.

---

## PowerShell logcat (reuse)

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools-2\adb.exe"
& $adb -s <device-serial> logcat -c
& $adb -s <device-serial> logcat -v time `
  PusherService:D `
  LiveTvTipAnimation:D `
  WalletPaidAnimationTrace:D `
  *:S
```

Record: `Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'` on host at payment time.
