# Android TV MVP — business model audit (first deliverable)

**Date:** 2026-11-10  
**Baseline verified:** `origin/main` @ `281fe26be890bbea435a42edf0235f40538d5d12` (unchanged)  
**Accepted tip commit on main:** `c56b09d38ee7b9271b069e56cbec810daf7578cf` (ancestor of main)  
**Implementation branch:** `feature/android-tv-mvp-focused` (from `origin/main`)

---

## 1. Verified Tipmanna business behavior (backend + Kotlin evidence)

| Domain | Verified behavior (source) | TV client usage |
|--------|---------------------------|-----------------|
| **Tipper auth** | `POST /api/v1/auth/login-tipper`, `verify-code` → JWT | `AuthRepository`, `SessionStore` (encrypted prefs) |
| **Wallet ledger** | `GET /api/v1/wallet`, `GET /api/v1/wallet/transactions` | Hub visibility + LIVE prerequisite checks |
| **Wallet debit** | `POST /api/v1/wallet/debit` — Zod schema (`walletDebitSchema`: PIN, amount, eventId, characters[], optional quote/surface/idempotency metadata); `debitTipperWalletForTip` creates **succeeded** `Transaction`, ledger, animation delivery (server) | **LIVE only** (`WalletRepository`, `LiveTvTipCoordinator`) — **11AQ preserved** |
| **Wallet funding** | Top-up via Stripe PI (`wallet_topup` metadata), webhook/reconcile — **not exposed in TV `WalletRepository`** | External Tipmanna channels only (PO directive) |
| **Tip quote / limits** | `POST /api/v1/tip-quote`, `GET tip-config/{eventId}/active` | LIVE cart planning (`LiveTvTipQuotePlanner`) — server determines amounts |
| **Character catalog** | `GET /api/v1/events/{eventId}/character-catalog` | LIVE programme binding |
| **QR / card** | `POST /api/v1/tv-tip-checkout/sessions`, status poll, cancel | `LiveTvQrCheckoutFlow` — authoritative **paid** from poll, not client guess |
| **Paid animation** | Pusher + `payments/animation-recovery` (claim uses backend canonical payload) | `LiveTvPaidAnimationRecovery` + `LiveTvTipAnimationHost` — **11AQ staging PASS** |
| **Fees / payout** | `recordEventTipLedger`, revenue ledger, Stripe paths in `tip-payment-side-effects` / webhooks — **not duplicated in Kotlin** | **Not verified in this audit:** tipper-facing fee copy; assume server ledger is truth |

**Not invented:** No TV-only balances, fees, or payout rules in Kotlin MVP changes.

---

## 2. Media surfaces vs Android TV (comparison)

| Capability | Other Tipmanna surfaces (inferred from backend) | Android TV (current) |
|------------|-----------------------------------------------|----------------------|
| Auth | Shared tipper JWT | Same endpoints |
| Tipping | Wallet debit / Stripe checkout | **LIVE TV** wallet + QR only (MVP) |
| Funding | Web / Stripe top-up | **Not on TV** (by design) |
| Animations | Pusher + recovery | LIVE recovery path **verified staging**; Screen Display still Pusher-only (deferred) |
| Creator/context | eventId on transactions | LIVE schedule → programme `eventId` |

---

## 3. APIs reused by TV MVP (no new paths)

See `WalletRepository`, `AuthRepository`, `TvScheduleRepository`, `TvEventBootstrapRepository` (deferred screens only), `TvAnalyticsClient` (`POST /api/v1/tv/analytics`).

---

## 4. Gaps for TAYE / PO

| Gap | Owner |
|-----|--------|
| Production `tipmanna-live` parity with staging **48297cc0** wallet recovery | TAYE |
| Confirm tip fee / benefit messaging for TV copy | Product |
| Analytics ingest monitoring on staging/live | TAYE/Ops |
| Wallet top-up operator runbook for TV testers | Product/Ops |

---

## 5. TV implementation (this branch)

- `TvMvpNavigation` — hub limited to LIVE TV, wallet visibility, sign-in/out
- Wallet hub UI — balance + transactions only (no hub tipping)
- Screen Display auto-restore disabled for MVP
- **No changes** to `LiveTvPaidAnimationRecovery` / 11AQ debit path

---

## 6. Acceptance evidence template (section 7)

| Journey | Status (pre-MVP branch) |
|---------|-------------------------|
| Cold launch → LIVE TV | **NOT TESTED** on this branch |
| Sign-in / session | **NOT TESTED** |
| LIVE wallet tip + rules | **PASS** staging @ `c56b09d` + backend `48297cc0` (prior 11AQ) |
| Wallet visibility | **NOT TESTED** after hub UI change |
| QR + animation | **PASS** staging 11AH (prior) |
| … | Re-run full matrix after MVP branch merge |

Unit tests: `TvMvpNavigationTest` + existing wallet/recovery suite — run on CI locally.
