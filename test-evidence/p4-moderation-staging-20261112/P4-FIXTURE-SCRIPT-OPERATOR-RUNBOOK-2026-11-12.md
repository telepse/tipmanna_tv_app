# P4 isolated fixture — operator runbook (reviewed script)

**Script:** `backend-wt-p4-tv-moderation-validate-fc29451b/scripts/staging/_tmp-p4-tv-moderation-fixture-provision.mjs`
**Closeout review (2026-11-12):**

| Policy | Implementation |
|--------|----------------|
| Fixed synthetic host | `STG-HOST-ISOLATED` (default `P4_RESUME_HOST_ID`) |
| Event label | `P4-QA-ANDROID-TV` (`P4_EVENT_NAME`) |
| No new host registration | Step `register-host` → **OMIT** |
| No password reset | Step `password-reset` → **OMIT** |
| No subscription grant | Step `admin-plan-grant` → **OMIT** (no `approve-host-subscription-tier` call) |
| Superadmin auth | `P4_STAGING_SUPERADMIN_BEARER` only (vault); no credential logging |
| Idempotency | Reuses existing event/channel by name on host when present |
| Verification | Read-only HTTP bundle at end |

**Static validation:** `node --check` → exit **0** (2026-11-12).
**Forbidden string scan:** no live `forgot-password`, `reset-password`, or `approve-host-subscription-tier` HTTP paths in script body.

**Cursor / CI:** **Do not execute** this script from automated agents.

See `P4-ADULT-OPERATOR-ACTION-PACKET-2026-11-12.md` for execution steps and evidence fields.
