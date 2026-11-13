# Phase 4 — UGC moderation staging E2E checklist (Slice 4.1)

**Prerequisite:** Staging API @ **`fc29451b0cb25c91a965c0cce027f2680bca8828`** + migration `20261111140000_tipper_host_moderation` applied (TAYE-verified).
**Handoff pack:** `test-evidence/p4-moderation-staging-20261112/` (fixture, APK SHA, execution matrix).
**2026-11-13 programme closeout:** Kotlin **complete with limitations**; **E1–E13 staging moderation ACCEPTED WITH DOCUMENTED LIMITATIONS** — `P4-OPERATOR-EXECUTION-HANDOFF-RECORD-2026-11-13.md`, `P4-E1-E13-EXECUTION-MATRIX.md`. **No operator retest** in this closeout.

**Accounts:** Actual execution used an **alternative TV-verifiable staging tipper** (not the synthetic isolated `example.test` account — isolated fixture unused). Account A/B tipper UUIDs **not fully attested** in git.

**Isolated fixture (provisioned, unused for execution):** host `STG-HOST-ISOLATED` · channel `STG-CHANNEL-ISOLATED` — raw provision JSON private archive only
**Actual execution IDs:** `P4-ACTUAL-EXECUTION-DB-SNAPSHOT-2026-11-13.json`

Record each row: **PASS** | **FAIL** | **BLOCKED** | **NOT RUN** | Evidence (request id / timestamp / screenshot ref; no tokens).

---

## API contract compatibility (pre-E2E static audit)

| # | Operation | Backend | Android | Match? |
|---|-----------|---------|---------|--------|
| C1 | Report video | `POST /api/v1/messages/channel/:channelId/report` JSON `{ reason, otherText? }` | `ContentReportRepository` same path/body; reasons `verbally_abusive`… | **YES** |
| C2 | Report host | `POST /api/v1/moderation/hosts/:hostId/report` `{ reason, otherText?, contextChannelId? }` | `HostReportRepository` | **YES** |
| C3 | List blocked | `GET /api/v1/tippers/me/blocked-hosts` → `{ hostIds, hosts[] }` | Parses `hostIds` only (ignores `hosts` — OK) | **YES** |
| C4 | Block | `POST /api/v1/tippers/me/blocked-hosts/:hostId` → **201** | POST same path; accepts any 2xx | **YES** |
| C5 | Unblock | `DELETE /api/v1/tippers/me/blocked-hosts/:hostId` → **200**; 404 if not blocked | Same | **YES** |
| C6 | Host id vs channel | Report/block use **host UUID**; video report uses **channel UUID**; TV resolves host via `GET …/saved-hosts/by-channel/:channelId/host-id` | **YES** |
| C7 | Explore filter | Bearer tipper → `event.hostId notIn` blocked | Authenticated batch uses same API | **YES** (server-side) |
| C8 | LIVE filter | Bearer tipper on `GET …/tv-app/schedule` → schedule items filtered by channel→host | TV sends session Bearer | **YES** (server-side) |
| C9 | Anonymous | No Bearer → no block list; no host exclusion | TV signed-out paths | **YES** |

**Mismatch notes:** None blocking E2E prep. TV does not call `refresh()` on blocked list at login (only **Blocked hosts** screen); server-side filters still apply when signed in.

---

## E2E scenarios

| ID | Scenario | Status | Evidence | Owner |
|----|----------|--------|----------|-------|
| E1 | Account A reports video → `ContentReport` row | **ACCEPTED** | Snapshot `STG-CR-E1`; handoff record | Operator |
| E2 | Account A reports host → `HostReport` + notify | **ACCEPTED** | Snapshot `STG-CR-E2`; `staging QA host display name (operator notification sink)` sink | Operator |
| E3 | Account A blocks host → `TipperBlockedHost` | **ACCEPTED** | Block host `STG-HOST-E3-BLOCKED` (≠ E2 host) | Operator |
| E4 | GET blocked list includes host | **ACCEPTED** | Handoff record | Operator |
| E5 | Explore filter while blocked | **ACCEPTED** | Handoff record | Operator |
| E6 | LIVE schedule filter | **ACCEPTED** | Handoff record | Operator |
| E7 | Restart → block persists | **ACCEPTED** | Handoff record | Operator |
| E8 | Unblock → content returns | **ACCEPTED** | Handoff record | Operator |
| E9 | Block during playback | **ACCEPTED** | Handoff record | Operator |
| E10 | Account B isolation | **ACCEPTED** | Handoff record (no B UUID in repo) | Operator |
| E11 | Invalid host / no auth → 4xx | **ACCEPTED WITH LIMITATION** | `e11-anonymous-boundary.json` + handoff | Operator / QA |
| E12a | Moderator API resolve | **ACCEPTED WITH LIMITATION** | Handoff (no resolution export on disk) | Admin |
| E12b | Staff admin UI | **NOT VERIFIED** | No UI artefact in repo | Admin |
| E13 | No payment from moderation UI | **ACCEPTED WITH LIMITATION** | Handoff; exclude Render wallet reconcile | Operator |

---

## Manual procedure hints (when staging live)

1. Sign in Account A on TV (staging APK).
2. Open LIVE or Explore on a **fixture** channel; note `channelId` (share/debug if available).
3. Use **Report video** / **Report host** / **Block host** chips.
4. For DB verify, backend owner runs read-only queries on `ContentReport`, `HostReport`, `TipperBlockedHost` (no PII in checklist attachments).
5. Account B: fresh sign-in on second device/emulator profile.

---

## Next action owner

| Action | Owner |
|--------|--------|
| Execute E1–E13 using handoff APK + fixture | **QA** |
| Supply Account A / B + admin `report_engine` (vault) | **PO** |
| DB read-only verification (optional per scenario) | **DB operator** |
| QR same-video acceptance | **BLOCKED** — see `P4-QR-SHARE-BLOCKED.md` |
| No push of TV/backend branches without separate approval | **PO** |
