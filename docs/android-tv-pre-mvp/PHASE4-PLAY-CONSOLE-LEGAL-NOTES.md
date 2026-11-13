# Play Console — legal metadata notes (account owner)

**Do not paste secrets.** Adult account owner enters these in Play Console when Gate 4 signing/listing tasks are ready.

## Privacy policy

| Field | Value |
|-------|--------|
| URL (production) | `https://tipmanna.tv/privacy-policy` |

## Terms of use

| Field | Value |
|-------|--------|
| URL (production) | `https://tipmanna.tv/cookie-policy` |
| Note | Tipmanna web/mobile signup labels this page as **“terms”** (`terms-privacy-label.tsx`). Confirm with **Legal** that Play listing should reference cookie policy or a separate terms URL. |

## Account deletion (web resource)

Google Play requires a link explaining how users request account deletion ([policy](https://support.google.com/googleplay/android-developer/answer/13327111)).

| Field | Suggested value | Caveat |
|-------|-----------------|--------|
| Deletion URL | `https://tipmanna.tv/privacy-policy` | Section **21. Account and content deletion**; no dedicated `/delete-account` page in audited frontend |
| In-app deletion | **No** (TV consumption-only) | Self-service delete available on **web** when signed in (tipper profile → delete); API `PUT /auth/delete-tipper` |
| Contact | `hello@tipmanna.tv` | Privacy policy §29 |

**Owner:** **Legal** must confirm the privacy-policy URL alone satisfies Play for TV tipper accounts and host-linked identities.

## App category / UGC

- UGC policy items remain **partial** until P4-012-BE backend APIs ship — do not mark moderation complete in Console.
