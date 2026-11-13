# P4 moderation staging — E1–E13 execution matrix

**Reconciliation date:** 2026-11-13 (operator evidence closeout)
**Tests repeated during this closeout:** **None**
**Backend SHA:** `fc29451b0cb25c91a965c0cce027f2680bca8828`
**TV smoke APK:** SHA-256 `DA83F69A1B64110D3AD0B2180AC6FBF63F0AD62D2157EA5E95ECF360EA9E4DAA`

## Programme verdict

**ACCEPTED WITH DOCUMENTED LIMITATIONS** — operator executed E1–E13 on staging; artefact coverage varies by row (see reconciliation). Isolated fixture **not** used. **No** scenario re-run required for this acceptance.

**Out of scope / separate gates:** Play Console submission; QR same-video (`P4-QR-SHARE-BLOCKED.md`); Kotlin lint-green release build.

---

## Fixture sets

| Set | Status | Evidence |
|-----|--------|----------|
| **Isolated provision** (`STG-HOST-ISOLATED`, synthetic `example.test` email, `STG-CHANNEL-ISOLATED`) | **PROVISIONED, UNUSED** | Private archive only (not published) |
| **Actual execution** | **USED** | `P4-ACTUAL-EXECUTION-DB-SNAPSHOT-2026-11-13.json`, `P4-OPERATOR-EXECUTION-HANDOFF-RECORD-2026-11-13.md` |

**Execution content IDs:**

| Role | Alias |
|------|------|
| Content channel (E1/E2) | `STG-CHANNEL-EXEC` |
| Account A / reporter tipper | `STG-TIPPER-A` |
| E2 reported host (DB) | `STG-TIPPER-A` (`staging QA host display name (operator notification sink)` per operator notification) |
| E3–E10 blocked host | `STG-HOST-E3-BLOCKED` (**≠** E2 host) |

---

## Prerequisites (operator-confirmed)

| Prerequisite | Status | Source |
|--------------|--------|--------|
| `report_engine` moderator | **CONFIRMED** | Operator handoff record §prerequisites |
| QA notification sink | **CONFIRMED** | Operator handoff; E1/E2 sink message text |
| DB read-only verification | **CONFIRMED** | Operator handoff; DB snapshot JSON |
| LIVE content placement | **CONFIRMED** | Operator handoff |
| Backup/recovery attestation | **CONFIRMED** | Operator handoff (not duplicated in git) |

---

## Scenario matrix

**Legend:** **Artifact** = file or DB row in repo; **Attestation** = operator handoff record; **TV smoke** = operator physical TV run (APK `DA83F69A…`).

| ID | Overall | TV UI | Backend / behaviour | Artifact | Attestation | Primary sources |
|----|---------|-------|---------------------|----------|-------------|-----------------|
| E1 | **ACCEPTED** | PASS | PASS | DB row + ids | Notification text, TV pass | `P4-ACTUAL-EXECUTION-DB-SNAPSHOT-2026-11-13.json`; handoff record E1 |
| E2 | **ACCEPTED** | PASS | PASS | DB row + ids | `staging QA host display name (operator notification sink)` sink, TV pass | Same snapshot; handoff record E2 |
| E3 | **ACCEPTED** | PASS | PASS | Block row | TV pass | Snapshot `TipperBlockedHost`; handoff E3 |
| E4 | **ACCEPTED** | PASS | PASS | — | List UX | Handoff E4 |
| E5 | **ACCEPTED** | PASS | PASS | — | Explore exclusion | Handoff E5 |
| E6 | **ACCEPTED** | PASS | PASS | — | LIVE exclusion | Handoff E6 |
| E7 | **ACCEPTED** | PASS | PASS | — | Post-restart block | Handoff E7 |
| E8 | **ACCEPTED** | PASS | PASS | — | Unblock + restore | Handoff E8 |
| E9 | **ACCEPTED** | PASS | PASS | — | Playback → Explore | Handoff E9 |
| E10 | **ACCEPTED** | PASS | PASS | — | Account B unaffected | Handoff E10 |
| E11 | **ACCEPTED WITH LIMITATION** | PARTIAL | PASS (anon) | `e11-anonymous-boundary.json` | Auth’d invalid-host | Handoff E11; `…/e11-anonymous-boundary.json` |
| E12a | **ACCEPTED WITH LIMITATION** | N/A | PASS | — | Resolution persisted | Handoff E12a (no `ReportAdminResolution` export in repo) |
| E12b | **NOT VERIFIED** | N/A | — | — | — | No staff UI artefact in repo; not claimed in handoff |
| E13 | **ACCEPTED WITH LIMITATION** | PASS | PASS | — | No payment from moderation UI | Handoff E13; **not** Render `walletTopupReconcile` |

**Kotlin TV implementation:** **ACCEPTED** (closeout `215e464`; smoke on APK `DA83F69A…`).

---

## Remaining documentary items (no retest)

| Item | Owner |
|------|-------|
| File redacted `ReportAdminResolution` row for E12a (optional hardening) | DB / Admin |
| Staff admin UI screenshot or export for E12b | Frontend / Admin |
| Account B tipper UUID (vault-safe attestation) | Security |
| Sanitized TV network trace on disk for E13 (optional hardening) | QA |
| Redacted notification-sink receipts on disk (optional) | Deploy |

**Reconciliation:** `P4-E1-E13-EVIDENCE-RECONCILIATION-2026-11-13.md`
