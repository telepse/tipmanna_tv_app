# Phase 4 — UGC moderation API (verified + Slice 4 / 4.1)

**Slice 4 (2026-11-11):** Backend P4-012-BE in commit **`964034ce`** (`backend-wt-finish-a-daily-ack-8fb2f9c`). **Not deployed** to staging.

**Slice 4.1 (2026-11-11):** Branch/migration audit, API contract, staging runbook, E2E checklist — **staging E2E NOT RUN**.

**Slice 4.2 (2026-11-11):** **E12** — `HostReport` rows appear in superadmin Report Engine **reports** library (`GET …/report-engine/libraries/reports`); admins with `report_engine` permission set resolution via `POST …/report-engine/reports/resolution` with `reportKind: "host"`. Email/ingress remains supplementary. Backend commit **`c03fbce1`** (dev branch) / cherry-pick **`f2e892a1`** on isolated staging branch. **Staging verification NOT RUN.**

## Report video (EventChannel)

| Method | Path | Entity |
|--------|------|--------|
| POST | `/api/v1/messages/channel/:channelId/report` | `ContentReport` on `EventChannel` |

**TV:** PASS — `ContentReportRepository`.

## Report host

| Method | Path | Persistence | Moderation |
|--------|------|-------------|------------|
| POST | `/api/v1/moderation/hosts/:hostId/report` | `HostReport` (+ optional `contextChannelId`) | `NotificationKind.host_reported` + `sendAdminNotification` |

Body: `{ reason: ContentReportReason, otherText?, contextChannelId? }` — context channel must belong to host.

Duplicate: 429 per reporter/host/24h.

**Auth:** `authMiddleware` on all moderation routes; tipper id from JWT (`requireTipperId` / paired host→tipper where applicable).

**Validation:** Invalid `hostId` / unknown host → 404; `contextChannelId` must belong to reported host → 400; invalid reason → 400.

**Moderation workflow (E12):** Row persisted + `host_reported` notification + Report Engine queue (`kind: host`, `reportedHostId`, `reportedHostLabel`, `contextChannelId`, `videoTitle`, `otherText`) + `ReportAdminResolution` with `reportKind: host`. **Authorization:** `authMiddleware`, `requireAdmin`, `requireAdminPermission("report_engine")` on superadmin routes (not exposed to tippers).

**TV:** PASS (local eng.) — `HostReportRepository` + report dialog; staging E2E **NOT RUN**.

## Block / unblock host (tipper)

| Method | Path |
|--------|------|
| GET | `/api/v1/tippers/me/blocked-hosts` → `{ hostIds, hosts[] }` |
| POST | `/api/v1/tippers/me/blocked-hosts/:hostId` |
| DELETE | `/api/v1/tippers/me/blocked-hosts/:hostId` |

Persistence: `TipperBlockedHost` (unique `tipperId` + `hostId`).

**TV:** PASS — `BlockedHostRepository`; hub **Blocked hosts** for unblock.

## Feed / LIVE filtering (authenticated tipper)

| Surface | Mechanism |
|---------|-----------|
| Explore `POST /api/v1/contents/feed/next` & `/feed/batch` | `event.hostId notIn` blocked list |
| TV `GET /api/v1/tv-app/schedule` | `filterScheduleItemsForBlockedHosts` on ongoing/upcoming items |

**TV client:** Relies on API + refreshes schedule / exits Explore after block. Unauthenticated callers get **unfiltered** explore/schedule (by design).

**Migration (not executed here):** `20261111140000_tipper_host_moderation` — `HostReport`, `TipperBlockedHost` (unique `tipperId`+`hostId`, FKs CASCADE/SET NULL per schema), `NotificationKind.host_reported` (enum append; rollback does not remove enum value).

## API ↔ Android contract (Slice 4.1 static audit)

All five TV operations match backend paths and JSON shapes. Host identity on TV: `GET /api/v1/saved-hosts/by-channel/:channelId/host-id` — **not** channel id for report/block. See compatibility table in `PHASE4-UGC-STAGING-E2E-CHECKLIST.md` (C1–C9): **no mismatches**.

## UGC policy readiness

**Partial** — engineering (incl. E12 queue) complete locally; **staging deploy** and **E2E NOT RUN**. Do not mark full Play UGC policy **PASS** or **Gate 4** until PO/legal sign-off after staging E2E.

## Legal (Slice 4 verification, unchanged)

- Host register: `acceptedTerms` required (`auth.schema.ts`). **PASS**
- Upload re-check: **NOT RUN** (no per-event `acceptedTerms` in audited controllers)
- Account deletion URL: see `PHASE4-BRANDING-LEGAL-AUDIT.md` — **BLOCKED** (legal)

## Android tests

- `ContentReportRepositoryTest`, `HostIdentityRepositoryTest`, `HostReportRepository` (via moderation VM), `BlockedHostRepositoryTest`

## Backend tests

- `tipper-blocked-host.util.spec.ts`
- `tipper-moderation.controller.spec.ts`
