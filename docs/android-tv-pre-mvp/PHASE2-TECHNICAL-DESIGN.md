# Phase 2 — Technical design (Pre-MVP consumption-only)

**Status:** Design only — not implemented in Phase 1.
**Branch:** `feature/android-tv-pre-mvp` from `281fe26`.

## 1. Proposed navigation

### Hub (`JourneyHubScreen` + `MainActivity`)

| Entry | Auth | Destination |
|-------|------|-------------|
| Watch LIVE TV | Yes (retain baseline unless PO changes) | `LiveTvFoundation` |
| Explore | TBD — target **open browse** if API permits | `ExplorePlaceholder` → `ExploreTvScreen` |
| Sign in / Sign out | No | `TipperLogin` or `JourneyHubViewModel.signOut()` |

**Remove from hub (Phase 2):** Event screen display, Community room display, Tips & wallet, YouTube spike.

**Do not navigate to:** `TipsWallet`, `CodeInput`/`FundraisingMilestone` from hub; disable Screen Display auto-restore on resume (pattern from PR #2 `deferScreenDisplayAutoRestore`, implemented locally without merging PR #2).

### Back stack

Retain `NavigationBackResolver`; simplify only if hub removals require it.

---

## 2. Excluding wallet / tip / checkout without deleting infrastructure

### Pattern: UI gating + compile-safe dead paths

| Layer | Action |
|-------|--------|
| Hub | Remove callbacks `onTipsWallet`, `onScreenDisplay`, `onCommunityRoom`, `onYouTubeSpike` |
| `LiveTvFoundationScreen` | Hide tip strip / drawer entry chips; set `tip.showDrawer` unreachable from UI; do not call `openTipDrawer`, `startQrCheckout`, wallet submit |
| `LiveTvFoundationViewModel` | Optional: `PreMvpFeatureFlags` object `enableTvPaymentUx = false` wrapping coordinator calls (minimal diff) |
| `WalletRepository` / recovery | **Keep** classes; no deletion |
| Manifest / DI | **No** removal of Hilt modules |

### Verification

- Logcat: no `POST …/wallet/debit`, no `tv-tip-checkout/sessions` while using Pre-MVP flows.
- Static search: no remaining `startQrCheckout` call sites from LIVE UI.

---

## 3. Explore integration

- Hub always shows Explore (already on baseline; ensure auth policy matches PO decision).
- No wallet dependency in `ExploreViewModel` / `ExploreFeedRepository`.
- Extend navigation args:

```kotlin
// Proposed Screen.ExplorePlayback
data class ExplorePlayback(
    val videoId: String,      // YouTube — playback
    val title: String,
    val eventId: String,      // Tipmanna event UUID
    val channelId: String,    // ExploreFeedItem.id
) : Screen()
```

LIVE programme: derive `eventId` from `TvModeEngine` active programme; `channelId` from schedule item channel id when available (investigate schedule models in Phase 2 implementation).

---

## 4. Video-sharing QR

### Contract (pending PO lock on domain)

```
URL = https://tipmanna.tv/content/{eventId}?v={channelId}
Label = "Watch this video on your mobile"
```

### Implementation sketch

| Piece | Detail |
|-------|--------|
| Builder | New `TipmannaShareUrl` (Kotlin) mirroring web `buildContentSharePath` — configurable base URL from `BuildConfig` or constants file **not** hard-coded in multiple screens |
| UI | Composable `VideoShareQrOverlay` on `LiveTvFoundationScreen` + `ExplorePlaybackScreen` |
| Encoder | Existing ZXing bitmap; no `/c/pay/` |
| Updates | Regenerate when programme/video changes |
| Fallback | If `eventId` or `channelId` blank → hide QR + short message |

### Web/mobile dependency

- Confirm staging/prod `GET /content/{eventId}?v={channelId}` opens correct video on phone browser.
- Native Tipmanna app handoff: **unverified** — document in release notes if web-only.

---

## 5. Incoming successful-payment animations

### Retain

- `LiveTvTipAnimationHost.start()` when entering LIVE.
- `onProgrammeEventChanged(eventId)` from engine.
- `TipAnimationGuard` unchanged semantics.
- `TipAnimationOverlay` + `reportPlaybackCompleted()` for server delivery bookkeeping (progress only; no financial writes).

### Disable in UX

- `recoverAfterWalletPaid` / `recoverAfterQrPaid` triggers from coordinator — coordinator not invoked from UI.
- Optional: leave Pusher as sole passive path.

### Programme matching

- Payload `eventId` must equal active LIVE programme `eventId` (existing guard).

---

## 6. Duplicate prevention

Existing client guard:

- `seenBundleIds`, `seenTransactionIds`, seq/serverTs stale threshold (`TipAnimationGuard`).
- Server-side `TipAnimationDelivery` + recovery progress (backend).

Phase 2 tests: unit tests for guard; staging double-emit if feasible.

---

## 7. Network interruption

| Scenario | Expected Pre-MVP behaviour |
|----------|----------------------------|
| Brief dropout while on LIVE | Pusher client reconnects; subscription restored via `PusherService` lifecycle |
| Full offline during web tip | Event **missed** — document for users/PO; no replay API |
| App background | Existing pause/resume on WebView |
| Session expiry | `SessionExpiryHandler` — existing LIVE auth messaging |

---

## 8. Files expected to change (Phase 2)

| File | Change type |
|------|-------------|
| `MainActivity.kt` | Hub routes; disable Screen Display restore |
| `JourneyHubScreen.kt` | Hub labels/actions |
| `LiveTvFoundationScreen.kt` | Hide payment UI; add share QR |
| `LiveTvFoundationViewModel.kt` | Flag/guard payment flows |
| `ExploreTvScreen.kt` | Pass Tipmanna IDs to playback |
| `ExplorePlaybackScreen.kt` | Share QR + args |
| `Screen.kt` | Extend `ExplorePlayback` |
| `ui/navigation/NavigationBackResolver.kt` | If needed |
| New: `share/TipmannaShareUrl.kt`, `ui/share/VideoShareQrOverlay.kt` | New |
| Optional: `PreMvpFeatureFlags.kt` | New |
| Tests: hub exclusion, share URL, guard | New/updated |

### Do not change in Phase 2 (without separate approval)

- `WalletRepository.kt` payment logic
- Backend / web / mobile financial code
- Package ID, branding assets, signing

---

## 9. Testing plan (Phase 2+3)

| ID | Type | Scope |
|----|------|-------|
| UT-1 | Unit | Pre-MVP hub entries only |
| UT-2 | Unit | `TipmannaShareUrl` golden URLs |
| UT-3 | Unit | `TipAnimationGuard` (existing) |
| UT-4 | Unit | No payment API calls from ViewModel when flag off (mock) |
| IT-1 | Integration/emulator | Explore feed → play |
| IT-2 | Staging | Web tip → TV animation (TV online, matching programme) |
| IT-3 | Device | QR scan → mobile content URL (not checkout) |
| IT-4 | Device | D-pad, Back, cold start |
| IT-5 | Physical TV | Soak LIVE playback |

---

## 10. External dependencies

| System | Dependency |
|--------|------------|
| Backend | `contents/feed/batch`, TV schedule APIs, Pusher credentials, staging `tipmanna-staging.onrender.com` |
| Web | `/content/{eventId}?v=` routing live on `tipmanna.tv` |
| Mobile app | Optional; unverified deep links |
| Play | Out of Phase 2 |

---

## 11. Known limitations

- Passive TVs do not recover missed animations while disconnected.
- Explore playback uses YouTube WebView — platform/WebView constraints apply.
- Explore auth on baseline is required — opening browse without login needs API proof.
- Production deploy SHA for live API not verified in Phase 1.
- Native mobile app may open browser for QR unless app links exist.

---

## 12. Unverified assumptions (resolve in Phase 2)

- [ ] `contents/feed/batch` works without Bearer for anonymous browse
- [ ] Production primary origin = `https://tipmanna.tv` for share links
- [ ] LIVE schedule exposes channel id for QR on global programme
- [ ] Staging web tip emits to same Pusher app/cluster as TV build
