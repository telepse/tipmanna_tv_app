# Pre-MVP — Phases 4–6 roadmap

**Gate 3:** **PASS WITH ACCEPTED RISKS** (2026-11-11) — Phase 4 audit **started**; Phase 4 **not complete**. See `PHASE4-READINESS-AUDIT.md`, `PHASE4-TASK-REGISTER.md`.

---

## Phase 4 — Production and Google Play readiness

| Workstream | Deliverables | Gate 4 evidence |
|------------|--------------|-----------------|
| Application ID & branding | Permanent `applicationId`, icons, feature graphic, TV banner | Signed staging vs prod IDs documented |
| Production configuration | Prod `BuildConfig` (API, Pusher, share origin) verified on **prod** flavor only | `testProdDebugUnitTest` + smoke |
| Release signing | Play upload keystore, `signingConfigs`, CI secrets (not in repo) | Signed `bundleRelease` builds |
| Android TV manifest | `LEANBACK_LAUNCHER`, banner, leanback feature declarations | Play pre-launch report / manifest review |
| Legal | Privacy policy, terms, account deletion URL (Play policy) | URLs live + in Play Console |
| UGC / moderation | Reporting path if UGC surfaces exist on TV | Policy questionnaire answers |
| Data safety | Play Data safety form aligned with app behaviour | Form submitted draft |
| Content declarations | Ads, payment, target audience | Console declarations |
| Target SDK / ABI | Current Play target API; 64-bit; **16 KB page size** (re-check current Play requirements at execution time) | Bundle analyzer + Play compatibility |
| TV assets | Screenshots, TV-specific listing assets | Console upload |
| Reviewer access | Test account + steps for Google review | Review notes |
| Release verification | `assembleProdRelease` / AAB on physical TV | Install + LIVE smoke |

**Gate 4 exit:** PO sign-off on prod build + policy artefacts; **no Phase 5** until Gate 4 PASS.

---

## Phase 5 — Testing and Google Play submission

| Workstream | Deliverables | Gate 5 evidence |
|------------|--------------|-----------------|
| Internal testing track | AAB to internal testers | Install from Play |
| Closed testing | Expanded tester group | Feedback log |
| Signed release bundle | Production AAB with versionCode policy | Artifact hash + tag |
| Store listing | Description, TV category, content rating | Published draft |
| Policy declarations | Final Data safety, ads, financial features | Console green checks |
| Testers / reviewers | Google review credentials | Encrypted handoff |
| Submission | Production → review | Submission receipt |

**Gate 5 exit:** App **approved** in Play Console (or explicit PO decision on blockers).

---

## Phase 6 — Publication and post-release

| Workstream | Deliverables | Gate 6 evidence |
|------------|--------------|-----------------|
| Staged rollout | % rollout plan | Play rollout config |
| Production monitoring | Crashlytics / Play vitals / ANR | Dashboard baseline |
| Playback monitoring | LIVE/Explore error rates | Staging vs prod comparison |
| Pusher / backend | Alerting on animation delivery failures | Runbook + owner |
| Support & rollback | Kill switch / rollback AAB procedure | Documented drill |
| Post-release acceptance | Smoke on prod after rollout | Checklist sign-off |

**Gate 6 exit:** Stable prod metrics over agreed window; PO closeout.

---

## Dependency chain

```text
Gate 3 PASS → Phase 4 → Gate 4 PASS → Phase 5 → Gate 5 PASS → Phase 6
```

Do **not** begin Phase 4 implementation until Gate 3 is formally **PASS** and PO approves.
