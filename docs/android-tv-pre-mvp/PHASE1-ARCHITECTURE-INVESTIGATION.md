# Phase 1 — Architecture investigation (baseline `281fe26`)

Evidence paths are relative to `tipmanna_tv_app` unless noted.

## LIVE TV

### Existing implementation

| Component | Path | Role |
|-----------|------|------|
| Entry | `MainActivity.kt` → `Screen.LiveTvFoundation` | Auth-gated navigation (`requireAuth`) |
| Screen | `ui/screen/LiveTvFoundationScreen.kt` | WebView YouTube playback, schedule panel, tip drawer, animation overlay |
| ViewModel | `ui/viewmodel/LiveTvFoundationViewModel.kt` | `TvModeEngine`, Pusher, tip coordinator, paid animation recovery |
| Engine | `livetv/TvModeEngine.kt` | Schedule source, player adapter, programme `eventId` |
| Player | `player/WebViewYouTubePlayerAdapter.kt` | YouTube embed |
| Lifecycle | `ON_STOP` / `ON_START` in screen | Pause/resume background |

### Payment UX entry points (exclude in Pre-MVP UX, retain code)

| Entry | Path / symbol |
|-------|----------------|
| Tip drawer | `LiveTvTipDrawer`, `openTipDrawer`, `closeTipDrawer` |
| Wallet pay flow | `LiveTvTipCoordinator`, `WalletDebitSubmitSession`, `wallet/debit` via `WalletRepository` |
| Payment QR | `LiveTvQrCheckoutFlow`, `startQrCheckout`, `checkoutUrl` → web `/c/pay/{token}` |
| Preview animations (non-payment) | `LiveTvTipAnimationHost.previewSelect` — **decide in Phase 2** if previews remain |

### Animation receive (retain)

| Component | Path | Behaviour |
|-----------|------|-----------|
| Pusher subscribe | `data/realtime/PusherService.kt` → `observeTipAnimation()` | Channel `global-live-tv`, event `tip-animation` |
| Host | `tips/LiveTvTipAnimationHost.kt` | Filters by active programme `eventId`; enqueues overlay |
| Guard | `player/TipAnimationGuard.kt` | Dedup: `bundleId`, `transactionId`, `seq`, stale threshold |
| Overlay | `ui/animations/TipAnimationOverlay.kt` | Renders triggers; completion → `LiveTvPaidAnimationRecovery.reportPlaybackCompleted()` |
| Payer recovery | `tips/LiveTvPaidAnimationRecovery.kt` | Claims via recovery session after **TV wallet/QR pay** — not passive-viewer catch-up |

### Backend emit (reference: `backend-wt-finish-a-daily-ack-8fb2f9c`)

- After succeeded tip: `pusher.trigger("global-live-tv", "tip-animation", { eventId, transactionId, … })` in `tip-payment-side-effects.service.ts` (~506).
- Success record: `transaction.status === "succeeded"` + `TipAnimationDelivery`.

### Passive vs payer recovery

| Path | When | Pre-MVP |
|------|------|---------|
| Pusher live | TV connected, programme `eventId` matches | **Primary** |
| `LiveTvPaidAnimationRecovery` | TV initiated wallet debit or QR checkout paid | **Do not expose in UX**; keep library |
| Historical replay API | Not present | **Do not build in Phase 2 without PO approval** |

### Disconnect behaviour

- Missed `tip-animation` while offline: **not recovered** for passive TVs via current architecture.
- On reconnect: Pusher resubscribes when LIVE engine runs; only **new** events animate.

### Financial side effects on receive

- `LiveTvTipAnimationHost` / mapper / overlay: **no** wallet debit or checkout API calls.
- Risk: tip **drawer** still can initiate debit in baseline — Phase 2 must hide UX only.

---

## Explore

| Component | Path | Notes |
|-----------|------|-------|
| Hub | `JourneyHubScreen.kt` label "Explore" | Currently **auth-gated** in `MainActivity` (`requireAuth`) — Phase 2 may open browsing if API allows without weakening backend |
| Feed UI | `ExploreTvScreen.kt` | Trends batch, YouTube thumb from URL |
| Playback | `ExplorePlaybackScreen.kt` | Params: `videoId` (YouTube), `title` only — **missing** `eventId` + `channelId` for share QR |
| API | `ExploreFeedRepository.kt` | `POST /api/v1/contents/feed/batch` |
| Models | `ExploreFeedItem`: `id` (channel), `event.id` (event UUID), `url` (YouTube) |

**Stable Tipmanna IDs:** `ExploreFeedItem.id` = channel id; `ExploreFeedItem.event.id` = parent event UUID. Playback today uses `YoutubeVideoIdUtil.fromUrl(item.url)` only.

---

## Video-sharing QR

### Web contract (verified in `frontend-wt-tv-checkout-8b`)

| Item | Evidence |
|------|----------|
| Builder | `src/lib/public-video-url.ts` — `buildContentShareUrl({ eventId, channelId })` |
| Path | `/content/{eventId}?v={channelId}` (`VIDEO_DEEP_LINK_PARAM = "v"`) |
| Primary origin default | `getPrimaryOrigin()` → `https://tipmanna.tv` (`site-config.ts`) |
| Page | `src/app/content/[eventId]/page.tsx` — `parseChannelSearchParam`, `pickInitialChannelContent` |
| Checkout route (forbidden for share QR) | `/c/pay/{token}` via `LiveTvQrCheckoutFlow` |

### Mobile / native deep link

- **Unverified:** No Android app link config found in this investigation pass for `/content/…`.
- **Verified (web):** Browser URL resolves channel row via `?v=` and plays content page.
- **Phase 2:** Encode **absolute** `https://tipmanna.tv/content/{eventId}?v={channelId}` pending PO confirmation of production primary origin.

### TV baseline

- ZXing present in Gradle (`com.google.zxing:core`).
- **No** share QR UI on LIVE or Explore screens at `281fe26`.
- Payment QR exists only in tip checkout flow — must not reuse.

---

## Authentication

| Surface | Baseline (`281fe26`) |
|---------|----------------------|
| LIVE TV | `requireAuth` before `LiveTvFoundation` |
| Explore | `requireAuth` before `ExplorePlaceholder` |
| Feed API | Optional Bearer via Ktor default request + `SessionStore` |
| Tipper login | `TipperLoginScreen`, `AuthRepository`, encrypted `SessionStore` |

**Working direction:** Explore open without login **if** `contents/feed/batch` succeeds anonymously — **verify on staging in Phase 2** before removing hub auth gate.

---

## Screen Display (exclude)

| Item | Location |
|------|----------|
| Hub entry | "Event screen display" → `CodeInput` → `FundraisingMilestone` |
| Auto-restore | `MainActivity` + `ScreenDisplaySessionStore` + `shouldScheduleScreenDisplayRestoreOnStart` |
| PR #2 | Sets `TvMvpNavigation.deferScreenDisplayAutoRestore = true` — Pre-MVP should adopt similar **without** taking PR #2 merge |

---

## Shared infrastructure — do not delete

- `PusherService`, `LiveTvTipAnimationHost`, `TipAnimationGuard`, `AnimationContractRepository`
- `WalletRepository`, recovery clients (unused in Pre-MVP UX)
- Auth, schedule, explore repositories
- Backend tip webhooks and emit paths unchanged
