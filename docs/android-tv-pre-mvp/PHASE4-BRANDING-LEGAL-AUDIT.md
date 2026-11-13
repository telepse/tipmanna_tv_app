# Phase 4 Slice 3 — Branding and legal audit

**Date:** 2026-11-11 · **Branch:** `feature/android-tv-pre-mvp` · **Gate 4:** not passed.

## 1. Official branding

| Asset | Spec (Play / Android TV) | Repo state | Status | Owner |
|-------|-------------------------|------------|--------|-------|
| Launcher icon | Adaptive / mipmaps per density | `mipmap-*/ic_launcher.webp` — default **Android Studio placeholder** (green grid bugdroid) | **BLOCKED** | **PO / design** |
| TV banner | **320×180**, app name visible ([TV quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality)) | Manifest `android:banner="@mipmap/ic_launcher"` (same placeholder) | **BLOCKED** | **PO / design** |
| Display name | Tipmanna | `strings.xml` `app_name` = Tipmanna | **PASS** | — |
| Logo / colours / typography | Official brand pack | No verified brand guidelines or approved logo sources in TV repo; web PWA references `/icons/icon-*.png` in `manifest.webmanifest` but binary icons not verified in workspace listing | **BLOCKED** | **PO / design** |
| Integrate without approval | — | **Not done** — no invented or scraped logo | — | Dev |

**Required from design:** layered adaptive icon (foreground/background), `drawable-xhdpi/tv_banner.png` **320×180**, Play feature graphic (separate listing task), colour tokens if UI theming must match brand.

## 2. Privacy policy and Terms of Use

| Item | Verified source | Production URL | Staging URL | Status |
|------|-----------------|----------------|-------------|--------|
| Privacy policy | `frontend-wt-tv-checkout-8b/src/app/privacy-policy/page.tsx` | `https://tipmanna.tv/privacy-policy` (HTTP 200; Next.js) | `https://tipmanna-fj3ffgvfq-test01.vercel.app/privacy-policy` | **PASS** — TV hub link via `LegalUrls` |
| Terms (platform label) | `terms-privacy-label.tsx` links **“terms”** → `/cookie-policy` | `https://tipmanna.tv/cookie-policy` (HTTP 200) | same origin pattern on staging | **PASS** — TV label “Terms of use”; **Legal** must confirm Play listing wording vs cookie policy |
| Terms at `/terms` | — | **404** on prod | — | **NOT RUN** (not used by product) |

**TV wiring:** Journey hub → confirm dialog → `ACTION_VIEW` HTTPS only (`TvLegalExternalLink`).

**Legal sign-off:** Policy text last updated **7 September 2026** in source — **Owner: PO / legal** for Play attestation.

## 3. Host Terms before UGC upload

| Check | Evidence | Status |
|-------|----------|--------|
| Registration / signup | `auth.schema.ts` `acceptedTerms` required; `auth.controller.ts` rejects false | **PASS** (backend) |
| Event/content upload gate | No `acceptedTerms` re-check found on event controllers in audited backend worktree | **NOT RUN** — legal should confirm web wizards block upload without terms |
| TV upload | Consumption-only — N/A | **NOT RUN** |

## 4. Account deletion resource

| Check | Evidence | Status |
|-------|----------|--------|
| Dedicated deletion URL | No `src/app/delete*` route in frontend worktree | **BLOCKED** for dedicated URL |
| Privacy policy §21 | Account/content deletion described (`privacy-policy/page.tsx`) | **PASS** (content in repo) |
| Web self-service delete | `tipper-banner.tsx` → `PUT …/auth/delete-tipper` (`auth.ts`); requires bearer token | **PASS** (contract in repo); **NOT RUN** E2E against live API |
| TV in-app deletion | PO: external web only | **NOT RUN** (by design) |
| Play Console URL | Use privacy policy URL + notes (see `PHASE4-PLAY-CONSOLE-LEGAL-NOTES.md`) | **BLOCKED** until **Legal** confirms sufficiency |

**Contact:** `hello@tipmanna.tv` (privacy policy §29).

## 5. UGC (unchanged)

P4-012-BE host report / block / filter remains **BLOCKED**. **Do not** mark UGC policy readiness as passed.

## 6. Build verification (icons/banner)

Staging and production **debug** builds still package placeholder mipmaps/banner — evidenced by resource audit above; no asset swap in slice 3.
