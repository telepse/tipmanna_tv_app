# Phase 4 — Task register (Production and Play readiness)

**Gate 4 exit:** PO sign-off on prod-ready artefact checklist (**not achieved**).
**Slice 1–3:** prior commits. **Slice 4:** UGC BE `964034ce` + TV `7bee285`. **Slice 4.1–4.2:** staging runbook, E2E checklist, E12 report-engine, isolated deploy branch; **staging deploy / E2E NOT RUN**. **Phase 4 / Gate 4 still open.**

| ID | Task | Status | Evidence / acceptance | Blocker | Owner | PO approval? |
|----|------|--------|----------------------|---------|-------|--------------|
| P4-001 | Production `applicationId` | **DONE (slice 1)** | `tv.tipmanna.androidtv` / `.staging`; `ApplicationIdFlavorTest` | Play listing ID conflict check before upload | **PO / account owner** | **Yes** (Console) |
| P4-002 | Tipmanna branding (icon, 320×180 TV banner) | **BLOCKED** | `PHASE4-BRANDING-LEGAL-AUDIT.md`; default bugdroid mipmaps | Official asset pack from design | **PO / design** | **Yes** |
| P4-003 | TV-only manifest | **DONE (slice 1)** | leanback `required=true`; `LEANBACK_LAUNCHER` only; `AndroidManifestTvDistributionTest` | — | Dev | No |
| P4-004 | Release signing + Play App Signing | **OPEN** | No keystore in repo | Eligible adult account | **Account owner** | **Yes** |
| P4-005 | Prod `BuildConfig` vs live Render | **OPEN** | Static Gradle prod flavor | Backend attestation | Backend + PO | **Yes** |
| P4-006 | Target SDK (TV) | **PASS** | API 36 | TV row API 34+ met | Dev | No |
| P4-007 | 16 KB page size | **OPEN** | Policy cited | Release AAB analyzer | Dev | No |
| P4-008 | 64-bit ABI | **OPEN** | — | Release AAB | Dev | No |
| P4-009 | Privacy policy URL + TV link | **PASS** | Prod URL + `LegalUrlsTest`; hub confirm → browser | Legal text sign-off | **PO / legal** | **Yes** |
| P4-010 | Terms URL + TV link | **PASS** | `/cookie-policy` per frontend; `LegalUrls` | Legal: Play label | **PO / legal** | **Yes** |
| P4-011 | Account deletion (web) | **BLOCKED** | `PHASE4-PLAY-CONSOLE-LEGAL-NOTES.md`; no `/delete-account` | Legal confirms privacy URL; E2E delete **NOT RUN** | **Legal / backend** | **Yes** |
| P4-013-inv | Data safety evidence | **PASS (inventory)** | `PHASE4-DATA-SAFETY-INVENTORY.md` | Play Console form | **Account owner** | **Yes** |
| P4-012a | UGC — report **video** (EventChannel) | **PASS (slice 2)** | Contract audit + `ContentReportRepositoryTest`; UI: Report video on LIVE/Explore | — | Dev | No |
| P4-012b | UGC — report **host** | **PASS (eng.)** | `POST /api/v1/moderation/hosts/.../report` + TV dialog | Staging E2E | **Backend** deploy | No |
| P4-012c | UGC — block/unblock + filter | **PASS (eng.)** | `TipperBlockedHost` + explore/TV filter + TV UI | Staging deploy + E2E | **Backend** deploy | No |
| P4-012d | UGC — host identity for context | **PASS** | GET `saved-hosts/by-channel/.../host-id`; `HostIdentityRepositoryTest` | — | Dev | No |
| P4-012e | Platform ToS before host upload | **NOT RUN (TV N/A)** | `auth.schema` `acceptedTerms` on register | Legal confirm web upload gate | **PO / legal** | Yes |
| P4-012f | Staging deploy moderation APIs + migration | **NOT RUN** | `PHASE4-BACKEND-DEPLOYMENT-ISOLATION.md`; tip `f2e892a1` | Backend owner | **Backend owner** | No |
| P4-012g | Staging E2E moderation (12 scenarios) | **NOT RUN** | `PHASE4-UGC-STAGING-E2E-CHECKLIST.md` | After P4-012f | **QA** | No |
| P4-012h | Report-engine / moderator triage for `HostReport` | **DONE (eng.)** | `reports-library.service.ts` + tests; auth via `report_engine` | Staging E12 verify | **Backend** → **QA** | No |
| P4-013 | Data safety form | **OPEN** | Token/email collection | Console draft | **Account owner** | **Yes** |
| P4-014 | Reviewer credentials + steps | **OPEN** | — | Vault → Play notes | **PO** | **Yes** |
| P4-015 | Store listing assets | **OPEN** | — | TV screenshots | **PO / marketing** | **Yes** |
| P4-016 | Physical TV smoke (D1) | **OPEN** | Emulator only | Hardware | QA | Recommended |
| P4-017 | `bundleProdRelease` smoke | **OPEN** | `assembleStagingDebug` OK | Signing | Dev | No |

### Slice 1 acceptance criteria (met)

- [x] Approved application IDs in Gradle (namespace unchanged).
- [x] Display name Tipmanna in `strings.xml`.
- [x] TV-only manifest per PO.
- [x] UGC API documented; video report wired without payment side effects.
- [x] Unit tests for application ID, manifest policy, report reasons.
- [x] No production URL/key changes; no backend/PR #154 edits.
