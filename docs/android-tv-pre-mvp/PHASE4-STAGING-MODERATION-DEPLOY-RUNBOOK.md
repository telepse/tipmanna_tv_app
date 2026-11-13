# Staging deployment runbook — P4-012-BE moderation (Slice 4.1)

**Audience:** Authorized **backend owner** only. **Staging only.** No production steps in this document.

## Deploy artifact (exact)

| Item | Value |
|------|--------|
| Worktree | `backend-wt-finish-a-daily-ack-8fb2f9c` |
| Branch (current) | `review/streaming-a-billing-refund-9afbfe3` |
| **Isolated deploy tip** | **`f2e892a1`** on `feature/p4-android-tv-moderation-staging` (see `PHASE4-BACKEND-DEPLOYMENT-ISOLATION.md`) |
| **Moderation commit** | **`27f0c598`** (cherry-pick of `964034ce`) + **E12 `f2e892a1`** |
| **Staging baseline** | `826fbba3` on `release/combined-taye-streaming-a-staging-rc` |

### Branch ancestry warning

`964034ce` is **one commit** on top of streaming/billing work (`d56876bb`, `53bfb2a4`, …). **Do not assume “moderation only” for the whole branch.** PO/backend should either:

1. **Recommended:** Cherry-pick or merge **`964034ce`** onto the **current staging API branch** after review of the single-commit diff, or
2. Deploy the full branch **only** after explicit PO approval of all commits since staging’s last deploy.

### Worktree hygiene

The worktree may have **uncommitted `dist/` build artifacts**. Deploy from **git commit `964034ce` source**, not from a dirty tree. Run `npm run build` on the deploy revision after checkout.

## Migration (do not run from TV repo agents)

| Migration | Path |
|-----------|------|
| `20261111140000_tipper_host_moderation` | `prisma/migrations/20261111140000_tipper_host_moderation/migration.sql` |

### Schema summary

- **`HostReport`** — `hostId`, reporters, `ContentReportReason`, optional `contextChannelId` → `EventChannel`
- **`TipperBlockedHost`** — `@@unique([tipperId, hostId])`, FK cascade on tipper/host delete
- **`NotificationKind`** — adds `host_reported` (enum; rollback does not remove enum values in PG)

### Rollback implications

- **Forward:** `prisma migrate deploy` applies migration once.
- **Rollback (staging only, if needed):** Stop API → `DROP TABLE "TipperBlockedHost";` `DROP TABLE "HostReport";` (drops tipper block/report data). **Do not** deploy API build that expects tables. Enum value `host_reported` may remain (harmless).
- **Data loss:** Rollback deletes all `TipperBlockedHost` / `HostReport` rows on that DB.

## Environment

No new secrets for moderation endpoints. Existing requirements:

- `JWT_SECRET` — Bearer auth for tipper routes
- Admin notification path — `EMAIL_RELAY_URL` / `EMAIL_RELAY_API_KEY` (or equivalent) for `sendAdminNotification`
- Notification ingress — same as `content_reported`

Verify staging Render (or host) **matches TV** `BuildConfig.API_BASE_URL`: `https://tipmanna-staging.onrender.com`.

## Deployment order

1. Announce maintenance window (staging only) if required by team practice.
2. Checkout deploy revision (`964034ce` or approved branch containing it).
3. `npm ci` / install, `npx prisma generate`, `npm run build`.
4. **Run migration on staging DB:** `npx prisma migrate deploy` (staging credentials only).
5. Deploy API process / restart Render staging service.
6. Run smoke tests below.
7. Notify TV QA: staging E2E may begin (`PHASE4-UGC-STAGING-E2E-CHECKLIST.md`).

## Post-deploy health checks

| Check | Expected |
|-------|----------|
| Existing API `/api/v1/tv-app/schedule` | 200 (unchanged for anonymous) |
| `GET /api/v1/tippers/me/blocked-hosts` without Bearer | **401** |
| `GET /api/v1/tippers/me/blocked-hosts` with staging tipper JWT | **200** `{ hostIds: [], hosts: [] }` |
| `POST /api/v1/moderation/hosts/{invalid}/report` no auth | **401** |
| Migration status | `npx prisma migrate status` shows `20261111140000_tipper_host_moderation` applied |

### API smoke (authenticated tipper token)

Use **staging test tipper** token only (from PO vault; do not commit).

1. `GET …/saved-hosts/by-channel/{knownChannelId}/host-id` → `{ hostId }`
2. `POST …/moderation/hosts/{hostId}/report` body `{ "reason": "deceptive" }` + optional `contextChannelId` → **201**
3. `POST …/tippers/me/blocked-hosts/{hostId}` → **201**
4. `GET …/tippers/me/blocked-hosts` → host in `hostIds`
5. `DELETE …/tippers/me/blocked-hosts/{hostId}` → **200**

## Verify migration success

```sql
-- Staging DB read-only check (owner runs)
SELECT tablename FROM pg_tables WHERE tablename IN ('HostReport', 'TipperBlockedHost');
SELECT indexname FROM pg_indexes WHERE tablename = 'TipperBlockedHost';
```

Expect both tables and unique index `TipperBlockedHost_tipperId_hostId_key`.

## Android TV pairing

- TV branch: `feature/android-tv-pre-mvp` @ **`7bee285`** (staging flavor).
- Install **staging debug** APK; sign in as same staging tipper used in smoke tests.
- E2E: `PHASE4-UGC-STAGING-E2E-CHECKLIST.md`.

## E12 moderator queue (Slice 4.2)

After deploy, authorized staff with `report_engine` permission: **Report Engine → Reports library** lists `kind: host` rows; resolve via `POST /api/v1/superadmin/report-engine/reports/resolution` with `reportKind: "host"`. Staging E12 checklist row remains **NOT RUN** until QA verifies.
