# P4 staging QA handoff — Android TV moderation (E1–E13)

**Date (UTC):** 2026-11-12
**Scope:** Documentation handoff only — **E1–E13 not executed** in this pack.
**Git branch:** `feature/android-tv-pre-mvp` @ `1e664f73c0ade08e2730b7fbc329eb0b4025f072`

---

## Backend acceptance target

| Field | Value |
|-------|--------|
| Staging API base (TV APK) | `https://tipmanna-staging.onrender.com` |
| Deployed backend SHA | **`fc29451b0cb25c91a965c0cce027f2680bca8828`** |
| Migration | `20261111140000_tipper_host_moderation` (per TAYE / DB gate coordination) |
| Includes | Tipper host report/block/list, blocked-host filtering, HostReport report-engine queue, Explore saves-feed fix |

**Provenance:** TAYE read-only report verified fixture IDs against this staging deployment. Operator detail: `tipmanna_tv_app-phase2-recovery/test-evidence/p4-moderation-db-gate-20261112/DB-GATE-OPERATOR-PACK.md` (no secrets in git).

---

## Isolated staging fixture (moderation E1–E11)

**Not QR acceptance.** Legacy community IDs — **do not mutate:** `STG-LEGACY-COMMUNITY-A` / `STG-LEGACY-COMMUNITY-B` / `STG-TIPPER-A`.

| Role | Alias |
|------|-------|
| `hostId` | `STG-HOST-ISOLATED` |
| `eventId` | `STG-EVENT-ISOLATED` |
| `channelId` | `STG-CHANNEL-ISOLATED` |
| Label | `Box Draw` |

**Provision evidence:** outcome **PROVISIONED** (raw operator JSON — private archive only; not published).
**TV resolution:** saved-host by `STG-CHANNEL-ISOLATED` → host `STG-HOST-ISOLATED` (per private provision record).
**E1–E13:** **NOT RUN** — see `P4-CLOSEOUT-EVIDENCE-AUDIT-2026-11-12.md`.

---

## Staging APK artefact

| Field | Value |
|-------|--------|
| Application ID | `tv.tipmanna.androidtv.staging` |
| versionName | `1.0-staging` |
| versionCode | `1` |
| Repo HEAD (docs + implementation baseline) | `1e664f73c0ade08e2730b7fbc329eb0b4025f072` |
| APK size (bytes) | `18406910` |
| APK path (repo-relative) | `app/build/outputs/apk/staging/debug/app-staging-debug.apk` |
| SHA-256 | `BAE13E092CE72D8D30EEED532BCC6ED03499CE7A442FC6BC2304045D99CD81F1` |
| Build validation | `:app:testStagingDebugUnitTest` + `:app:assembleStagingDebug` — **BUILD SUCCESSFUL** (2026-11-12); unit summary **324 tests, 0 failures** (report HTML); `--rerun-tasks` blocked on Windows file lock |
| Install | Debug-signed; `adb install -r` on TV emulator/device |

---

## Scripted auth helper note

`TIPMANNA_DEPLOYMENT_ENV` (backend `e2e-auth-helper.util.ts`) is **optional** for QA: it gates **scripted** test-account bootstrap paths when unset or non-staging. **Deployed moderation JWT routes** (`/api/v1/moderation/…`, `/api/v1/tippers/me/blocked-hosts`, standard tipper login) do **not** require setting this env var on Render for manual TV E1–E13 execution.

---

## QR / share acceptance (out of scope)

| Item | Status |
|------|--------|
| Moderation fixture QR proof | **OUT OF SCOPE** — fixture approved for moderation only |
| Test01 Vercel alias `tipmanna-fj3ffgvfq-test01.vercel.app` | Live `/api/deploy-info` reports commit **`f15ebfd`** (2026-11-12) |
| Accepted deployment evidence pin | **`2267ee6`** / `dpl_8qjMR2N4Xcvpa257rSXVN1VU5ZY9` (immutable deployment URL in TipLink programme docs) |
| QR same-video on mobile | **BLOCKED** — alias drift + build-time `NEXT_PUBLIC_BACKEND_API_URL` not proven for current `f15ebfd` without fixture scan or pinned deployment URL |

See `P4-QR-SHARE-BLOCKED.md` in this folder.

---

## Related documents

- E1–E13 matrix: `P4-E1-E13-EXECUTION-MATRIX.md`
- Checklist (living): `docs/android-tv-pre-mvp/PHASE4-UGC-STAGING-E2E-CHECKLIST.md`
- DB gate: `tipmanna_tv_app-phase2-recovery/test-evidence/p4-moderation-db-gate-20261112/`

**Closeout audit (2026-11-12):** `P4-CLOSEOUT-EVIDENCE-AUDIT-2026-11-12.md` — fixture **VERIFIED**; E1–E13 **NOT RUN** in repo; Phase 4 **not closed**.

**Next owner:** QA executes matrix; PO/vault attestation for accounts, admin, sink, DB (not in git).
