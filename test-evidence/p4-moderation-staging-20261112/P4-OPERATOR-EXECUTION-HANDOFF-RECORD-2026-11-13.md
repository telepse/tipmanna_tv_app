# P4 operator execution handoff record

**Recorded (UTC):** 2026-11-13
**Source:** Operator programme handoff and project discussion (evidence reconciliation task). **No tests repeated in this recording.**

## Execution context

| Item | Record |
|------|--------|
| Isolated fixture | Host `STG-HOST-ISOLATED`, synthetic staging QA email (`example.test`), channel `STG-CHANNEL-ISOLATED` — **provisioned, unused** (raw operator provision log: private archive only) |
| Reason unused | TV email verification / OTP could not be completed in the TV app |
| Actual path | Alternative verified staging TV tipper + existing staging content |
| TV smoke APK | SHA-256 `DA83F69A1B64110D3AD0B2180AC6FBF63F0AD62D2157EA5E95ECF360EA9E4DAA` |
| Backend SHA | `fc29451b0cb25c91a965c0cce027f2680bca8828` |

## Operator-confirmed prerequisites (preserved)

- Least-privilege `report_engine` moderator access.
- QA-only staging notification sink.
- Authorized read-only DB verification.
- Ability to place relevant staging content on LIVE.
- Backup/recovery attestation for the moderation window.

## Scenario outcomes (operator-supplied)

### E1 — Report video

- DB: `ContentReport` `STG-CR-E1`; channel `STG-CHANNEL-EXEC`; reporter `STG-TIPPER-A`; reason `deceptive`.
- Notification: video **"How to cook a perfect steak every time"** reported for deceptive content (QA sink).
- TV: report dialog and submission smoke **passed**.

### E2 — Report host

- DB: `HostReport` `STG-CR-E2`; reported host id `STG-TIPPER-A`; context channel `STG-CHANNEL-EXEC`; reason `deceptive`.
- Notification: host **`staging QA host display name (operator notification sink)`** reported (QA sink).
- TV: report dialog and submission smoke **passed**.

### E3 — Block host

- DB: `TipperBlockedHost` `STG-BLK-E3`; tipper `STG-TIPPER-A`; blocked host **`STG-HOST-E3-BLOCKED`**.
- TV: block confirmation and UI feedback **passed**.

### E4 — Blocked list

- Operator: blocked host visible; list reachable from TV hub.

### E5 — Explore filter

- Operator: blocked host's video **absent** from Explore while block active.

### E6 — LIVE filter

- Operator: blocked host's video **absent** from LIVE schedule while block active.

### E7 — Restart persistence

- Operator: block **remained** after TV app restart.

### E8 — Unblock and restore

- Operator: unblock succeeded; content **restored** to Explore and LIVE.

### E9 — Block during playback

- Operator: block during playback returned viewer to **Explore**.

### E10 — Account B isolation

- Operator: **Account B unaffected** by Account A's block.

### E11 — Auth / invalid host

- Operator: unauthenticated rejection and authenticated invalid-host responses verified.
- Repo artifact: `tipmanna_tv_app-phase2-recovery/test-evidence/p4-staging-acceptance-e1-e13-20261112/e11-anonymous-boundary.json` (anonymous 401/404).

### E12a — Moderator API resolution

- Operator: moderator **resolution persisted** (API / report engine).

### E12b — Staff admin UI

- **Separate** from E12a. Operator handoff does **not** cite a filed staff-UI screenshot in the TV repo.

### E13 — Payment isolation

- Operator: moderation did **not** trigger wallet, tip, debit, checkout, or `/c/pay/` from the TV moderation session.
- Render `walletTopupReconcile` entries treated as **background** only (not moderation-triggered client calls).

## Identity notes (public aliases only)

| Alias | Role in execution |
|-------|-------------------|
| `STG-TIPPER-A` | Account A session / `reporterTipperId`; E2 **reported host id** in DB export (same value as `reporterTipperId`) |
| `STG-CHANNEL-EXEC` | E1/E2 content channel (steak video context per operator notification) |
| `STG-HOST-E3-BLOCKED` | E3–E10 **blocked host** — **not** the E2 reported host |

**Documentation issue (non-retest):** `HostReport.hostId` equals `reporterTipperId` in the DB export — reconciled against the operator E2 notification (staging QA host display name in the notification sink), not as proof that tipper and host rows always share identifiers in all environments.

**Account B:** No tipper UUID filed in git; operator attestation only for E10.
