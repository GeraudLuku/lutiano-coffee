# Seller / Operations app: phase plan

Status date: 17 Nov 2026. Source: the Seller design boards (00 overview, batches 01 to 08), the existing Flutter code in `apps/seller-app`, and the current API.

## Where we are

- **Built (about 4,050 lines of Dart, 47 files):** sign-in, home, my sales operation and its QR code, the new-sale flow (products, cart, cash payment, confirmation, PDF receipt), sales history and detail, inventory (overview, stock list, product stock, movements, low stock, incoming transfers with receipt confirmation), cash shifts (open, close, outflow requests, remittance, recount), profile (role and permissions, change password, about).
- **Never compiled or run.** Flutter is not installed on this machine, so every line is unverified. This is the biggest risk and Phase 0 removes it.
- **Not built:** Earnings and commissions (10 boards), Targets and bonuses (10), Notifications (10), Stock requests as designed (10), most of Profile and settings, customer lookup at sale, sign-in with QR, forgot password, push notifications.
- **API gaps for the missing screens** (all current earnings, targets and commission endpoints are administrator-only): no seller self-service for earnings, targets, bonuses, payouts, profile edit, support requests, notification preferences, devices or login activity. Stock requests exist only one product at a time. Notifications inbox (`/notifications/mine`) and the seller's own allocations (`/inventory/allocations/mine`) do exist.
- **Design caveats:** the boards are titled "PWA" but the code and README say Flutter. The Cash management board (batch 04) is missing from the pack. Prices, points, commissions and targets on the boards are illustrative.

## Decisions

| # | Decision | Status |
|---|---|---|
| 1 | Platform | **Decided: Flutter native** (as built). |
| 2 | Languages | Assumed: French default, English second (as in the admin portal and SMS). Say if not. |
| 3 | Push notifications | Assumed: yes, Phase 7, Firebase Cloud Messaging. Needs a Firebase project from you. |
| 4 | Two-factor sign-in for sellers | **Decided: administrators only**; the app shows the toggle read-only. |
| 5 | Stock requests | **Decided: one request with several items** and an SR- reference. Changes the admin Transfers screens. |
| 6 | Campaign and tiered bonuses | Assumed: later, with the admin feature; monthly targets and bonuses first. |
| 7 | Mobile Money at checkout | Assumed: cash only until the provider integration exists; methods shown as "coming soon". |
| 8 | What differs between the three seller roles | **Open: needs your answer** before Phase 1 (which screens or actions differ for fixed POS, mobile and market agent). |
| 9 | Distribution and test platform | **Decided: iOS simulator for development and testing from now on** (no Android emulator needed here). Android release comes after the pilot, from the same code. |
| 10 | Cash management board (04) | **Open:** the board is missing; send it or approve defining the screens from the specification. |

## Phases

Sizes: S under a week, M one to two weeks, L two to four weeks (one developer, including tests).

### Phase 0: Foundation (S, DONE on iOS)

Status: Flutter 3.50 installed, iOS and Android platforms created, all code compiles and analyses clean, 12 unit and widget tests pass, an integration test signs in against the live API on the iOS simulator and opens every main tab (passes), CI workflow added (`.github/workflows/seller-app.yml`). Original scope:
- Install Flutter and the Android SDK (and use Xcode and the iOS simulator tools already on this machine for iOS). Run `flutter create` for platforms, `flutter pub get`, `flutter analyze`.
- Compile and fix the existing 4,050 lines. Run the app against the local API with the seeded seller (`seller@lutiano.local`).
- Add CI: `flutter analyze` and `flutter test` on each change.
- Add the test base: repository tests with a fake HTTP client, widget tests for the sale flow, one integration test (sign in, open shift, sell, close shift).
- Decide and apply: state management (the app uses simple controllers and an inherited scope, keep it), localisation setup (ARB files, French and English), secure token storage, API base per environment.
- **Done when:** the app builds on Android and iOS, the existing flows work end to end on a device or emulator, tests run in CI.

### Phase 1: Authentication and core sales (M) — board 01
- Sign-in polish: forgot password (reuses the SMS code service), sign-in with QR (decision needed), session expiry handling.
- Role-aware home: today's sales, orders, my commission, points issued (needs the seller summary endpoint below).
- Customer at sale: scan the customer's loyalty QR or look up by phone or code (exact lookup, rate limited, as the admin search does), show tier and points, send the `customerToken`.
- Sales history: Today, Week, Month, Custom range; points earned on each sale.
- Search: product search in the sale flow and in history through `/search` (a seller only finds their own sales and active products).
- Receipt sharing, offline-safe retry using the existing idempotency key.
- **API:** `GET /me/summary` (today and month totals for the signed-in seller), `GET /customers/lookup`.

### Phase 2: Inventory (S to M, DONE 18 Nov 2026 except the request shortcut) — board 02

Decisions: no maximum level or stock-level % for now, no cost price, no Request button until Phase 3 (then only for mobile and market roles). Added `GET /inventory/summary`, `from`/`to` and `balance_after` on `/inventory/movements`, a shared period bar, the product figures, consumption detail and the inventory summary screen.

- Mostly built. Add: category filters, stock level against minimum and maximum, the sale link on a consumption movement, the period summary (in, out, net, top consumed), quick request shortcut from low stock.
- **API:** movement summary by period for the seller's operation.

### Phase 3: Stock requests (L, DONE 18 Nov 2026) — board 03

Decisions: a request is approved, rejected, dispatched and cancelled as a whole (all or nothing: if any product is short in central stock nothing is set aside); only Mobile Barista and Market Sales Agent can request (the server refuses Fixed POS with 403; they still see the status); the admin lists one row per request with its products (new Stock Requests tab; the Transfers tab keeps single-product transfers); existing transfers were left as they are (no reference, not in My Requests). Built: migration V24 (`stock_request`, `stock_allocation.request_id`, `SR-yyyy-NNNNN`), `/inventory/requests` (create with idempotency key, mine, list, get, approve, reject, dispatch, cancel), approved and rejected notifications, admin Stock Requests tab and approvals queue, and the Seller screens (overview, select products, summary and submit, My Requests, details with progress, Duplicate & Modify, start from a previous request). Receipt is still confirmed per product in Receive Stock; the request completes with the last one. Not built: notification inbox for request updates (Phase 7), the calendar filter on My Requests.

- New request (several products and quantities, requested delivery, reason), summary and submit, my requests with status filters and search by reference, request details for each status (pending, approved, rejected, in fulfilment, allocated), duplicate or modify a past request, status updates as notifications.
- **API and admin:** a request header with items (migration), statuses and reference numbers, approval and rejection with reason, fulfilment progress; the admin Transfers screens group by request. This is the biggest backend change in the project.
- **Done when:** a seller submits a three-product request, an inventory manager approves it, stock arrives and is confirmed, and every step shows on both sides.

### Phase 4: Cash management and reconciliation (M, DONE 18 Nov 2026 with a proposed design) — board 04 is missing

The board is still missing, so the screens follow the specification (S12) and the style of the other boards. Proposed design, built: the Cash screen opens with "Needs attention" (disputed remittances, remittances awaiting confirmation with their total, outflow requests awaiting approval, across all shifts; each line opens its list), a warning when a shift was left open since an earlier day (also on the Home card), the open shift's drawer and actions as before, then All outflow requests and All remittances (paged, filtered by status, with the recipient's note on a disputed one) and the shift history. Tapping a past shift opens its reconciliation: how the expected cash was reached, counted cash, variance and the seller's reason, the manager's review (who, when, note), every count (closing count and recounts), and the shift's outflows and remittances, with a Count again button when a recount was asked for. Closing a shift reminds about waiting outflow requests (they are withdrawn on close) and handovers not yet confirmed. New API: `GET /cash/overview`. Recipients still acknowledge handovers in the admin portal. Not built: push reminders (Phase 7), a seller acknowledging a handover in the app (the API lets only staff acknowledge).

- The screens exist (shift open and close, outflows, remittance, recount). Ask for the missing board or define the screens from the specification.
- Add: shift summary and history, variance explanation, handover reminder, remittance acknowledgement status.
- **Done when:** the cash flow matches the specification and the admin Cash screens.

### Phase 5: Earnings and commissions (L, DONE 18 Nov 2026) — board 05

Built as proposed: new seller-only `/me/earnings/*` endpoints (overview, breakdown, months, series, operations, sales, sale detail, payouts, payout detail, rules, statement PDF) and an Earnings tab (Home · Sales · Inventory · Earnings · Profile) with the ten board screens. Decisions taken without asking: a month is an earning period, **final** once an administrator finalises it and **provisional** (computed now, marked as such) until then; a bonus counts on the day its target ended; commission by source is product vs add-on (the percentage rules also apply to add-on value), bonuses and adjustments are shown beside it; payout statuses are grouped as Completed (paid), Pending (awaiting approval, approved, reversal pending) and Cancelled (rejected, cancelled, reversed); a payout's destination shows only the last four digits; the statement PDF is French or English and covers up to 12 months. Chart series (months, days, weeks) are bounded runs of points, not lists. Not built: product photos in the breakdown, campaign bonuses (Phase 6).

- Overview (total, commissions, bonuses, adjustments), commission breakdown by source and top products, earnings by period (day, week, month with the bar chart), earning detail for one sale, payouts and payment history, payout details, commission rules (read-only information), trends and analytics, earnings by operation, earnings statement export (PDF).
- **API (new, seller-only, own data):** `GET /me/earnings/overview`, `/breakdown`, `/periods`, `/sales/{id}`, `/payouts`, `/payouts/{id}`, `/rules`, `/analytics`, `/statement.pdf`. The data already exists (sale commissions, earning periods and adjustments, payouts); this is reading it per seller.
- **Rules:** only validated, non-refunded sales count; amounts are final only after the period is finalised.

### Phase 6: Targets and bonuses (M, DONE 18 Nov 2026, campaigns added in Phase 9) — board 06

Built: seller-only `/me/targets` (list with state and month filters, one target, summary for a month, bonus history, performance for the last N months) and, under Earnings, Targets & Bonuses with screens 1 to 9 of the board (overview ring, target details, My Targets, bonus eligibility, bonus details, bonus history, performance summary, target rules in words, progress trend). Decisions taken without asking: **only the condition the system enforces is shown as eligibility (reaching 100% of the target)**; the board's minimum sales days, no cash discrepancy and active-all-period checks are not rules anywhere, so they are not listed; a bonus is **Pending** once reached, **In earnings** when the month it ended in is finalised (it is paid out with the earnings, not on its own, so the board's "Paid" is "In earnings"), **Reversed** when a refund took it back; the trend uses the average progress of the targets that ended each month (a line, with monthly sales as bars). Not built: screen 10, **Sales Campaigns** (needs the admin campaign feature first).

- Targets overview with progress ring, target details, targets by period, bonus eligibility checklist, bonus details, bonus history, performance summary, target rules and bonus policy, progress trend, campaigns (later).
- **API:** `GET /me/targets`, `/targets/{id}`, `/bonuses`, `/performance`. Eligibility conditions shown on the board (minimum sales days, no cash discrepancy) need rules that do not exist yet; show only the conditions we actually enforce.
- **Campaigns (added 18 Nov 2026):** admin Campaigns tab (create with tiers, standings, pay bonuses once after the last day, cancel); a seller sees the campaigns that reach them with their progress (Targets & Bonuses → Sales Campaigns). A campaign has a period, an optional location, and tiers of net sales (paid less refunds); the highest tier reached pays, tiers do not add up. Settling posts a CAMPAIGN adjustment into earnings (so it flows through statements and payouts like any adjustment) and sends CAMPAIGN_BONUS. Refunds after settling do not take a bonus back. Not built: per-product or per-seller-role campaigns, campaign photos.

### Phase 7: Notifications and alerts (M to L, in-app DONE 18 Nov 2026; push NOT built) — board 07

Built with the agreed recommendations: **in-app notifications now, push later.** Backend: `app_notification` (V25) fed from the same place SMS is queued, so every message to a seller also appears in the app whether or not an SMS was sent (sellers only; one-time codes never); `/me/notifications` (list with category, unread/important and period filters; counts; one; read; clear; unread count; preferences); per-category preferences where EARNINGS and OPERATIONS are locked on; cleared items are removed after 90 days; three new in-app-only events (target at 80%, target reached, shift left open since an earlier day, once a day in the daytime), with SMS off by default; admin announcements (`/announcements`, a new Announcements tab in the admin Notifications page) to all sellers, a seller role or one operation. Seller app: bell with unread badge on Home (refreshed at sign-in, on return to the app, and after reading), the center (All / Unread / Important, long-press to select, mark as read, clear), details with a button to the screen the message is about, categories with counts, history by day, preferences. **Android push added 18 Nov 2026** (iOS push still waits for an Apple Developer account, `GoogleService-Info.plist` and an APNs key). `PushSender` is a standalone service any module can call (`notifyAfterCommit(userId, title, text, data)`); device tokens live in `device_token` (V26) behind `DeviceTokenRepository` and `POST/DELETE /me/notifications/devices`; the in-app layer sends a push for every notification unless the person switched push off for the category. The service-account key stays out of Git and out of the jar: `backend/lutiano-api/secrets/firebase-service-account.json` (gitignored; override with `LUTIANO_FIREBASE_CREDENTIALS`); without it push is simply off. In the app, `PushService` (Android only) asks permission, registers the phone after sign-in, unregisters on sign-out, and opens the notification when its push is tapped; the preferences screen shows a push switch per category once the phone is registered. **Verified:** the backend authenticates to Firebase and sends (a fake token was rejected by Firebase and removed); the app builds, installs and runs on the Android emulator. **Not yet verified:** a real push arriving on a phone. This Mac's network sends HTTPS through a TLS-intercepting proxy (Cisco Umbrella) whose certificate the emulator does not trust, so the phone cannot reach Firebase from here; try a network without the proxy (a phone hotspot) or a real device. Not built either: "Payment received" alerts (Mobile Money is not integrated) and the Acknowledge button on announcements.

- Notification centre (All, Unread, Important), detail screens, categories with counts, mark as read, select and clear, history timeline, live alerts (low stock, stock allocated, payment received, target progress, handover reminder), system announcements.
- Preferences per category, in-app and push; push token registration and delivery through Firebase Cloud Messaging.
- **API:** `/notifications/mine` exists. Add: per-user category preferences, device registration, push sending from the existing notification service, announcements (an admin broadcast screen).
- **Done when:** a stock approval on the admin portal reaches the seller's phone within seconds, in French or English as chosen.

### Phase 8: Profile, account and settings (M, DONE 18 Nov 2026) — board 08

Decisions: the seller changes their **photo, name and phone** (a new phone is confirmed with an SMS code sent to it, and cannot belong to another account); email stays with the admin; **login activity and "sign out of all other devices"** (no per-device list: sessions are stateless tokens, so the sign-in log is the history and revoking invalidates every older token while this phone gets a fresh one); **support requests with an admin inbox and a reply the seller sees**; settings **language** (kept on the phone, any language the app is translated into, French and English now), **receipt after sale**, **default operation**, notification preferences; About keeps the built-in licenses page; Terms and Privacy are not built (no text yet); Appearance (dark theme) and Data usage were not asked for. Built: `PUT /me/profile`, `POST /me/phone` and `/me/phone/confirm`, `POST /me/photo`, `GET /me/activity`, `POST /me/sessions/revoke`, `/me/support` (multipart, optional image or PDF up to 10 MB), `/support` (admin list, reply with status, attachment), a Support tab in the admin Notifications page, a SUPPORT_REPLIED notification, and the Seller screens (edit profile, security, login activity, app settings, help and support with an FAQ, contact / report an issue / suggest an improvement, my requests with replies). Not built: trusted devices per device, two-factor for sellers (administrators only, shown read-only), account deactivation (contact an admin), dark theme.

- Overview, edit profile (name, phone, email, photo; some fields read-only as the board says), role and permissions (built), sales operation details (coverage, supervisor, hours), security (change password built; trusted devices and login activity new), app settings (language, appearance, data usage, camera access, receipt options), help and support (FAQ, create a request with attachment), about the app, sign out.
- **API:** `PUT /me/profile`, photo upload, `GET /me/sessions` and revoke, support requests with attachments (and an admin inbox for them).

### Phase 9: Hardening and release (M, code part DONE 18 Nov 2026; pilot and store setup remain)
- **Done:** the session token is kept in secure storage (Keychain / Keystore) and restored at launch (an expired one starts at sign-in); tap targets of the quantity stepper and add button raised to 40; screen-reader labels checked on every icon button; Terms of Use and Privacy Policy screens with draft French and English text in the translations (legal review needed before release); campaigns (above). Network failures already show a clear message and sales reuse their idempotency key.
- **Brand assets (18 Nov 2026):** app icons (iOS, Android adaptive and legacy), splash screens (navy background with the bean and wordmark) and the in-app mark are generated by `apps/seller-app/tool/make_brand_assets.py` from a redraw of the bean in the sign-in board; the design pack has no standalone logo file.
- **Also done:** Android release signing, Crashlytics crash reports, a release app bundle builds; see `docs/release-checklist.md` for what remains and who does it.
- **Remains (needs people or accounts):** pilot with sellers, low-end Android performance, Android signing and the internal testing track, Apple developer account and iOS push, a crash-reporting provider decision, root detection decision, a legal review of the texts.
- Original plan:
- Offline and bad-network behaviour (queue and retry sales with the idempotency key, clear errors), accessibility (large text, contrast, screen readers), performance on low-end Android, crash reporting, secure storage and root detection decision, French and English review.
- Pilot with two or three sellers at one location, a week of daily use, fix list, then roll out. Training one-pager per role.
- Store setup: Android signing, internal testing track; Apple developer account if iOS is wanted.

## Order and dependencies

```
Phase 0 ──► Phase 1 ──► Phase 2 ──► Phase 3 ──► Phase 4
                │                        │
                └───────────► Phase 5 ───┴──► Phase 6 ──► Phase 7 ──► Phase 8 ──► Phase 9
```

- Phase 0 blocks everything. Phases 5 and 6 need only API work and can start in parallel with Phase 3 on the backend side.
- Phase 7 (push) depends on the Firebase decision and gives value to Phases 3, 5 and 6, so build the notification categories early and the push delivery last.
- Each phase ends with: backend tests for new endpoints, widget tests for its screens, an admin-side check where it touches approvals, and an update to `docs/design-fidelity-report.md`.

## Risks

1. Unverified existing code (Phase 0 resolves it).
2. Stock requests change a working admin flow (Phase 3): do it behind a migration that keeps old allocations readable.
3. Boards show figures we do not compute (campaign bonuses, trusted devices, supervisor phone): show only what the data supports, never invent numbers.
4. Flutter cannot be tested without a device or emulator in each session; plan for emulator access.
5. The cash board is missing, so Phase 4 depends on a new board or a written specification.


# Running the phases in parallel

Phases 1 to 8 touch different screens and different API areas, so they can run at the same time as long as three things hold: a short shared setup lands first, each track owns its own folders, and the few shared files are edited by one track only.

## Prerequisite: git

There is no git repository yet, so nothing is versioned and parallel work would overwrite itself. Before starting: `git init`, one baseline commit, then one branch (and one worktree) per track. This is the only way to let several sessions edit the same project safely.

## Step 0.5: shared setup (one session, about 2 to 3 days, before any track starts)

Removes the shared-file collisions so the tracks never touch the same lines.

1. **Split the shared Dart files by feature:** `data/models.dart` and `data/repositories.dart` become `data/models/<feature>.dart` and `data/repositories/<feature>.dart`, with barrel files so existing imports keep working.
2. **Navigation and home slots:** bottom navigation becomes Home, Sales, Inventory, Earnings, Profile (as the boards show), with the Earnings tab, the bell, and a named route table (`core/routes.dart`) already in place. Each track registers its own routes and home cards in its own file; `main_shell.dart` and `home_screen.dart` read from a list and are not edited again.
3. **Localisation:** French and English ARB files, a generated `AppLocalizations`, and a rule that every new string goes in both. Existing strings are extracted in this step.
4. **API client:** add `put`, `delete`, multipart upload (photos, attachments) and file download (PDF statements).
5. **Shared widgets used by several tracks:** period picker (day, week, month, custom), progress bar and ring, detail rows, status pill, empty and error states, a filter chip row, a chart wrapper (earnings, targets and inventory all draw charts).
6. **Backend conventions:** every seller self-service endpoint lives under `/me/...` in the package of its own domain (for example `earnings/MyEarningsController`), reads only the signed-in seller's data, and reuses the existing services. Flyway version ranges are reserved per track (below) so two migrations never share a number. The OpenAPI file is split into one fragment per track.
7. **Test conventions:** the fake API helper, one widget test per screen group, backend tests with Testcontainers, and the iOS integration test extended per track.

## The tracks

Sizes as before (S under a week, M one to two weeks, L two to four weeks). Each track is one session or one developer.

| Track | Phases | Flutter folders (owned) | Backend (owned) | Admin web | Migrations | Size | Waits for |
|---|---|---|---|---|---|---|---|
| **A. Sales and sign-in** | 1 | `features/auth`, `sale`, `history`, `home` (cards only) | `sales` (summary), customer lookup, forgot password | none | V24–V25 | M | step 0.5 |
| **B. Inventory and stock requests** | 2, 3 | `features/inventory`, `features/requests` | `inventory` (request header and items, summaries) | Transfers screens group by request | V26–V28 | L | step 0.5 |
| **C. Cash** | 4 | `features/shift` | `cash` (summaries, reminders) | small | V29 | M | step 0.5; the missing board |
| **D. Earnings and commissions** | 5 | `features/earnings` | `earnings` (`/me/earnings/*`, statement PDF) | none | none expected | L | step 0.5 |
| **E. Targets and bonuses** | 6 | `features/targets` | `earnings` target reads (`/me/targets/*`) | campaigns later | V30 | M to L | step 0.5 |
| **F. Notifications** | 7 | `features/notifications` | `notification` (preferences, devices, push, announcements) | announcements screen | V31–V32 | M to L | step 0.5; Firebase project |
| **G. Profile, security, support** | 8 | `features/profile`, `features/support` | `staff` (`/me/profile`, sessions), support requests | support inbox | V33–V34 | M | step 0.5 |
| **H. Hardening and release** | 9 | cross-cutting | cross-cutting | none | none | M | A to G mostly merged |

Track H starts last, except two pieces that can begin early and in parallel: the accessibility audit checklist and crash reporting setup.

## Dependencies between tracks (small, all one-directional)

- **B, D, E emit events that F displays.** They only call the existing notification service on the backend; F builds the screens. F does not block them, and they do not block F.
- **D and E share period and progress widgets**, which are built in step 0.5, so neither waits for the other.
- **A owns the Home screen's card slots in the shell**, but D, E and F each contribute their own card through the registry from step 0.5.
- **C's design is open.** Everything else can finish without it.

## Rules that keep parallel work from colliding

1. A track edits only its own folders and backend package. A change to a shared file goes through the track named as its owner, or into step 0.5.
2. New strings go in both language files in the same change.
3. Every endpoint added has a backend test; every screen group has a widget test; each track adds one flow to the iOS integration test.
4. Each track updates its own section of `docs/design-fidelity-report.md` (separate headings, so no conflicts).
5. Merge order: step 0.5, then tracks in any order as they finish. After each merge, the other tracks rebase and rerun the whole suite.
6. Review gate on each track before merge: `flutter analyze`, `flutter test`, backend tests, the backend-review checklist, and a run on the iOS simulator against the live API.

## Definition of done per track

The board screens exist and match the layout; every number comes from the API (nothing hardcoded from the boards); French and English strings; empty, loading and error states; works against the seeded dev data; tests green; the fidelity report updated.

## Open items that can still block a track

- **Track A:** what differs between the three seller roles (decision 8).
- **Track C:** the missing Cash management board (or approval to define it from the specification).
- **Track F:** a Firebase project for push; in-app notifications can ship without it.
- **Track E:** campaign and tiered bonuses need the admin feature first; the track ships monthly targets and bonuses.
