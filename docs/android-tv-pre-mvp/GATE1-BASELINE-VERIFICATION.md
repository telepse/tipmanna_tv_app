# Gate 1 — Pre-MVP baseline verification

**Date:** 2026-11-10
**Phase:** 1 (preparation only)
**Repository:** `telepse/tipmanna_tv_app`

## Verified Git state

| Item | SHA / status | Evidence |
|------|--------------|----------|
| `origin/main` (after `git fetch`) | `281fe26be890bbea435a42edf0235f40538d5d12` | Unchanged from approved baseline |
| `origin/main` tip message | Merge PR #1: Task 11AQ Kotlin TV wallet and paid-animation recovery | `git log -1 origin/main` |
| Payment-enabled branch | `origin/feature/android-tv-mvp-focused` @ `b5aa32349048b42bc4a182e269049cf837add7be` | 1 commit ahead of main |
| Pre-MVP branch (local) | `feature/android-tv-pre-mvp` @ `281fe26…` | Created from `origin/main`; tracks `origin/main` |
| Pre-MVP on remote | Does not exist | No push performed (authorised restriction) |

## Why `origin/main` @ `281fe26` is the Pre-MVP base

1. **Explore in hub** — `JourneyHubScreen` lists "Explore" and `MainActivity` routes to `ExplorePlaceholder` → `ExploreTvScreen` (payment branch **removes** Explore from hub via `TvMvpNavigation`).
2. **11AQ animation infrastructure** — PR #1 merged wallet/QR recovery paths; Pre-MVP needs passive Pusher receive + shared recovery libs **unused** in UX, not removed.
3. **Isolation from payment hub** — PR #2 only changes hub/wallet UI (`6 files`, `+142/-228` vs main); must not be merged into Pre-MVP.

## Commits / changes that must NOT enter Pre-MVP

| Source | Commit / PR | Reason |
|--------|-------------|--------|
| PR #2 | `b5aa323` | Payment MVP hub: LIVE tip + wallet visibility; drops Explore hub entry |
| PR #2 | `TvMvpNavigation.kt` (new on branch) | Product scope = payment-enabled MVP |
| Backend PR #154 | `33551c09…` on `integrate/wallet-recovery-11ap-11aq-into-dev` | Wallet debit `animationRecovery`; not required for passive web→TV Pusher path |

## PR protection status (read-only check)

| PR | Repo | State | Modified in Phase 1? |
|----|------|-------|----------------------|
| #2 | `telepse/tipmanna_tv_app` | OPEN (`b5aa323` → base `281fe26`) | **No** |
| #154 | `telepse/tipmanna_server` | OPEN (base `dev`) | **No** |

## Local worktree notes

- Stale local `main` @ `c18370c` (behind `origin/main` by 13) was **not** used as base.
- `tipmanna_tv_app-phase2-recovery` worktree remains separate; untracked test evidence preserved.
- Phase 1 added **documentation only** under `docs/android-tv-pre-mvp/` on `feature/android-tv-pre-mvp`.

## Must not merge for Pre-MVP

- `feature/android-tv-mvp-focused` (PR #2)
- `integrate/wallet-recovery-11ap-11aq-into-dev` (backend PR #154)
