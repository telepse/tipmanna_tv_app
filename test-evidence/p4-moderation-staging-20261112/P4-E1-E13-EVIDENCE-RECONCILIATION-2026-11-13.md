# P4 E1–E13 — evidence reconciliation (programme closeout)

**Recorded (UTC):** 2026-11-13
**Scope:** Reconcile operator-supplied execution evidence already collected. **No tests re-run. No staging writes.**

## Programme verdict

**ACCEPTED WITH DOCUMENTED LIMITATIONS**

- E1–E11 and E12a and E13 accepted on combined **DB artefacts**, **operator handoff**, and **TV smoke** (where cited).
- **E12b** (staff admin UI) **not verified** in repo — does not block TV Kotlin programme acceptance.
- **Play / Gate 4 store** remains a **separate** track (signing, AAB, branding, Console forms).

## Fixture distinction

Isolated host `STG-HOST-ISOLATED` / synthetic staging QA email (`example.test`) was **provisioned but unused** (TV OTP). Actual execution used verified staging account and content IDs in `P4-ACTUAL-EXECUTION-DB-SNAPSHOT-2026-11-13.json`. Different IDs are **expected**, not failure.

## Evidence layers (this closeout)

| Layer | What was used |
|-------|----------------|
| **Artifact-backed** | `P4-ACTUAL-EXECUTION-DB-SNAPSHOT-2026-11-13.json`; `e11-anonymous-boundary.json`; isolated provision outcome (unused; raw log private archive only) |
| **Operator handoff** | `P4-OPERATOR-EXECUTION-HANDOFF-RECORD-2026-11-13.md` (canonical narrative from operator programme discussion) |
| **TV UI smoke** | Operator report on APK `DA83F69A…` — dialogs, Actions row, player/LIVE chips, blocked list, unblock (preserved in handoff record) |
| **Not treated as E13 proof** | Render `walletTopupReconcile` background logs |

## Identity reconciliation (existing evidence only)

| Entity | Interpretation | Evidence |
|--------|----------------|----------|
| Account A / `reporterTipperId` `STG-TIPPER-A` | Staging tipper session used for TV moderation run | DB snapshot; operator handoff |
| E2 reported host `STG-TIPPER-A` | Reported host for channel context (same alias as `reporterTipperId` in DB export); operator notification used staging QA host display name in the sink | DB `HostReport`; handoff E2 |
| E2 `hostId` = `reporterTipperId` in DB export | **Documented anomaly** — reconciled by operator intent (report host on steak channel), not by re-querying staging | Snapshot `correlationLimits`; handoff identity notes |
| Channel `STG-CHANNEL-EXEC` | E1/E2 `contextChannelId`; E1 content title per operator notification | Snapshot; handoff E1 |
| Blocked host `STG-HOST-E3-BLOCKED` | **Separate** host for E3–E10 block/filter/unblock flow — **not** E2 reported host | Snapshot; handoff E3–E10; operator explicit separate block target |
| Account B | Operator attests isolation | Handoff E10 only — **no** tipper UUID in git |

## Per-scenario evidence classification

| ID | Artifact | Attestation | TV smoke | Limitation |
|----|----------|-------------|----------|------------|
| E1 | DB row | Sink + title | PASS | — |
| E2 | DB row | Sink + staging QA host display name (operator notification sink) | PASS | No sink file on disk |
| E3 | Block row | Block UX | PASS | E3 host ≠ E2 host (by design) |
| E4 | — | List UX | PASS | No GET JSON filed |
| E5 | — | Explore filter | PASS | No feed capture filed |
| E6 | — | LIVE filter | PASS | No schedule capture filed |
| E7 | — | Restart | PASS | No relaunch log filed |
| E8 | — | Unblock restore | PASS | No DELETE JSON filed |
| E9 | — | Playback exit | PASS | No recording filed |
| E10 | — | B unaffected | PASS | No B session UUID |
| E11 | Anonymous JSON | Auth’d invalid-host | Partial TV | TV negative paths not filed |
| E12a | — | Resolution persisted | N/A | No resolution export in repo |
| E12b | — | — | N/A | **No staff UI artefact** |
| E13 | — | No payment from moderation | PASS | No network file in repo; background wallet logs excluded |

## Operator prerequisites (preserved)

See `P4-OPERATOR-EXECUTION-HANDOFF-RECORD-2026-11-13.md` § Operator-confirmed prerequisites.

## Cross-references

- Matrix: `P4-E1-E13-EXECUTION-MATRIX.md`
- Kotlin closeout: `test-evidence/KOTLIN-PROJECT-CLOSEOUT-2026-11-13.md`
- Index: `EVIDENCE-INDEX.md`

**Confirmation:** **No E1–E13 tests were repeated** during this documentation update.
