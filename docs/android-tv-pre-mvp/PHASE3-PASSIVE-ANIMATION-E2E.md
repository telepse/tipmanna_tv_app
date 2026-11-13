# Phase 3 — Passive web payment → TV animation (E2E)

**Staging only · test payments only · no TV checkout**

## Environment checklist

| Item | Expected |
|------|----------|
| Branch | `feature/android-tv-pre-mvp` (uncommitted QR + LIVE QR fixes) |
| APK | `app/build/outputs/apk/staging/debug/app-staging-debug.apk` (`com.example.telepse.staging`) |
| API | `https://tipmanna-staging.onrender.com` |
| Pusher channel | Public `global-live-tv`, event `tip-animation` (no private auth for this path) |
| Cluster | **`eu`** on `stagingDebug` (`BuildConfig.PUSHER_CLUSTER`; host `ws-eu.pusher.com` in logs) |

## ADB setup (PowerShell)

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools-2\adb.exe"
& $adb -s emulator-5554 logcat -c
& $adb -s emulator-5554 logcat -v time `
  PusherService:D `
  LiveTvTipAnimation:D `
  WalletPaidAnimationTrace:D `
  *:S
```

Save to file for evidence:

```powershell
& $adb -s emulator-5554 logcat -v time -f /sdcard/tv-animation-e2e.log `
  PusherService:D LiveTvTipAnimation:D WalletPaidAnimationTrace:D *:S
# After test: adb pull /sdcard/tv-animation-e2e.log .
```

## Procedure — successful path (A1)

1. TV: sign in → **Watch LIVE TV** → wait for playback (not Error/Empty).
2. Log: note **`Active programme eventId=<UUID>`** (`LiveTvTipAnimation`).
3. Log: confirm **`✅ Subscribed to global-live-tv`** and **`✅ Pusher connection established`** (`PusherService`).
4. Web (staging): complete **one** successful **test** tip/payment for **that exact `eventId`** (same programme on air).
5. Record **`transactionId`** from web UI, network tab, or staging admin (do not log card data).
6. Within ~60s on TV log, expect **passive** lines (not recovery):
   - `WalletPaidAnimationTrace`: `stage=tv_paid_animation_event_received` … `detail=channel=pusher`
   - `LiveTvTipAnimation`: **`tip-animation accepted`** … `tx=<first 8 of txn>`
7. Visually confirm tip animation overlay on TV; then `Animation removed` when complete.
8. **Must NOT appear** for passive proof alone: `recovery animation accepted`, `wallet_debit`, `submitWalletCheckout`.

## Correlation table (Session 3 — 2026-11-11)

| Field | Source |
|-------|--------|
| Active TV `eventId` | `STG-P3-EVENT-ID` |
| Web payment `transactionId` | `STG-P3-TXN-ID` |
| Stripe livemode | `false` (payments owner) |
| TV passive path | Received → `tv_paid_animation_event_received` → `tip-animation accepted` → overlay |
| Match? | **Yes** |
| Artifacts | Log + recording saved — attach under `test-evidence/session3-passive-e2e/` |

## Negative tests

| ID | Action | Expected |
|----|--------|----------|
| N1 | Cancel/fail test payment on web | No `tip-animation accepted`; no overlay |
| N2 | Tip different `eventId` than TV programme | `tip-animation rejected by guard` or `tv_paid_animation_event_rejected` |
| N3 | (If tooling allows) replay same txn | Second event rejected (guard duplicate `transactionId`) |

## Financial safety (TV)

Pre-MVP: no tip drawer. During test, grep log for `WalletRepository`, `walletDebit`, `LiveTvPaidAnimationRecovery` **recovery** paths — none should run for web-only passive test.

## P3-DEF-003 (DNS)

If `PUSHER connection error` mentions DNS for `ws-us3.pusher.com`: note timestamp vs payment time. If payment succeeds while connected and `tip-animation accepted` fires, DNS is **non-blocking** for that run. If payment during outage with no event, retry on stable network — do not change app config.

## Gate

**P3-DEF-002** **CLOSED** (2026-11-11): correlation below + log at `test-evidence/session3-passive-e2e/`. Overlay visibility = PO/QA attestation; recording not in repo.
