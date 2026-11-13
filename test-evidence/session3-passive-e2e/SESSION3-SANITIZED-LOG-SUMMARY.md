# Session 3 — sanitized log summary (publication)

**Replaces:** full `tipmanna-session3-startup-full.log` (retained on private archive / original feature branch only).

**Emulator:** `Television_1080p` · **Local window:** 2026-11-10 ~21:43–22:02

## Gate 3 correlation (preserved)

| Field | Public alias |
|-------|----------------|
| Active programme `eventId` | `STG-P3-EVENT-ID` |
| Web payment `transactionId` | `STG-P3-TXN-ID` |

## Observed sequence (paraphrased log lines)

1. LIVE playback: `Active programme eventId=STG-P3-EVENT-ID` before tip flow.
2. Pusher: `tip-animation emitted` for the same event (tips=2).
3. TV client: `tv_paid_animation_event_received` with `tx=STG-P3-TXN-ID` (short log prefix); `tip-animation accepted`; payment animations queued.
4. Overlay lifecycle: `Animation removed` lines for paid animation ids tied to `STG-P3-TXN-ID` (full raw ids omitted in publication).
5. ~10 min soak: no DNS/Pusher disconnect called out in the reviewed window (see Phase 3 gate docs).

## Omitted from publication

- Full logcat (~1.3 MB): device identifiers, unrelated Cast CCS message UUIDs, secondary programme/event ids, and verbose animation id strings.
- Pusher public key: fingerprint only (`1492…b418` in prior index); not repeated here.

## Artefacts still in repo

| File | Role |
|------|------|
| `Screenshot 2026-11-11 110341.png` | Emulator overlay capture |
| `EVIDENCE-INDEX.md` | Folder index |

**No new tests run** for this sanitization pass.
