# Phase 2 — Execution report (Gate 2)

**Date:** 2026-11-10
**Branch:** `feature/android-tv-pre-mvp`
**Technical base:** `origin/main` @ `281fe26be890bbea435a42edf0235f40538d5d12`
**Documentation commit:** `644d7df433d6bcd74dbb6ba4957e9baff3007e55`
**Feature work:** uncommitted at report time (working tree; not pushed).

## Gate 2 recommendation

**PASSED WITH CONDITIONS**

Conditions: physical TV / staging E2E (web tip → TV animation, QR scan on device) not executed in this session; Explore remains auth-gated pending PO D3 verification on staging API.

## PR separation

- TV PR **#2** (`feature/android-tv-mvp-focused`): **not merged, not modified** on remote.
- Backend PR **#154**: **not merged, not modified**.

## Implementation summary

| Area | Status |
|------|--------|
| Hub (LIVE, Explore, sign-in/out) | Done |
| Wallet / Screen Display / dev spikes removed from hub | Done |
| Screen Display auto-restore | Disabled via `PreMvpFeatureFlags.deferScreenDisplayAutoRestore` |
| LIVE payment UX (tips, wallet, checkout QR) | Hidden when `enableTvPaymentUx = false` |
| Pusher animations (`TipAnimationOverlay`, host, guard) | Unchanged in code paths |
| Explore ID propagation | `eventId` + `channelId` on `Screen.ExplorePlayback` |
| Share QR | `VideoShareQrOverlay` on LIVE + Explore; URL via `TipmannaShareUrl` |

## Changed files (feature work — why)

| File | Why |
|------|-----|
| `premvp/PreMvpFeatureFlags.kt` | Pre-MVP toggles (payment UX off, no Screen Display restore, share origin). |
| `ui/navigation/PreMvpNavigation.kt` | Hub entry list for consumption-only TV. |
| `ui/screen/JourneyHubScreen.kt` | Pre-MVP hub rows and focus. |
| `MainActivity.kt` | Hub wiring; Explore playback args; remove wallet/spike hub routes. |
| `ui/navigation/Screen.kt` | `ExplorePlayback` carries Tipmanna IDs. |
| `ui/navigation/TelepseStartupNavigation.kt` | Respect defer Screen Display restore flag. |
| `ui/screen/LiveTvFoundationScreen.kt` | Hide payment UX; share QR; focus chain without Tips chip. |
| `share/TipmannaShareUrl.kt` | Canonical `https://tipmanna.tv/content/{eventId}?v={channelId}`. |
| `share/LiveTvShareIds.kt` | LIVE programme ID resolution for QR. |
| `ui/share/VideoShareQrOverlay.kt` | QR UI + approved message. |
| `ui/livetv/LiveTvQrCodeImage.kt` | Optional `contentDescription` for share vs checkout. |
| `ExploreTvScreen.kt` / `ExplorePlaceholderScreen.kt` | Pass IDs to playback; skip cards without UUIDs. |
| `ExplorePlaybackScreen.kt` | Share QR when player ready. |
| Unit tests (navigation + share) | Hub, URL, LIVE IDs, startup navigation, back stack. |

## Payment exclusion evidence

- `PreMvpFeatureFlags.enableTvPaymentUx = false`.
- `LiveTvFoundationScreen`: `LiveTvTipDrawer`, `LiveTvTipPreviewStrip`, Tips control chip gated off; checkout QR not shown in Pre-MVP path.
- `JourneyHubScreen` / `PreMvpNavigation`: no Wallet row.
- `MainActivity`: no navigation from hub to `TipsWallet`.
- Wallet/tip coordinator code retained for future payment MVP branch.

## Animation preservation evidence

- `TipAnimationOverlay` still mounted on `LiveTvFoundationScreen`.
- `LiveTvTipAnimationHost`, `PusherService`, `TipAnimationGuard` not removed; existing unit tests retained (`TipAnimationGuardTest`, `LiveTvTipAnimationRecoveryGuardTest`, etc.).

## QR and mobile routing evidence

- URL builder matches web `buildContentSharePath` shape (`frontend-wt-tv-checkout-8b/src/lib/public-video-url.ts`).
- Origin fixed to `https://tipmanna.tv` (PO D5).
- Overlay omitted when UUIDs invalid (no silent YouTube-only fallback).
- **Not verified:** end-to-end scan on physical phone against production/staging Tipmanna app (Gate 3 item).

## Tests

**Command:** `.\gradlew.bat :app:testStagingDebugUnitTest --no-daemon`
**SDK:** `local.properties` with `sdk.dir` (local only, gitignored).

**Result (2026-11-10):** `BUILD SUCCESSFUL` — `:app:testStagingDebugUnitTest` completed in ~4m 22s (stagingDebug unit suite, including new Pre-MVP tests).

**New/updated unit tests:** `PreMvpNavigationTest`, `TipmannaShareUrlTest`, `LiveTvShareIdsTest`, `NavigationBackResolverTest`, `TelepseStartupNavigationTest`.

**Not run:** Instrumented / physical TV / staging Pusher E2E.

## Remaining blockers / unverified

| ID | Item |
|----|------|
| P2-012 | Staging: web payment success → TV passive animation |
| P2-013 | QR scan opens same Explore video on mobile |
| P2-010 | Anonymous Explore — currently auth-gated in `MainActivity` |
| P2-015 | Physical Android TV D-pad regression |

## Task register updates

| ID | Status |
|----|--------|
| P2-001 | Done |
| P2-002 | Done |
| P2-003 | Done |
| P2-004 | Done (UX hidden; code retained) |
| P2-005 | Done (code unchanged) |
| P2-006 | Done |
| P2-007 | Done |
| P2-008 | Done (QR hidden if IDs missing) |
| P2-009 | Done (resolve from schedule + target) |
| P2-010 | Open — auth gating kept |
| P2-011 | Done (unit); partial suite re-run after compile fix |
| P2-012–013, P2-015 | Not done (Phase 3 / QA) |
| P2-014 | Not done (optional copy) |

## Next step

**Stop at Gate 2.** Await explicit approval before Phase 3.
