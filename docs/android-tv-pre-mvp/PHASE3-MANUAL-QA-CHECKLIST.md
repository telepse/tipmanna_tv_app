# Phase 3 — Manual QA checklist (Gate 3 essential evidence)

**Applies to build:** `feature/android-tv-pre-mvp` @ `cdb73e2` (Phase 2) or later on same branch
**Flavor for QA:** `stagingDebug` → package `com.example.telepse.staging`, API `https://tipmanna-staging.onrender.com`
**Share QR domain (approved production):** `https://tipmanna.tv/content/{eventId}?v={channelId}` (not checkout)
**Staging QR (known defect P3-DEF-001):** Current APK still encodes **production** origin while API is staging — verify fixes against **PO-confirmed staging frontend origin** before pass.

**Do not use:** real card charges, production customer data, or TV-initiated wallet debit as pass criteria for passive animation.

**Consolidated session:** run all remaining manual tests in **`PHASE3-CONSOLIDATED-MANUAL-RUNBOOK.md`** (single day).
**Definitive matrix:** `PHASE3-FINAL-ACCEPTANCE-MATRIX.md`.
**Gate 3 PO decisions:** `PHASE3-GATE3-DECISION-SHEET.md`.

---

## Evidence standards

For each case, capture **at minimum**:

| Artefact | Format |
|----------|--------|
| Test ID + date + tester initials | Table row in `GATE3-EXECUTION-REPORT.md` § Manual acceptance matrix |
| Device / browser versions | Text |
| Screenshots or short screen recording | PNG / MP4 |
| TV log excerpt (if animation / Pusher) | `adb logcat` filtered: `LiveTvTipAnimation`, `PusherService`, `TipAnimationGuard` |
| Web payment reference | Staging transaction id or internal test reference (**no** card numbers) |
| QR payload | Photo of TV QR + decoded URL string (any QR reader app) |

**Pass:** Expected result observed with evidence attached.
**Fail:** Expected not met; file defect (see template at bottom).
**Blocked:** Prerequisite missing (list in § Setup).
**Not run:** Scheduled but not attempted.

---

## QA setup (required — no secrets in reports)

| # | Requirement | Owner | Notes |
|---|-------------|-------|-------|
| S1 | Physical **Android TV** device (Google TV, Fire TV, or STB with official Android TV / Google TV) | QA | Emulator **not** valid for Gate 3 device sign-off |
| S2 | **stagingDebug** APK: `.\gradlew.bat :app:assembleStagingDebug` → `app/build/outputs/apk/staging/debug/` | Dev/QA | Sideload via `adb install` or USB |
| S3 | `adb` access to TV (network debugging or USB) | QA | For logcat |
| S4 | **Staging tipper account** (email/password held in team vault — **do not paste in reports**) | PO/QA | TV Sign in (tipper) on hub |
| S5 | **Staging web** session (same or second tipper) on desktop browser | QA | Staging web origin aligned with staging API |
| S6 | **Test payment method** enabled on staging only (Stripe test mode / existing internal test tip flow) | Finance/Backend | No real money |
| S7 | LIVE programme on air with **known `eventId`** matching schedule row used for tip on web | Content/QA | Confirm IDs in schedule API or on-screen DEBUG line if enabled |
| S8 | **Phone** with Tipmanna mobile app **or** mobile browser logged into Tipmanna | QA | For QR tests |
| S9 | Network: TV and phone reach staging API / Pusher; TV can play YouTube embeds | QA | |
| S10 | Optional: Pusher debug dashboard access (read-only) to confirm `tip-animation` on `global-live-tv` | Backend | Channel/event names in app: `PusherConfig.GLOBAL_LIVE_TV_CHANNEL`, `TIP_ANIMATION_EVENT` |

**Staging configuration reference (non-secret):**

- TV API base: `BuildConfig.API_BASE_URL` → `https://tipmanna-staging.onrender.com`
- Staging Pusher (public): `BuildConfig.PUSHER_CLUSTER` → `eu`; masked key `1492…b418` (must match staging backend / tv-app bootstrap)
- Prod Pusher (public): `us3`; masked key `09e0…3d77` — **not** used on `stagingDebug`
- Pusher: private channels via TV tipper session (`PusherHttpAuthorizer`); `global-live-tv` is public (no auth)
- Animation filter: `LiveTvTipAnimationHost` accepts only when payload `eventId` matches **active LIVE programme** and `TipAnimationGuard` accepts (dedup / stale rejection)

---

## A-pre. Staging Pusher LIVE runtime (pre–passive-animation)

**Goal:** Confirm staging TV uses the **same Pusher application** as staging backend before any payment E2E.
**Does not satisfy:** A5 (overlay), A8 (`tip-animation accepted`), or P3-DEF-002 closure.

| ID | Steps | Expected result | Evidence (2026-11-11) |
|----|--------|-----------------|----------------------|
| P1 | Install `stagingDebug`; filter `PusherService` / `LiveTvTipAnimation`. Open **Watch LIVE TV**. | Credential log: `cluster=eu`, masked key `1492…b418`, `matchesEmbeddedKey=true` | **PASS** |
| P2 | Same session | `✅ Pusher connection established` | **PASS** (two app starts) |
| P3 | Same session | `✅ Subscribed to global-live-tv` | **PASS** |
| P4 | Same session | `✅ Bound tip-animation on global-live-tv` | **PASS** |
| P5 | Wait for playback | `Active programme eventId=` non-empty UUID; video playing | **PASS** — `STG-P3-EVENT-ID` |
| P6 | 10 min LIVE playback; monitor Pusher logs | No disconnect / DNS / subscription errors on `eu` | **PASS** (2026-11-11 emulator) — P3-DEF-003 closed for Gate 3 |
| P7 | Payment / animation | `Received tip-animation` → `tip-animation accepted` → overlay | **PASS** (Session 3, txn `STG-P3-TXN-ID`) |

**Note:** Empty `activeEventId` on first `Observing tip-animation` line is expected until programme bind; use the later `Active programme eventId=` line for A3/A4 correlation.

---

## A. Web → passive TV animation

**Goal:** Successful **web-originated** staging payment → TV on LIVE shows animation **once**; TV performs **no** debit/ledger action.

| ID | Steps | Expected result | Evidence |
|----|--------|-----------------|----------|
| A1 | Build/install `stagingDebug` on physical TV. Cold launch. | Hub shows LIVE, Explore, Sign in only | Screenshot |
| A2 | Sign in as staging tipper. Open **Watch LIVE TV**. Wait for playback. | Video plays; **no** Tips chip, drawer, or payment QR | Video/photo |
| A3 | Note active programme: schedule title + capture `eventId` (logcat `Active programme eventId=` or schedule API). | Non-empty UUID matches programme on air | Log/API note |
| A4 | On **web** (different session OK): tip the **same programme `eventId`** using **test payment** until backend records success. | Web shows success; no real charge | Web screenshot + txn id |
| A5 | Observe TV within ~30s. | Luxurious/tip animation overlay plays on TV | Recording |
| A6 | Repeat tip with **same** transaction id or trigger duplicate Pusher delivery if QA tooling allows. | **One** animation per authoritative event (`TipAnimationGuard`) | Log: `tv_paid_animation_event_rejected` or single `accepted` |
| A7 | During/after animation, confirm TV UI: no wallet drawer, no checkout, no balance prompts. | No payment UI | Screenshot |
| A8 | Capture logcat during A4–A5. | Lines like `tip-animation accepted`; **no** `submitWalletCheckout` / wallet debit success on TV | Log file |
| A9 | **Negative:** Attempt failed/cancelled web payment. | TV shows **no** success animation | Web + TV recording |
| A10 | Disconnect TV network 60s, reconnect, **do not** tip again. | Subscription restores; **no** fabricated “recovery” animation for missed events while offline | Log: Pusher reconnect; no false overlay |

**Explicit fail criteria:** Animation for wrong `eventId`; double animation for single payment; TV wallet debit API call from TV app during test.

**Not valid evidence:** `LiveTvPaidAnimationRecovery` after **TV-initiated** checkout only.

---

## B. QR → mobile video (LIVE + Explore)

**Goal:** Scanning TV QR opens the **same** Tipmanna content on mobile; never `/c/pay/`.

| ID | Steps | Expected result | Evidence |
|----|--------|-----------------|----------|
| B1 | On LIVE (playing, valid UUIDs): locate top-end QR. | Message exactly: **Watch this video on your mobile** | Photo |
| B2 | Decode QR (reader app). | URL `https://tipmanna.tv/content/{eventId}?v={channelId}`; both UUIDs; **no** `/c/pay/` | Text paste |
| B3 | Scan with phone camera / Tipmanna app. | Opens same video/channel in Explore/content surface | Recording |
| B4 | Compare title/thumbnail/YouTube id to TV playback. | Same content | Side-by-side note |
| B5 | Sign out on mobile (or use incognito) and rescan. | Document login redirect vs direct play (expected: may require sign-in for restricted content) | Note only |
| B6 | Open **Explore** on TV, play item with valid IDs. | Share QR appears when player ready | Photo |
| B7 | Repeat B2–B4 for Explore QR. | Same as LIVE | Evidence set |
| B8 | If schedule/item lacks UUIDs: confirm **no** QR shown (LIVE) or card not openable (Explore). | No misleading generic URL | Screenshot |

**Pass:** B2–B4 and B7 satisfied for **one LIVE** and **one Explore** item each.

---

## C. Physical Android TV (navigation + payment exclusion)

| ID | Steps | Expected result | Evidence |
|----|--------|-----------------|----------|
| C1 | Cold launch after install. | Lands on **Tipmanna TV** hub | Photo |
| C2 | D-pad Up/Down on hub; OK on each row. | Focus visible; LIVE and Explore open after auth | Recording |
| C3 | Back from LIVE / Explore. | Returns to hub (not Wallet / Screen Display) | Recording |
| C4 | On LIVE: traverse control row (Back, Schedule, Refresh, Play, …). | **No** Tips control when Pre-MVP flag off | Photo |
| C5 | Search UI for “Wallet”, “Screen Display”, “Checkout”, “Tips (” | None exposed from hub or LIVE/Explore | Checklist sign-off |
| C6 | Resume from sleep on hub. | Stays on hub (no Screen Display auto-restore) | Recording |
| C7 | Sign out on hub; try LIVE. | Redirect to Sign in | Recording |
| C8 | Sign in; session persists across app restart. | Still signed in | Recording |
| C9 | Optional: force session expiry per staging policy. | Graceful re-auth; no crash | Note + log |
| C10 | 10+ min LIVE playback. | Stable playback; QR remains when IDs valid | Note |

Record: **device model**, **Android version**, **build** `versionName` + git SHA flashed on QA sheet.

---

## Defect template (if any manual case fails)

| Field | Value |
|-------|--------|
| Defect ID | P3-DEF-### |
| Severity | P0 / P1 / P2 |
| Test ID | e.g. A5 |
| Steps | … |
| Expected | … |
| Actual | … |
| Evidence | link/file |
| Root cause | if known |
| Fix scope | Android TV only unless PO approves other repos |

---

## Gate 3 essential acceptance (all must be **PASSED** with evidence)

- [x] **A-pre P1–P6** — Staging Pusher LIVE runtime + 10 min soak — 2026-11-11 emulator
- [x] **A5** — Web test payment → passive TV animation — Session 3 **PASS** (`test-evidence/session3-passive-e2e/`)
- [x] **A6** — No duplicate animation — UT **PASS**; runtime replay **NOT RUN — ACCEPTED RISK (D3)** 2026-11-11
- [x] **A8** — No TV-side financial action — Session 3 **PASS**
- [x] **B3 + B7** — QR LIVE + Explore → exact mobile video (2026-11-10 staging)
- [x] **C1–C8** — Physical TV — **NOT RUN — ACCEPTED RISK (D1)** 2026-11-11 (emulator evidence only; not physical hardware)

**Gate 3:** **PASS WITH ACCEPTED RISKS** — Authorized Product Owner **2026-11-11** (`PHASE3-GATE3-DECISION-SHEET.md`).

**Executor:** Authorized Product Owner (risk acceptances D1–D3) **Date:** 2026-11-11
