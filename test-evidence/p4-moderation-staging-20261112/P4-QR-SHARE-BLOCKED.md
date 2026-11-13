# P4 — QR / share same-video acceptance (blocked)

**Date:** 2026-11-12
**Not part of PO moderation fixture approval.**

## TV staging configuration (unchanged in this handoff)

| BuildConfig | Value |
|-------------|--------|
| `API_BASE_URL` | `https://tipmanna-staging.onrender.com` |
| `SHARE_CONTENT_ORIGIN` | `https://tipmanna-fj3ffgvfq-test01.vercel.app` |

QR URLs follow `{SHARE_CONTENT_ORIGIN}/content/{eventId}?v={channelId}` (matches web `buildContentSharePath` contract).

## Blocker

| Check | Result |
|-------|--------|
| Live Test01 alias `/api/deploy-info` (2026-11-12 closeout recheck) | Commit **`f15ebfd`** (unchanged) |
| Programme-accepted deployment evidence | Commit **`2267ee6`** / deployment `dpl_8qjMR2N4Xcvpa257rSXVN1VU5ZY9` |
| `deploy-info` exposes `NEXT_PUBLIC_BACKEND_API_URL` | **No** — cannot prove current alias → staging API without Vercel build env or fixture scan |
| PO fixture for moderation | Approved for **E1–E11** only — **not** QR mobile parity |

**Status:** **BLOCKED** until authorized fixture-based QR scan against a **known** Test01 deployment (pinned URL or release-owner env attestation) confirms the same staging `eventId` + `channelId` play on mobile.

**Moderation E1–E13 may proceed** without closing this item unless PO adds QR to scope.
