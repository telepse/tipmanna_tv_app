# Phase 4 — Data safety evidence inventory (engineering)

**Purpose:** Factual inventory for drafting the Play Data safety form — **not** a completed Console declaration.
**App:** `tv.tipmanna.androidtv` (+ `.staging`) · consumption-only Pre-MVP.

## Data collected or processed on device

| Category | What | Where stored | Encrypted? | Sent off-device? |
|----------|------|--------------|------------|------------------|
| Account / auth | Tipper JWT/session bearer token | `SessionStore` → EncryptedSharedPreferences (`tipmanna_tv_session`) | Yes (AES) | Yes — `Authorization` header to Tipmanna API |
| Account / auth | Tipper email (display) | Same prefs | Yes | Used for login OTP flow only |
| Auth (transient) | Email OTP inputs | UI memory during sign-in | No | Yes — `POST` auth endpoints |
| Playback / UGC context | Event/channel/video ids, schedules | In-memory / repository caches | No | Yes — TV schedule, explore, bootstrap APIs |
| Moderation | Content report reason + channel id | Request body only | No | Yes — `POST …/messages/channel/:id/report` |
| Screen Display (legacy code path) | Last event id preference | `ScreenDisplaySessionStore` | App prefs | Minimal; Pre-MVP hub hides wallet/SD paths |
| Wallet / payment (code present) | Wallet debit pending store | Encrypted prefs — **not exposed in Pre-MVP hub** | Yes | **Not in Pre-MVP user journeys** |

**Not collected on TV Pre-MVP paths:** precise location, contacts, SMS, microphone/camera (no permissions declared).

## Network destinations (BuildConfig)

| Flavor | API | Public web origin (QR / legal links) |
|--------|-----|--------------------------------------|
| staging | `https://tipmanna-staging.onrender.com` | `https://tipmanna-fj3ffgvfq-test01.vercel.app` |
| prod | `https://tipmanna-live.onrender.com` | `https://tipmanna.tv` |

## Third-party SDKs / libraries (gradle)

| SDK | Role | Typical data |
|-----|------|----------------|
| Ktor client | REST API | Headers, bodies per calls above |
| Pusher Java client | Realtime (LIVE) | Channel auth, connection metadata |
| ExoPlayer | Video where used | Stream URLs |
| WebView (YouTube adapter) | YouTube playback | Google/YouTube per provider |
| Coil | Images/GIFs | Image URLs |
| ZXing | QR generation for share | Encoded URLs only |
| AndroidX Security Crypto | Session encryption | Local only |

## Security practices (observed in app)

- HTTPS API base URLs in flavors (no user-configurable prod URL).
- Ktor logging redacts sensitive headers in debug (`KtorClientLogging`).
- Legal links open via system browser (`ACTION_VIEW`), not embedded login.

## Gaps for account owner

- Formal data retention / deletion mapping for TV-signed tippers vs web delete flow (**Legal / backend**).
- Whether Pusher/YouTube require additional Play SDK disclosures (**Account owner**).
