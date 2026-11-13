# Phase 4 — Production and Google Play readiness audit

**Started:** 2026-11-11 (after Gate 3 **PASS WITH ACCEPTED RISKS**). **Phase 4 not complete.**
**Slice 1–3:** prior. **Slice 4 (2026-11-11):** P4-012-BE local + TV @ `7bee285`. **Slice 4.1–4.2 (2026-11-11):** staging runbook, E2E prep, **E12 report-engine** (`c03fbce1` / isolated `f2e892a1`), clean deploy worktree — **staging deploy and E2E NOT RUN**. **Slice 3 branding** still **BLOCKED**. **Gate 4 / Phase 4 not complete.**
**Scope unchanged:** consumption-only Pre-MVP; no TV wallet/checkout/payment initiation.

**Official sources checked (2026-11-11):**

- Target API: [Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878) · [Android target SDK](https://developer.android.com/google/play/requirements/target-sdk) — **Android TV:** API **34+** for new apps/updates (Aug 2025).
- 16 KB: [Support 16 KB page sizes](https://developer.android.com/guide/practices/page-sizes) · [Play blog May 2025](https://androiddevelopers.googleblog.com/2025/05/prepare-play-apps-for-devices-with-16kb-page-size.html).
- UGC: [User Generated Content policy](https://support.google.com/googleplay/android-developer/answer/9876937).
- Account deletion (TV): [Account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111) — web resource; non-mobile in-app deletion not required.
- TV quality: [TV app quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality) — 320×180 banner with app name.

---

## PO decisions recorded (slice 1)

| ID | Decision | Status in repo |
|----|----------|----------------|
| **P4-001** | Prod `tv.tipmanna.androidtv`; staging `tv.tipmanna.androidtv.staging` | **IMPLEMENTED** — `app/build.gradle.kts`; Kotlin `namespace` unchanged `com.example.telepse` |
| **P4-002** | Display name **Tipmanna**; official brand assets when verified | **BLOCKED** — placeholder `ic_launcher`; no 320×180 banner — `PHASE4-BRANDING-LEGAL-AUDIT.md` |
| **P4-003** | Dedicated Android TV; leanback required; no phone `LAUNCHER` | **IMPLEMENTED** — `AndroidManifest.xml` |
| **P4-011** | External web account deletion; no in-TV deletion flow | **DOCUMENTED** — URL **not verified** (legal/backend) |
| **P4-012** | UGC classification conservative | **PARTIAL** — video/host/block **eng. complete**; staging deploy + E2E + moderator triage for `HostReport` **open** — `PHASE4-UGC-MODERATION-API.md` |
| **P4-004** | Signing by eligible adult account owner | **UNCHANGED** — no keystore in repo |

**Play Console:** Adult account owner must confirm **P4-001** does not conflict with an existing listing before first upload.

---

## Audit detail (P4-001 … P4-017)

| ID | Current state | Evidence | Verified? | Required action | Owner | PO / adult? |
|----|---------------|----------|-----------|-----------------|-------|-------------|
| P4-001 | `tv.tipmanna.androidtv` (+ `.staging`) | Gradle | **Yes** (repo) | Play listing conflict check | **Account owner** | **Yes** |
| P4-002 | Name Tipmanna; launcher + TV banner | Placeholder mipmaps; audit doc | **Blocked** | Official icon + 320×180 banner pack | **Design / PO** | **Yes** |
| P4-003 | TV-only manifest | Main manifest + unit test | **Yes** | Flavor merged manifest review at release | Dev | No |
| P4-004 | No release signing | `build.gradle.kts` | **Blocked** | Upload keystore + Play App Signing | **Account owner** | **Yes** |
| P4-005 | Prod URLs/keys in Gradle only | Flavors | **Repo only** | Backend owner live env match | Backend + PO | **Yes** |
| P4-006 | targetSdk 36 | Gradle + Play TV row | **Yes** (target API) | Not overall Play readiness | Dev | No |
| P4-007 | 16 KB not measured | target 36 | **Not verified** | APK Analyzer on release AAB | Dev | No |
| P4-008 | 64-bit default AAB | AGP 8.12.3 | **Not verified** | Release bundle check | Dev | No |
| P4-009 | Privacy policy | `https://tipmanna.tv/privacy-policy` + TV hub | **Pass (link)** | Legal sign-off on policy text | **Legal / PO** | **Yes** |
| P4-010 | Terms of use | `https://tipmanna.tv/cookie-policy` (platform “terms”) + TV hub | **Pass (link)** | Legal: Play wording vs cookie policy | **Legal / PO** | **Yes** |
| P4-011 | Web deletion URL | Privacy §21; web `DELETE-TIPPER` API | **Blocked** | No dedicated URL; Legal Play acceptance | **Legal / backend** | **Yes** |
| P4-012 | UGC moderation (video/host/block + E12 queue) | `PHASE4-UGC-MODERATION-API.md`; deploy `PHASE4-BACKEND-DEPLOYMENT-ISOLATION.md` | **Partial** | Staging deploy from `f2e892a1` + migration; E2E **NOT RUN** | **Backend** → **QA** | **Yes** |
| P4-013 | Data safety | `PHASE4-DATA-SAFETY-INVENTORY.md` | **Not verified** | Console form draft | Account owner | **Yes** |
| P4-014 | Reviewer access | Phase 3 vault refs | **Not verified** | Play review notes | PO | **Yes** |
| P4-015 | Store assets | — | **Blocked** | Screenshots, copy | Marketing / PO | **Yes** |
| P4-016 | Physical TV (D1) | Emulator | **Deferred** | Hardware smoke | QA | Recommended |
| P4-017 | Release bundle | Staging debug OK | **Not verified** | Signed `bundleProdRelease` | Dev + account owner | Signing: **Yes** |

**Register:** `PHASE4-TASK-REGISTER.md` · **UGC API:** `PHASE4-UGC-MODERATION-API.md`.
