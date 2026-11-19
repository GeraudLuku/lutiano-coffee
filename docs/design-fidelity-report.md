# Design fidelity report: admin portal

Date: 17 Nov 2026. Compares the generated design boards in `design-assets/` with what is built.

## How this was checked, and its limits

- **Boards:** I viewed the module boards for every admin area (groups 00 to 07): dashboard (two variants), user details, sales locations (overview, add, detail, QR management), product catalogue, sales transactions, loyalty (two boards), targets and bonuses, reports, settings (system, roles, audit, notifications). The 19 boards in group 08 are composites of the same screens and were not compared one by one.
- **Evidence:** the dashboard, loyalty overview, locations list and the earlier module screens were compared visually in the running portal. The rest were checked against the code (routes, tabs, buttons, fields). Browser tooling was intermittently unavailable at the end, so several comparisons are code-level, not pixel-level.
- **Not compared at all:** the Seller app and Customer app boards (see the last section).
- **Result:** the portal does **not** match the designs exactly. The modules we built are close in layout and style. Several designed modules and many details are missing.

## Verdict by area — updated 17 Nov 2026

Match: same screens, layout and content. Partial: built, with differences listed. Missing: not built.

| Area (board) | Status | Differences from the design |
|---|---|---|
| Shell: sidebar and header | Mostly match | See "Shell" below. |
| Dashboard (2 boards) | Mostly match | See "Dashboard" below. |
| User details | Mostly match | Permissions tab and panel added. Tabs also include Locations & Stock and Documents (extra). |
| Sales Locations (overview, add, detail, QR management) | Match | Except the GPS position, which is required (the design says optional), as you asked. |
| Product catalogue | Match | Import (CSV) and Export. |
| Add / Edit Product | Mostly match | Variants are added inline (name, SKU, price, low stock). Loyalty points and commission are **not typed per variant**: they come from the loyalty and commission rules and are shown per variant on the product page. |
| Product details | Mostly match | Tabs Overview, Variants, Add-ons, Inventory, Loyalty & Commission, Sales Performance, Price History, Activity. Overview cards: Units Sold, Revenue, Commission Generated, Points Awarded (points are each sale's award shared by line value). |
| Sales transactions list | Mostly match | Points Issued and Commission cards added (administrators). No Status filter: every sale is completed until Mobile Money exists. |
| Sales transaction details | Match | |
| **Record Sale** | Mostly match | An administrator records a cash sale for a seller with an open shift. No add-ons, and no "Assisted Sale (QR)" mode. |
| Refund management | Match | |
| Loyalty overview, rules, rewards, tiers, adjustments | Mostly match | The design's tier editor (colour, upgrade and downgrade rules) and the split Point Settings / Expiry / Program Settings tabs are folded into existing screens. Not checked field by field. |
| **Targets and Bonuses** | Partial | Own menu item with Overview (active targets, average achievement, bonuses, targets by status, top performers), Sales Targets and Bonus Configuration. Not built: tiered and campaign bonuses, the achievement trend chart, and **Commission Disputes** (A13 skipped). |
| **Reports and Analytics** | Mostly match | Overview, Sales Reports (by operation, product, seller, channel, day, with filters and export), Loyalty & Customers (the loyalty overview), Report Builder (metrics and grouping, preview, export). The builder is simpler than the board: one data source (Sales), no saved reports, no filters step. |
| **Customers** | Partial | A Customers page (the member list with search, tiers and detail). There is no design board for it. |
| Settings: System settings | Mostly match | Editable business information, plus Business Rules, Integrations, Security and Notifications tabs and the Sales/Loyalty/Payment/Inventory/Regional sections. No Appearance tab, no logo upload. |
| Settings: Roles and permissions | Partial | 3 fixed roles, read-only (a V1 decision). The design has 7 roles, Create Role and a permission matrix. |
| Settings: Audit logs | Match | Log Entries, Unique Users, Critical Actions, date, user, module and action filters, IP address, Export. |
| Notifications page | Mostly match | Inbox with categories, icons and counts, Mark All as Read. The design's Settings and Templates tabs are our Settings and Messages tabs; the Delivery Log is an addition. |
| Payouts | Not designed | There is no payouts board. Built from the specification. |

## Shell (every page) — updated 17 Nov 2026

- **Done:** the "Admin Portal" chip under the logo, the promotional card at the bottom of the sidebar (a gradient card with the bean logo, since there is no photo asset), and the header avatar showing the uploaded photo when there is one.
- **Still different:**
  - **Menu items:** Customers, Reports and Targets & Bonuses are not in the sidebar, because those pages do not exist yet.
  - **Menu label:** the early boards say "Sales Operations" and the later ones "Sales Locations"; we use "Sales Locations".
  - **Role label:** the design says "Super Administrator". The platform has no such role, so it stays "Administrator".

## Dashboard — updated 17 Nov 2026

Follows the richer "alternative dashboard" board.

- **Done:**
  - **Top row:** Total Sales (with items sold), Payments Received (with the split by method), Customers (with new customers), Loyalty Points (with points redeemed).
  - **Second strip:** Inventory Value, Low Stock Items, Commissions (Earned), Payouts (Paid), Payouts (Outstanding).
  - **Row of panels:** Sales & Revenue Trend, Sales by Channel, and a Payment Methods panel with a share bar for every method, so MTN and Orange show 0% until enabled.
  - **Bottom row:** Recent Transactions, Inventory Alerts (the open stock alerts), Quick Actions.
- **Administrators only:** customers, loyalty, commission and payout figures. Inventory managers see Items Sold and Active Sales Locations in their place, plus the first two strip cards.
- **Still different:**
  - **Quick Actions:** the design shows Record Sale and Generate Report, which need pages that do not exist yet. Ours are Record Stock, Allocate Stock, Create Payout and Add Product.
  - **Payment method logos:** the design shows MTN and Orange logos. Ours uses text and coloured bars.
  - **System Health:** the panel on the other dashboard board is not built.
  - **Kept from before:** Top Products, Top Sellers, Needs Attention, Recent Activities and Inactive Sellers. They are not in the boards, so they sit below the designed section.
  - **Inventory Value:** it uses selling prices, because products have no cost price.

## Seller app and Customer app

- **Customer app (8 board batches):** not built.
- **Seller app (7 board batches; the Cash batch image is missing from the pack):**
  - **Built:** sign-in, sales, inventory, stock requests and cash screens, in Flutter.
  - **Not built:** earnings and commissions (batch 05), targets and bonuses (06), notifications (07), profile and settings (08).
  - **Not compared:** Flutter is not installed here, so the app has never been compiled or run, and I cannot say it matches its boards.

## Seller app: Inventory (batch 02) — updated 18 Nov 2026

Compared with `Batch_02_Inventory_Management.png` after Phase 2. Checked through widget tests and the iOS integration run against the live API; not yet by eye on the simulator.

| Board screen | Status | Notes |
|---|---|---|
| 1 Overview | Matches | Units in stock, low stock, stock value (selling price), deliveries to confirm, plus a new "Inventory summary" entry. |
| 2 My Inventory | Matches | Server-side search and category chips, paged. |
| 3 Product stock details | Mostly matches | Current stock, total received, consumed today, minimum level, sales price, SKU. **Not shown:** Cost Price (products have none) and the stock-level % bar (it needs a maximum level, decided to skip for now; the earlier bar used an invented scale of 3 times the minimum and was removed). |
| 4 Stock movements | Matches | All / In / Out plus a period (This Month by default, Today, This Week, Custom). |
| 5 Low stock | Mostly matches | The "Request Stock" button is left out until Phase 3. |
| 6 By category | Matches | Category chips from the server's summary. |
| 7 Stock level and thresholds | Partly | Minimum level and the alert note; no maximum level and no edit icons (decided: not now). |
| 8 Consumption detail | Matches | Sale reference, date and time, product, quantity, remaining stock (the balance after the sale), View sale details. Only a sale's movement opens it. |
| 9 Inventory summary | Matches | Received, consumed, net and the five most consumed products for the chosen period; no product photos in the list. |
| 10 Request replenishment | Not built | Phase 3. |

## Seller app: Stock requests (batch 03) — updated 18 Nov 2026

Compared with `Batch_03_Stock_Requests_and_Approval_Tracking.png` after Phase 3. Checked through widget tests, the iOS integration run against the live API (a real request was submitted) and the admin Stock Requests tab in the browser; the Seller screens were not looked at by eye on the simulator.

| Board screen | Status | Notes |
|---|---|---|
| 1 Overview | Matches | Total, Approved, Pending, Rejected, In fulfillment (counts from the server over all of the seller's requests), Request Stock, My Requests, Pending Approvals. "Recent Updates" is left for Phase 7 (notifications). |
| 2 Select products | Matches | Search, category chips, checkbox and quantity per product (tap the number to type one), "N products selected, total quantity". |
| 3 Summary and submit | Matches | Operation, requested delivery (as soon as possible or a date within 90 days), optional reason, items, total. Submit is pinned at the bottom; a retry reuses the same key so it never creates two requests. |
| 4 My Requests | Mostly matches | Status chips and search by reference prefix. No calendar filter. |
| 5-8 Details | Matches | Pending (awaiting review), approved (stock being prepared), rejected (reason and Duplicate & Modify), in fulfillment (progress: approved, on its way, allocated; confirm each delivery in Receive Stock). |
| 9 Request updates | Not built | Phase 7. The approved and rejected events are already sent (SMS and the notifications list). |
| 10 Create from previous | Matches | "Start from a previous request" in the product screen, and Duplicate & Modify from the details. |

**Role rule:** board 3 says all three seller roles can request; board 2 says Fixed POS only views. Following the decision taken: only Mobile Barista and Market Sales Agent can request.

## Seller app: Cash (batch 04) — updated 18 Nov 2026

**Not assessable against a board:** the Cash management board is still missing from the pack. Phase 4 followed the specification (S12) and a proposed design in the style of the other batches (see `docs/seller-app-plan.md`). Checked through widget tests and the iOS integration run against the live API; not looked at by eye on the simulator. Send the board and the screens will be compared with it.

## Seller app: Earnings (batch 05) — updated 18 Nov 2026

Compared with `Batch_05_Earnings_and_Commissions.png` after Phase 5. Checked through widget tests and the iOS integration run (English and French) against the live API with the demo seller's real data; the screens were not looked at by eye on the simulator, and the PDF was checked by text extraction only (not rendered).

| Board screen | Status | Notes |
|---|---|---|
| 1 Overview | Matches | Month picker, total with change on the month before, commissions, bonuses, adjustments, View Breakdown. Added: a Provisional / Final marker and what is still to be paid out. The "My Earnings" header card is kept; the bottom tab is now Earnings. |
| 2 Commission breakdown | Mostly matches | By source (product sales, add-ons), by product with shares (top 5, the board shows 3). "Other (non-product)" is shown as bonuses and adjustments. No product photos. |
| 3 Earnings by period | Matches | Daily, Weekly, Monthly bars with totals and the change on the month before (monthly). |
| 4 Earnings details (sale) | Mostly matches | Sale information, each line with its rule and commission, what a refund took back. Reached from Sales that earned commission and from the sale details. The "Bonus (Promo)" line is not shown because bonuses are not tied to one sale. |
| 5 Payouts and history | Matches | Total paid out and counts. The board's "Failed" is "Cancelled" here: payouts are rejected, cancelled or reversed, never failed. |
| 6 Payout details | Mostly matches | Dates, method, provider reference, account (last four digits), amount breakdown from the month's statement. |
| 7 Commission rules | Mostly matches | The rules in force for the seller's role, by product. The bonus rule is not listed: targets and bonuses come in Phase 6. |
| 8 Trends and analytics | Matches | Earnings and commissions per month for 3, 6 or 12 months, and key insights worked out from the figures. |
| 9 Earnings by operation | Matches | Per operation: commission, sales, sales amount, rate. |
| 10 Statement (export) | Matches | Period (month or last 3, 6, 12), sections to include, PDF in the seller's language. |

## Seller app: Targets and bonuses (batch 06) — updated 18 Nov 2026

Compared with `Batch_06_Sales_Targets_and_Bonuses.png` after Phase 6. Checked through widget tests and the iOS integration run (English and French) against the live API with the demo seller's real targets; not looked at by eye on the simulator.

| Board screen | Status | Notes |
|---|---|---|
| 1 Overview | Matches | Month picker, progress ring for the main target (a running monthly one), target, achieved, remaining, and the counts Active / Bonus earned / In earnings. Past 100% the ring is full and the real percentage is shown. |
| 2 Target details | Mostly matches | Target, achieved, progress, period, reward. No "Operation" row: targets belong to the seller, not to an operation. |
| 3 My Targets | Matches | All / Active / Upcoming / Completed. |
| 4 Bonus eligibility | Partly | One condition (target reached); the board's other conditions are not rules the system applies, so they are not shown. The status box says Reached, On track, Behind or Not reached, and how far away. |
| 5 Bonus details | Matches | Type, rate, target, achieved, amount, when it joins the earnings, and what the status means. "Paid on …" is "In earnings of <month>". |
| 6 Bonus history | Matches | In earnings / Pending / Reversed instead of Paid / Pending / Rejected. |
| 7 Performance summary | Matches | 3, 6 or 12 months: total sales, targets reached, bonuses earned, and month by month. |
| 8 Target rules and bonus policy | Matches | Written from how the system works; no figures of its own. |
| 9 Progress trend | Mostly matches | Monthly sales bars with an achievement line, average achievement, and the change on the period before. The board's target line is not drawn (not every month has a monthly target). |
| 10 Campaigns | Not built | Needs the admin campaign feature first. |

## Seller app: Notifications and alerts (batch 07) — updated 18 Nov 2026

Compared with `Batch_07_Notifications_and_Alerts.png` after Phase 7 (in-app only). Checked through widget tests, the iOS integration run against the live API (real notifications created by a sale and the shift reminder) and the admin Announcements tab through its API; not looked at by eye on the simulator.

| Board screen | Status | Notes |
|---|---|---|
| 1 Notification center | Matches | All (n) / Unread (n) / Important (n), category icon, title, text, time ago, unread dot. |
| 2 Details | Matches | Title, text, date, and a button to the screen the message is about (request, stock, target, payout, shift, remittances). |
| 3 Categories | Matches | Seven categories with how many are new. |
| 4 Alert preferences | Partly | In-app switches per category; EARNINGS and OPERATIONS stay on. No Push tab: push is not available yet. |
| 5 Real-time alerts | Mostly | Low stock, stock on its way, target progress and shift reminders exist; "Payment received" does not (no Mobile Money). New alerts show when the app is opened or comes back; there is no live push. |
| 6 Mark as read / manage | Matches | Menu (mark all as read, clear read, preferences) and long-press selection with mark as read and clear. |
| 7 History | Matches | Last 7 or 30 days grouped by day. |
| 8 In-app and push settings | Partly | In-app only (see 4). |
| 9 Target / bonus notification | Mostly | Same details screen; it does not draw the progress bar, it links to the target. |
| 10 System announcement | Mostly | Sent from the admin portal and shown with its own title; no Acknowledge button (opening it marks it read). |

## Seller app: Profile, account and settings (batch 08) — updated 18 Nov 2026

Compared with `Batch_08_Profile_Account_and_Settings.png` after Phase 8. Checked through widget tests, the iOS integration run and the live API; the photo picker and camera were not exercised (the simulator has no photo flow here).

| Board screen | Status | Notes |
|---|---|---|
| 1 My Profile | Mostly matches | Photo, name, role, status, operation, email, phone, shortcuts. No Employee ID or date of birth: the system holds neither. |
| 2 Edit Profile | Partly | Photo, name and phone (confirmed by SMS code). Email read-only. No date of birth or gender: not stored. |
| 3 My Role & Permissions | Matches | Built earlier. |
| 4 Sales Operation Details | Mostly matches | Built earlier: photo, address and area, stats. No supervisor or operating hours: the system holds neither. |
| 5 Security | Mostly matches | Change password, two-factor (read-only: administrators only), login activity, sign out of all other devices. No per-device Trusted Devices list. |
| 6 App Settings | Partly | Language, notification preferences, default operation, receipt after sale. No Appearance, Data Usage or Camera shortcut. |
| 7 Help & Support | Mostly matches | FAQ, contact, report an issue, suggest an improvement, my requests. No search box. |
| 8 Contact Support | Matches | Type, subject, description, optional image (10 MB). |
| 9 About | Partly | Version, licenses, Terms of Use and Privacy Policy (draft text in French and English, to be reviewed by legal). No What's New. |
| 10 Account actions | Partly | Change password and sign out. Account deactivation is left to an administrator. |

## Problems in the design pack itself

- **Mislabelled boards:** `Admin_Menu_Overview_Revision.png` (in the admin pack) is a Seller-app inventory board. `Sales_Locations_Dashboard.png` shows the product catalogue.
- **Boards disagree with each other:** the sidebar label (Operations vs Locations), the dashboard KPI sets, and the loyalty tabs differ between boards. "Exact" cannot be satisfied everywhere until one board is chosen as the reference.
- **Illustrative data:** the pack says prices, points, commissions and thresholds in the boards are examples.

## What is still open

1. **Commission disputes** and tiered or campaign bonuses (A13 was skipped by decision).
2. **Per-variant loyalty points and commission fields** in the product form (they are rule-driven here).
3. **Roles**: 7 roles, Create Role and a permission matrix (V1 keeps 3 fixed roles).
4. **Settings → Appearance**, logo upload, and the dashboard's System Health panel and payment-method logos.
5. **Loyalty tier editor** details (colour, upgrade and downgrade rules).
6. **Seller app screens** (earnings, targets, notifications, profile) and the **Customer app**, which cannot be built or checked here.

## Batch 06 screen 10 (Sales Campaigns), added after Phase 9

Built from the board: Active / Upcoming / Past chips, a card per campaign (name, dates, status, bonus now, progress toward the next tier) and a detail screen with every tier. The board's campaign photo and "campaign target" wording are not used: a campaign here has tiers of net sales, and every figure comes from the API. Checked with widget tests and the iOS integration run against the live API.

## Customer app (boards in `Customer_PWA`, batches 01 to 08)

Layout, sections, order and states follow the boards; every name, price, point and date comes from the API. Checked while building on the iOS simulator and by widget and integration tests. A line-by-line side-by-side of every screen against its board was not repeated in Phase 6, so treat "Matches" as "built from the board and seen working", not as a pixel review.

| Batch | Verdict | Differences |
|---|---|---|
| 01 Authentication and discovery | Mostly matches | Welcome, sign-up, sign-in (code or password), forgot password, Google and Apple buttons (hidden until the provider is configured), then a phone step for new social accounts. The sign-up screen has no app bar so the logo is not cut. |
| 02 QR scanning and sales points | Mostly matches | Scan, enter a code, sales point with hours and open or closed, that point's menu, recent points. Scanning needs a real camera: checked by entering the code. |
| 03 Shopping and cart | Mostly matches | Menu, categories, search, product with size and add-ons, favourites, cart tied to one sales point. |
| 04 Checkout and payments | Partly | Cash at the sales point only. Mobile money is shown as "coming soon" (needs a provider contract). No promo codes by decision. |
| 05 Orders and history | Matches | Chips, detail, timeline, receipt, buy again, cancel while pending. |
| 06 Loyalty and points | Mostly matches | Balance, tier, history, expiries, tiers, rules, estimate. |
| 07 Rewards and redemption | Partly | Catalogue, redeem, wallet, QR to show in store, history. A reward is used by a seller scanning its QR; applying a reward inside an order is not built. |
| 08 Profile, account and settings | Mostly matches | Profile, security (password, 2FA, activity, sign out everywhere), language, notifications, Help and Support, About, Terms and Privacy, delete account. No data export, no in-app chat. |
