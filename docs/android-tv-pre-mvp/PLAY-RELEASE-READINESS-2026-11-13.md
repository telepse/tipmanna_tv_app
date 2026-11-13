# Google Play release readiness — consolidated gates (2026-11-13)

**Branch target:** `feature/android-tv-pre-mvp-public` → `main`  
**Package:** `tv.tipmanna.androidtv` (prod flavor; staging uses `.staging` suffix)  
**Not a Play Console submission record** — engineering verification only.

## Automated gates (run locally)

| Gate | Command | Expected |
|------|---------|----------|
| Unit tests (staging) | `:app:testStagingDebugUnitTest` | 0 failures |
| Unit tests (prod) | `:app:testProdDebugUnitTest` | 0 failures |
| Lint (staging) | `:app:lintStagingDebug` | 0 errors |
| Lint (prod) | `:app:lintProdDebug` | 0 errors |
| Release bundle | `:app:bundleProdRelease` | Signed AAB when `keystore.properties` present |

**Signing:** Create `keystore.properties` locally (gitignored) with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Never commit keystore material.

**16 KB page size:** AGP 8.12.x with `packaging.jniLibs.useLegacyPackaging = false`. Re-verify ELF/ZIP alignment on output AAB with APK Analyzer / `bundletool` before Play upload.

## Product scope (pre-MVP)

| Area | Status | Notes |
|------|--------|-------|
| Consumption-only TV | **Implemented** | `PreMvpFeatureFlags.enableTvPaymentUx = false` |
| Explore / LIVE playback | **Implemented** | Gate 3 accepted with documented risks |
| UGC report / block / list | **Implemented** | Staging E1–E10 accepted; E11/E12a/E13 with limitations |
| E12b staff admin UI | **NOT VERIFIED** | **Release blocker** until effective moderator workflow evidenced |
| Terms before UGC upload (TV) | **N/A** | TV does not upload UGC |
| Terms before report/block | **Gap** | Legal/PO: confirm Play UGC policy satisfied via authenticated tipper signup terms on backend |
| Mobile share QR (prod) | **Disabled** | `ENABLE_VIDEO_SHARE_QR=false` on prod until same-video E2E on `tipmanna.tv` |
| Mobile share QR (staging) | **Enabled** | QA only; parity documented **BLOCKED** (`P4-QR-SHARE-BLOCKED.md`) |

## Configuration verification (prod flavor)

| Item | Value | Verified in source |
|------|-------|-------------------|
| `applicationId` | `tv.tipmanna.androidtv` | `app/build.gradle.kts` |
| `API_BASE_URL` | `https://tipmanna-live.onrender.com` | prod flavor |
| `SHARE_CONTENT_ORIGIN` | `https://tipmanna.tv` | prod flavor |
| Pusher public key | prod cluster `us3` | BuildConfig only (not a secret) |

## Play Console / human blockers (owner actions)

1. **Upload key / Play App Signing** — create or register upload key; provide `keystore.properties` on secure build host only.
2. **Branding** — official adaptive icon + **320×180 TV banner** (currently placeholder bugdroid).
3. **Legal** — Play privacy policy URL, Data safety form, UGC / moderation declaration, terms wording vs `/cookie-policy`.
4. **E12b** — evidence of operational moderator review (staff UI or equivalent workflow).
5. **Prod smoke** — signed `prodRelease` on physical Android TV: login, Explore, LIVE, moderation, back stack.
6. **Store listing** — do not claim mobile same-video QR until prod flag enabled and E2E passed.
7. **Internal testing upload** — authorized Play Console account after signed AAB exists.

## Phase 4 evidence (unchanged)

Programme: **ACCEPTED WITH DOCUMENTED LIMITATIONS**. Historical audit `P4-CLOSEOUT-EVIDENCE-AUDIT-2026-11-12.md` superseded for E1–E13 status by 2026-11-13 closeout pack.
