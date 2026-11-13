# P4 — consolidated adult-operator action packet

**Date (UTC):** 2026-11-12
**Scope:** Privileged / human-owned actions only. **No credentials in git or chat.**

---

## 1. Staging fixture operator (security-approved vault + own terminal)

**Goal:** Create/isolate `Box Draw` event + default channel on host `STG-HOST-ISOLATED` only.

1. From vault, export **short-lived** `P4_STAGING_SUPERADMIN_BEARER` in operator shell only (never Cursor).
2. Ensure `DATABASE_URL` or `DIRECT_URL` in local `.env` points at **staging** (read for script DB steps; do not paste values).
3. Run reviewed script (do **not** execute from Cursor):

```powershell
$env:P4_STAGING_SUPERADMIN_BEARER = "<from vault>"
$env:P4_EVENT_NAME = "Box Draw"
# optional: $env:P4_RESUME_HOST_ID = "STG-HOST-ISOLATED"  # default
cd backend-wt-p4-tv-moderation-validate-fc29451b
node scripts/staging/_tmp-p4-tv-moderation-fixture-provision.mjs
Remove-Item Env:P4_STAGING_SUPERADMIN_BEARER
```

**Required evidence (redacted JSON + one markdown row):**

| Field | Record |
|-------|--------|
| `hostId` | `STG-HOST-ISOLATED` |
| `eventId` | `STG-EVENT-ISOLATED` |
| `channelId` | `STG-CHANNEL-ISOLATED` |
| `eventName` | `Box Draw` |
| Staging API SHA | `fc29451b0cb25c91a965c0cce027f2680bca8828` |
| Read-only checks | `GET /api/v1/events/{eventId}`, `/channels`, `/tv-app/schedule`, saved-host by channel |

**Forbidden:** community IDs `STG-LEGACY-COMMUNITY-A`, `STG-LEGACY-COMMUNITY-B`, `STG-TIPPER-A`; password reset; subscription tier grant; payment/wallet/Stripe mutations.

---

## 2. QA accounts owner (security)

| Account | Purpose | Evidence |
|---------|---------|----------|
| Synthetic tipper A | E1–E9, E13 | Vault ref ID only in operator pack |
| Synthetic tipper B | E10 | Vault ref ID only |
| Least-privilege admin | E12 `report_engine` | Vault ref; permission attestation |

No passwords, OTP codes, or JWTs in evidence files.

---

## 3. Deploy / email owner

| Action | Evidence needed |
|--------|-----------------|
| Route moderation admin notification email to **QA-only sink** before any test that triggers `host_reported` | Redacted EMAIL_RELAY / notification config attestation |
| Confirm no production inboxes receive QA reports | Send-one test + sink receipt (no message bodies with PII in git) |

---

## 4. DB operator

| Action | Evidence needed |
|--------|-----------------|
| Read-only verification window for `ContentReport`, `HostReport`, `TipperBlockedHost` | Non-secret attestation + query window timestamps |
| Backup/recovery attestation for moderation test window | Signed operator statement (template in DB gate pack) |

---

## 5. E1–E13 QA executor (after §1–4 green)

Execute `P4-E1-E13-EXECUTION-MATRIX.md` on staging APK `app-staging-debug.apk` (SHA `BAE13E09…` @ HEAD `1e664f73…`). Record PASS/FAIL/NOT RUN per row with screenshots or log refs **without secrets**.

---

## 6. Frontend / Vercel owner (QR — optional to moderation)

| Action | Evidence needed |
|--------|-----------------|
| Pin Test01 alias to deployment `2267ee6` **or** attest `NEXT_PUBLIC_BACKEND_API_URL` on current `f15ebfd` | `/api/deploy-info` + mobile same-video scan |
| Do **not** change TV `SHARE_CONTENT_ORIGIN` without deployment proof | |

---

## 7. Legal owner

| Item | Status |
|------|--------|
| Privacy `https://tipmanna.tv/privacy-policy` | Engineering link OK; **legal sign-off outstanding** |
| Terms URL (currently `/cookie-policy` on TV) | **Legal approval outstanding** |
| Public account-deletion resource | **Not verified** as dedicated URL |
| UGC / reporting disclosures for Play | **Policy sign-off outstanding** |

---

## 8. Play Console account owner

| Item | Status |
|------|--------|
| Data Safety form completion | **NOT DONE** (engineering inventory only) |
| Production signing + upload key | **Owner action** |
| Release AAB build + 16 KB native compliance proof | **NOT RUN** (release bundle not built in closeout) |
| Store listing, app-id conflict check | **Owner action** |

---
