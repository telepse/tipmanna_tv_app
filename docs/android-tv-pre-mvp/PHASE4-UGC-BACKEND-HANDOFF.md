# P4-012-BE — Tipper moderation API handoff

**Status (2026-11-11 Slice 4 / 4.1):** **IMPLEMENTED** locally — commit **`964034ce`** on branch `review/streaming-a-billing-refund-9afbfe3` in worktree `backend-wt-finish-a-daily-ack-8fb2f9c`. Migration `20261111140000_tipper_host_moderation`. **Staging deployment NOT RUN.**

### Slice 4.1 branch audit (backend owner)

| Check | Result |
|-------|--------|
| Moderation-only diff `d56876bb..964034ce` | **11 files**, moderation only |
| Full branch ancestry | Contains **streaming/billing/refund** commits below moderation — **do not deploy whole branch** without PO review; prefer **cherry-pick `964034ce`** onto staging API revision |
| Worktree cleanliness | Large **uncommitted `dist/`** — deploy from **git commit**, run `npm run build` on server/CI |
| Migration executed in this slice | **No** (agents did not run `migrate deploy`) |

## Shipped endpoints

| Capability | Endpoint |
|------------|----------|
| Report host | `POST /api/v1/moderation/hosts/:hostId/report` |
| List blocked hosts | `GET /api/v1/tippers/me/blocked-hosts` |
| Block host | `POST /api/v1/tippers/me/blocked-hosts/:hostId` |
| Unblock host | `DELETE /api/v1/tippers/me/blocked-hosts/:hostId` |

## Data model

- `HostReport` — mirrors `ContentReport` reasons; optional `contextChannelId`
- `TipperBlockedHost` — unique `(tipperId, hostId)`

## Filtering

- Explore feed: `event.hostId notIn` blocked set when Bearer tipper present
- TV schedule: strip playlist items whose channel maps to blocked host

## Deployment checklist (backend owner)

**Authoritative runbook:** `PHASE4-STAGING-MODERATION-DEPLOY-RUNBOOK.md` (order, health checks, rollback, migration verify).

1. Deploy revision containing **`964034ce`** (or approved cherry-pick).
2. `npx prisma migrate deploy` on **staging DB only** — migration `20261111140000_tipper_host_moderation`.
3. Smoke: auth’d `GET/POST/DELETE` blocked-hosts + `POST` host report; 401 without Bearer.
4. Confirm `host_reported` admin notifications (ingress + email).
5. **E12 (Slice 4.2):** Report-engine includes `HostReport` (`f2e892a1` on isolated branch). Verify moderator queue on staging after deploy.
6. Notify QA — execute `PHASE4-UGC-STAGING-E2E-CHECKLIST.md` (all scenarios **NOT RUN** until step 2 completes).

**Deploy source (clean):** `backend-wt-p4-tv-moderation-isolated` branch `feature/p4-android-tv-moderation-staging` @ **`f2e892a1`** — see `PHASE4-BACKEND-DEPLOYMENT-ISOLATION.md`. Dev worktree commits: `964034ce` + **`c03fbce1`** on `review/streaming-a-billing-refund-9afbfe3`.

**Next action owner:** **Backend owner** (staging deploy + migration). **No push** without PO approval.

## Original proposal (reference)

The sections below remain as historical context for the TV slice 2 audit.

### Verified today (TV uses as-is)

| Capability | Endpoint | Entity |
|------------|----------|--------|
| Report video | `POST /api/v1/messages/channel/:channelId/report` | `EventChannel` → `ContentReport` row; admin notify + report engine |

**Not a chat message report** — route lives under `/messages` for historical reasons; controller loads `eventChannel.findUnique({ id: channelId })`.
