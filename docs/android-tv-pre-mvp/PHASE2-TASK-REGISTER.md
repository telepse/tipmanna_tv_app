# Phase 2 — Task register (implementation order)

**Gate 1:** passed. **Phase 2 authorisation:** executed 2026-11-10. See `PHASE2-EXECUTION-REPORT.md` for Gate 2.

| ID | Feature | Repo | Priority | Effort | Deps | Acceptance | Tests |
|----|---------|------|----------|--------|------|------------|-------|
| P2-001 | `PreMvpFeatureFlags` + compile-time payment UX off | Android TV | P0 | 0.5d | — | Flag defaults false | UT |
| P2-002 | Hub: LIVE + Explore + auth only | Android TV | P0 | 1d | P2-001 | 3 hub rows; no wallet/Screen Display | UT-1 |
| P2-003 | Disable Screen Display auto-restore | Android TV | P0 | 0.5d | P2-002 | Resume stays on hub/LIVE/Explore | IT-4 |
| P2-004 | LIVE: hide tip drawer, wallet pay, payment QR triggers | Android TV | P0 | 2–3d | P2-001 | No debit/checkout logcat | IT + log audit |
| P2-005 | Keep animation host + overlay on LIVE | Android TV | P0 | 0.5d | P2-004 | Pusher still subscribed | IT-2 |
| P2-006 | Explore: pass `eventId` + `channelId` to playback | Android TV | P0 | 1d | P2-002 | Navigation args populated | UT |
| P2-007 | `TipmannaShareUrl` builder (staging/prod base) | Android TV | P0 | 0.5d | PO D5 | Matches web path shape | UT-2 |
| P2-008 | Share QR overlay LIVE + Explore | Android TV | P0 | 1.5d | P2-006, P2-007 | Message + encodes content URL | IT-3 |
| P2-009 | LIVE QR: bind programme eventId + channelId | Android TV | P0 | 1d | P2-008 | QR updates on programme change | Manual |
| P2-010 | Explore auth policy (open vs gated) | Android TV + Product | P0 | 0.5d | staging API test | Matches PO D3 | IT-1 |
| P2-011 | Unit tests: hub, URL, payment exclusion | Android TV | P0 | 1d | P2-002–008 | CI green | UT |
| P2-012 | Staging E2E web tip → passive TV animation | Android TV + QA | P0 | 1–2d | P2-005, staging | Evidence pack | IT-2 |
| P2-013 | QR scan verification (phone) | QA + Web | P0 | 0.5d | P2-008, staging/prod URL | Not `/c/pay` | IT-3 |
| P2-014 | Document offline animation limitation in-app copy | Android TV + Product | P2 | 0.25d | — | Optional help text | — |
| P2-015 | Physical Android TV regression | QA | P0 | 1–2d | P2-011–013 | Sign-off sheet | IT-5 |

### Files/components (summary)

- `MainActivity.kt`, `JourneyHubScreen.kt`, `LiveTvFoundationScreen.kt`, `LiveTvFoundationViewModel.kt`
- `ExploreTvScreen.kt`, `ExplorePlaybackScreen.kt`, `Screen.kt`
- New: `PreMvpFeatureFlags.kt`, `TipmannaShareUrl.kt`, `VideoShareQrOverlay.kt`
- Tests under `app/src/test/…`

### Out of scope (Phase 2)

- Package ID, branding, signing, Play Console (Phase 4)
- Backend PR #154 merge
- Payment MVP PR #2 merge
- Historical animation replay API
