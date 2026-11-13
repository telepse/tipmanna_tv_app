# Phase 4 — Backend deployment isolation (Android TV moderation)

**Slice 4.2 (2026-11-11):** Staging deploy must **not** use the dirty `backend-wt-finish-a-daily-ack-8fb2f9c` tree (uncommitted `dist/`).

## Intended staging baseline

| Item | Value |
|------|--------|
| Baseline branch | `release/combined-taye-streaming-a-staging-rc` |
| Baseline commit | **`826fbba3`** (`fix(tv-checkout): propagate tvCheckoutSessionId into Stripe PI metadata`) |
| Merge-base with `964034ce` on dev branch | **`826fbba3`** — moderation does **not** require intermediate streaming/refund commits (`d56876bb`, `53bfb2a4`, …) |

## Isolated worktree (clean deploy source)

| Item | Value |
|------|--------|
| Path | `backend-wt-p4-tv-moderation-isolated` |
| Branch | `feature/p4-android-tv-moderation-staging` |
| Commits on baseline (cherry-pick order) | **`27f0c598`** ← `964034ce` (P4-012-BE moderation APIs + migration) |
| | **`f2e892a1`** ← `c03fbce1` (E12 report-engine HostReport queue) |
| **Deploy tip (use these SHAs)** | **`f2e892a1`** |

Cherry-picks onto `826fbba3` applied **without conflicts**.

## Dependency analysis (`964034ce`)

Moderation commit touches only: Prisma schema/migration, `tipper-moderation` routes/controllers, explore/schedule block filtering, mounts. **No imports** from refund/live-second commits on the dev branch. Safe to deploy as cherry-picks on staging RC.

**E12 (`c03fbce1`):** Report-engine library + resolution endpoint only; requires `HostReport` table from `27f0c598`.

## Do not deploy

- Whole branch `review/streaming-a-billing-refund-9afbfe3` without PO review of non-moderation commits.
- Dirty worktree `backend-wt-finish-a-daily-ack-8fb2f9c` artifacts.

## Next steps (unchanged)

1. Backend owner deploys from **`backend-wt-p4-tv-moderation-isolated` @ `f2e892a1`**.
2. Migration `20261111140000_tipper_host_moderation` on staging DB.
3. QA runs `PHASE4-UGC-STAGING-E2E-CHECKLIST.md` (E2E still **NOT RUN**).
