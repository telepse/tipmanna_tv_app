# Phase 3 — Defect investigation (no code changes)

**Branch:** `feature/android-tv-pre-mvp` (investigation + QR fix, uncommitted per PO)
**Gate 3:** **PASS WITH ACCEPTED RISKS** (2026-11-11) — see `PHASE3-GATE3-DECISION-SHEET.md`

### QR fix (PO-approved, 2026-11-10)

- Staging `SHARE_CONTENT_ORIGIN`: `https://tipmanna-fj3ffgvfq-test01.vercel.app`
- Production `SHARE_CONTENT_ORIGIN`: `https://tipmanna.tv`
- Wired via Gradle `BuildConfig` → `PreMvpFeatureFlags.shareContentOrigin` → `TipmannaShareUrl` / `VideoShareQrOverlay` (LIVE + Explore unchanged callers).
- **P3-DEF-001:** resolved at code level; physical scan not re-run in implementation session.

### LIVE QR visibility (P3-DEF-004, emulator QA 2026-11-10)

**Symptom:** Explore QR PASS; LIVE shows no QR overlay.

**Root cause (code):** `VideoShareQrOverlay` composes nothing when `TipmannaShareUrl.buildContentShareUrl` returns `null`. LIVE passes IDs from `resolveLiveTvShareIds`, which sets `channelId = item.id`. Staging schedule API sets top-level `id` to virtual row keys (`virt:{sourceId}:{position}:{start}`) per `mapPlaylistRowToClientPayload` / `axisSegsToPlaylistRows` in backend — **not** UUIDs. `isLikelyUuid` rejects → no QR. Web TV uses `current?.channel?.id` for `?v=` (`tv-looper.tsx`); Android `TvScheduleChannel` model omits `id`, so the EventChannel UUID is dropped on deserialize.

**Not root cause:** Z-order (overlay never composed); `playerReady` gate (Explore-only); payment UI.

**Fix (implemented, uncommitted):** `TvScheduleChannel.id` added; `resolveLiveTvShareIds` prefers `channel.id`, UUID fallback to `item.id`; `LiveTvShareIdsTest` covers virtual row + channel UUID. **Manual LIVE QR verification pending.**

---

## Part A — QR environment mismatch

### 1. TV share URL construction

| Piece | Location |
|-------|----------|
| Origin default | `PreMvpFeatureFlags.shareContentOrigin` → `"https://tipmanna.tv"` |
| Builder | `share/TipmannaShareUrl.kt` — path `/content/{eventId}?v={channelId}` |
| UI | `ui/share/VideoShareQrOverlay.kt` → `TipmannaShareUrl.buildContentShareUrl(eventId, channelId)` |
| Callers | `LiveTvFoundationScreen.kt`, `ExplorePlaybackScreen.kt` |
| LIVE IDs | `share/LiveTvShareIds.kt` — schedule row `id` + programme `eventId` |

There is **no** flavor-specific share origin: `stagingDebug` uses the same production hostname as `prodDebug`.

### 2. Web contract

| Piece | Location |
|-------|----------|
| Path | `frontend-wt-tv-checkout-8b/src/lib/public-video-url.ts` — `buildContentSharePath` / `buildContentShareUrl` |
| Absolute URL | `getPublicAbsoluteUrl()` → `getPrimaryOrigin()` → default **`https://tipmanna.tv`** (`site-config.ts`) |
| Page route | `src/app/content/[eventId]/page.tsx` — loads channel list via server data layer bound to deployment’s `NEXT_PUBLIC_BACKEND_API_URL` |

### 3. Staging frontend domain (verified in repo)

- **Canonical production** share origin: `tipmanna.tv` (PO-approved Pre-MVP).
- **Staging API** (TV + typical staging web): `https://tipmanna-staging.onrender.com` (`app/build.gradle.kts` staging flavor).
- **No committed stable staging “tipmanna.tv-equivalent”** in the TV repo. Internal evidence references **Vercel preview** hosts (e.g. `frontend-wt-task9-staging-integration-*.vercel.app`) with staging backend — **per-deployment**, not a fixed public name.

**Conclusion:** We **cannot** assert a single staging frontend URL without PO/DevOps confirmation. The `/content/{eventId}?v={channelId}` **route exists** on the Next.js app; staging behaviour requires a deployment whose **`NEXT_PUBLIC_BACKEND_API_URL`** points at staging.

### 4. Why “Invalid video link” on scan

1. QR encodes **production** origin + **staging-issued** UUIDs.
2. Production web loads content from **production** API.
3. Staging `eventId` / `channelId` are absent or empty URL on prod → `videoId` missing.
4. UI: `thumbnail-player.tsx` / `video-player.tsx` → `<p>Invalid video link</p>`.

**Content IDs** are normal UUIDs; **environment** is determined by which database issued them, not by ID format.

### 5. LIVE QR

Same code path as Explore → **same mismatch** on staging TV.

### 6. Proposed environment-aware sharing (approval required)

**Prerequisite (must verify before implement):**

1. PO names the **official staging frontend origin** (HTTPS) used for mobile browser QA.
2. On a phone, open
   `{STAGING_ORIGIN}/content/{stagingEventId}?v={stagingChannelId}`
   and confirm the **same video** as on TV (no `/c/pay/`).
3. Confirm production build still uses `https://tipmanna.tv`.

**Minimal TV-only fix (after verification):**

- Add `buildConfigField` `SHARE_CONTENT_ORIGIN` on `staging` flavor (verified URL) and keep `https://tipmanna.tv` on `prod`.
- Wire `TipmannaShareUrl` / `PreMvpFeatureFlags` to `BuildConfig` (single source).
- Extend `TipmannaShareUrlTest` for staging vs prod origins.

**Do not** invent a hostname. **Do not** point staging QR at production.

**Alternative (cross-repo):** Backend `tv-app` bootstrap returns `publicShareOrigin` — needs backend change + approval.

### 7. Regression tests (post-fix)

- Unit: `TipmannaShareUrlTest` per flavor origin.
- Manual: staging TV → staging URL; prod flavor → `tipmanna.tv`.
- Never `/c/pay/` in encoded string.

---

## Part B — Payment animation test

### 1. Staging build configuration

| Config | Value |
|--------|--------|
| API | `BuildConfig.API_BASE_URL` = `https://tipmanna-staging.onrender.com` |
| Pusher key/cluster (embedded fallback) | `PusherConfig.API_KEY`, `CLUSTER = "us3"` |
| Bootstrap override | Optional `pusherKey` / `pusherCluster` from tv-app bootstrap |
| LIVE animation channel | **Public** `global-live-tv`, event `tip-animation` (`PusherService.subscribeToGlobalLiveTv`) |
| Auth endpoint for **private** channels | `POST /api/v1/tv-app/pusher/auth` (staging base URL) — not used for `global-live-tv` |

### 2. Backend emit path (authoritative)

`tip-payment-side-effects.service.ts` → `pusher.trigger("global-live-tv", "tip-animation", {…})` on successful payment (staging server uses same channel names when triggered).

### 3. TV receive path (passive)

`LiveTvTipAnimationHost` → `pusherService.observeTipAnimation()` → `TipAnimationGuard.shouldAccept(payload, activeProgrammeEventId)` → overlay queue.

**Not passive evidence:** `LiveTvTipAnimationHost.acceptAuthoritativePaymentAnimation` → log **`recovery animation accepted`** — invoked only from `LiveTvPaidAnimationRecovery` after **TV wallet/QR checkout** API recovery (`LiveTvFoundationViewModel` coordinator). Pre-MVP hides checkout UX but coordinator remains wired.

### 4. Programme matching

`TipAnimationGuard` rejects if `payload.eventId != activeEventId`, stale `serverTs`, duplicate `transactionId` / `bundleId`, or non-increasing `seq`.

### 5. Financial side effects on display

Receiving Pusher animation only enqueues `AnimationTrigger` for overlay. Wallet debit / recovery HTTP runs only through `LiveTvTipCoordinator` checkout callbacks (not from passive Pusher handler).

### 6. Pusher DNS (`ws-us3.pusher.com`)

- Matches embedded cluster **`us3`**.
- DNS errors are **environmental** (emulator DNS, corporate network, transient).
- Logs showing **subscribe succeeded** and **CONNECTED** mean client config is plausibly correct; delivery during outages is **unverified**.

### 7. Repeatable staging procedure (correlate txn → TV)

**Before tip**

1. `adb logcat -c`
2. Filter: `LiveTvTipAnimation`, `PusherService`, `WalletPaidAnimationTrace`
3. TV: sign in → LIVE → wait for playback.
4. Record `Active programme eventId=` from `LiveTvTipAnimation` (must match tip target on web).

**Tip (web only)**

5. Staging web: tip **same `eventId`** with authorised **test payment** only.
6. Record **`transactionId`** from web network response, receipt UI, or staging admin (no card data in notes).

**TV observation (60s)**

7. Expect log: **`tip-animation accepted`** (not `recovery animation accepted`).
8. Expect `transactionId=` prefix matching step 6.
9. Expect visible tip animation overlay.
10. Confirm **no** log lines for wallet debit submit on TV.

**Duplicate test**

11. Replay same payload or double-fire if tooling allows → second event **`tip-animation rejected by guard`** or no second overlay.

**Negative**

12. Failed/cancelled payment → no `tip-animation accepted`, no overlay.

**Disconnect (policy check)**

13. Brief offline → reconnect → **no** claim that missed events were “recovered” unless Pusher delivers a **new** authoritative event (no offline replay API).

---

## Part C — Files that would change (QR fix only, if approved)

| File | Change |
|------|--------|
| `app/build.gradle.kts` | `SHARE_CONTENT_ORIGIN` per flavor |
| `premvp/PreMvpFeatureFlags.kt` | Read from `BuildConfig` |
| `share/TipmannaShareUrl.kt` | Default origin from `BuildConfig` |
| `TipmannaShareUrlTest.kt` | Staging origin case |
| Docs | Gate 3 matrix after QA |

No backend/web/financial changes in minimal TV-only fix.

---

## Part D — Gate 3 status (closeout 2026-11-11)

**PASS WITH ACCEPTED RISKS** — P3-DEF-001/002/003/004 resolved; physical TV (D1), manual A9 (D2), runtime replay (D3) accepted per Authorized Product Owner. See `PHASE3-GATE3-DECISION-SHEET.md`.
