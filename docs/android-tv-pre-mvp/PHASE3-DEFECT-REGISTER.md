# Phase 3 — Defect register (emulator QA round 1)

| ID | Severity | Feature | Status | Summary |
|----|----------|---------|--------|---------|
| P3-DEF-001 | **P0** | QR share (LIVE + Explore) | **RESOLVED (code)** | BuildConfig `SHARE_CONTENT_ORIGIN` per flavor; **manual phone scan (B1–B8) still required**. |
| P3-DEF-002 | **P0** | Passive web → TV animation | **RESOLVED** | Session 3 summary `test-evidence/session3-passive-e2e/SESSION3-SANITIZED-LOG-SUMMARY.md`; txn `STG-P3-TXN-ID`. |
| P3-DEF-003 | **P2** | Pusher connectivity | **RESOLVED** | 10 min LIVE soak on `eu` (2026-11-11): no disconnect, DNS, or listener errors. |
| P3-DEF-004 | **P0** | LIVE QR visibility | **FIXED (code)** | `channel.id` for share; unit tests added — **manual LIVE QR scan still pending**. |

---

## P3-DEF-001 — Staging QR → production web

| Field | Detail |
|-------|--------|
| **Reproduction** | Install `stagingDebug`; play Explore (or LIVE with valid UUIDs); scan QR on phone. |
| **Expected** | Same staging video opens on mobile (`/content/{eventId}?v={channelId}` on **staging** web). |
| **Actual** | QR opens `https://tipmanna.tv/content/...` (or `www` redirect); page loads prod frontend → prod API → channel missing / no `videoId` → *Invalid video link* (`thumbnail-player.tsx`). |
| **Root cause** | `PreMvpFeatureFlags.shareContentOrigin = "https://tipmanna.tv"` for **all** builds; TV data from `https://tipmanna-staging.onrender.com`. UUIDs exist only in **staging** DB. |
| **Affects LIVE?** | **Yes** — same `VideoShareQrOverlay` → `TipmannaShareUrl.buildContentShareUrl`. |
| **Recommended fix** | Environment-aware share origin on **`staging`** flavor only (after PO verifies staging web hostname + `/content` route). See `PHASE3-INVESTIGATION-REPORT.md` § QR. |
| **Files (if approved)** | `app/build.gradle.kts`, `PreMvpFeatureFlags.kt` or `BuildConfig`, `TipmannaShareUrl.kt`, `TipmannaShareUrlTest.kt` |

---

## P3-DEF-002 — Animation E2E not proven

| Field | Detail |
|-------|--------|
| **Reproduction** | Staging web test payment while TV on LIVE with matching `eventId`. |
| **Expected** | Log `tip-animation accepted` + visible overlay; **no** TV wallet debit. |
| **Actual** | Pre-fix: wrong Pusher app (`17455424-…`). Post-fix Session 3: passive accept + visible overlay; txn `STG-P3-TXN-ID`; event `STG-P3-EVENT-ID`. |
| **Root cause (confirmed)** | Staging backend Pusher app (`eu` / `1492…b418`) ≠ pre-fix TV embedded app (`us3` / `09e0…3d77`). Fixed via flavor `BuildConfig`. |
| **Resolution** | **2026-11-11** Session 3 E2E **PASS** — log verified in repo; screen recording optional. |

---

## P3-DEF-003 — Pusher DNS

| Field | Detail |
|-------|--------|
| **Reproduction** | Run LIVE on emulator; observe logcat. |
| **Expected** | Stable WebSocket to staging Pusher `eu` cluster after identity alignment. |
| **Actual** | 2026-11-11: connect + `global-live-tv` subscribe + `tip-animation` bind **PASS** (two cold starts). Prior runs showed intermittent `ws-us3.pusher.com` DNS when TV used wrong app. |
| **Root cause (likely)** | Emulator/host DNS or transient network; separate from resolved app-identity mismatch. |
| **Resolution** | 2026-11-11: 10 min emulator soak **PASS** — defect **closed** for Gate 3. Ongoing prod monitoring → Phase 6. |
