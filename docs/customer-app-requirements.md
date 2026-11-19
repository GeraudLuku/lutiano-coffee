# Customer app: what we need to build it

Based on the 8 customer boards in `design-assets/.../Customer_PWA`, and on what the backend already has.

## A. Decisions only you can make
1. **Product shape:** native Flutter app (as the Seller app, one code base for Android and iOS) or a PWA. The boards say "PWA", the Seller app is Flutter. Recommended: Flutter, reusing the Seller app's theme, API client, l10n and widgets.
2. **Online ordering model:** the boards show a customer who scans a sales-point QR, builds a cart and pays (MTN MoMo, Orange Money, or cash at the point of sale), then sees order status. Today only sellers record sales. Decide: does the customer's order become a *pending order* the seller prepares and confirms, or does the customer app only handle loyalty, catalogue and scan-to-identify? This decides the size of the whole project.
3. **Payments:** real mobile-money collection needs a provider contract (MTN MoMo API, Orange Money API, or an aggregator such as CinetPay, Flutterwave or Notch Pay) with merchant accounts, sandbox keys, callback URLs and fee rules. Today the backend only records which method the seller says was used.
4. **Sign-up and sign-in:** phone number + SMS code (already available through the SMS provider) and/or email + password. The boards also show Google and Apple sign-in (Apple sign-in is required by Apple if any other social login is offered on iOS).
5. **Promotions:** discount codes (board shows "LUTIANO10"), campaigns and popular/featured items: which are in the first release.
6. **Delivery or pick-up:** boards show pick-up at a sales point only; confirm there is no delivery, addresses or delivery fees.
7. **Content you provide:** Terms of Service and Privacy Policy (legal text), support contact (phone, WhatsApp, email), FAQ, onboarding slides and copy, product photos and descriptions (sizes and add-ons exist in the catalogue), opening hours per sales point, French and English wording.
8. **Accounts and brand:** Apple Developer account and Google Play account (owner names, one app per platform), the final logo files (the pack has none, the current bean mark was redrawn from the boards), store screenshots, and a privacy policy URL.
9. **Customer data rules:** how long to keep customer data, how a customer deletes their account (required by both stores), and consent wording for marketing messages.

## B. Backend work (does not exist yet)
1. **Customer identity:** the `customer` table has no login. Add credentials or phone-OTP sign-in, a `CUSTOMER` role/principal in security, token issue, password reset, and account deletion. Search design already reserves a `customerId` for own-records search.
2. **Public catalogue:** `/catalogue/search` exists. Add public reads for categories, product detail (variants, add-ons, images, price, availability per sales point) and sales-point list/detail (hours, area, open now).
3. **QR resolution:** each sales point has a permanent QR token; add a public endpoint that resolves it to the operation (name, type, hours, products sold there, stock availability).
4. **Orders (if decided in A2):** `customer_order` and lines with statuses (placed, paid, preparing, ready, collected, cancelled), idempotency key, order reference, link to the resulting `sale`, seller notification and accept/reject, stock reservation or check, cancellation and refund rules, order history and receipt PDF (receipt renderer exists).
5. **Payments (if decided in A3):** provider integration with initiate, callback/webhook (signature check), status polling, timeout and failure states, reconciliation with existing `payment` records, refunds through the existing refund flow.
6. **Loyalty for customers:** read endpoints for the customer's own points, tier, history, rewards catalogue and redemption (the staff-side loyalty exists; customer-facing reads and redeem do not), loyalty QR already exists (`loyalty_token`).
7. **Profile and preferences:** edit profile, addresses (only if delivery), favourites, notification preferences, language.
8. **Notifications:** order status and promotions through push (device tokens exist) and SMS; marketing consent flag.
9. **Support:** reuse the support-request model for customers, with an admin inbox filter.
10. **Security and limits:** rate limits on public endpoints and OTP, bot protection on sign-up, audit of customer actions, separate token audience so a customer token never works on staff endpoints, and tests for it.
11. **Admin portal additions:** customer list exists; add orders board, order detail, promotions/discount codes, customer app content (onboarding, FAQ) if it should be editable, and customer support inbox.
12. **OpenAPI contract and Postman/curl checks** for every new endpoint, as for the seller endpoints.

## C. The app itself (from the 8 boards)
1. **Authentication and discovery:** welcome/onboarding, login, register (name, phone, optional email, password, terms), OTP verification, forgot password, home (featured product, shortcuts, popular drinks), menu with categories and search, category view, product detail, customise (size, add-ons), added-to-cart confirmation.
2. **QR scanning and sales points:** scan to order, enter code manually, recent sales points, "operation found", operation details (hours, open/closed), product menu for that point, product detail and cart in that point's context.
3. **Cart and checkout:** cart (one sales point at a time), order review, discount code, payment method choice.
4. **Payments:** MTN MoMo and Orange Money flows (enter number, waiting for approval, success, failed, retry, change method), cash at the sales point with confirmation, order confirmation and receipt.
5. **Orders:** order list, detail, tracking, reorder, receipt download.
6. **Loyalty and rewards:** points balance, tier and progress, points history, rewards catalogue, redeem, redemption status.
7. **Profile and settings:** profile edit, preferences, language, notifications, security (password, sign out everywhere, delete account), help and support, FAQ, About, Terms and Privacy.
8. **Notifications and promotions:** inbox, order updates, offers.
9. **Cross-cutting (same standards as the Seller app):** French and English, loading, empty and error states, skeletons, secure token storage, crash reporting, accessibility (tap sizes, labels, text scaling), pagination on every list, tests (widget, repository, iOS integration), design fidelity report.

## D. Release work
Android signing and Play internal testing (same steps as the Seller app, a second application id such as `com.lutiano.lutiano_customer`), iOS bundle id, Apple Developer account, Firebase project/app entries for the new app id (`google-services.json`, `GoogleService-Info.plist`, APNs key), store listings and data-safety forms for customers, legal review, and a pilot with real customers at one sales point.

## E. Suggested order
1. Decisions in A (especially A2 and A3).
2. Customer sign-up/sign-in and the public catalogue and QR endpoints, then the app's Batches 1 and 2 (browse, scan, loyalty QR).
3. Loyalty and rewards for customers (backend reads already near-complete).
4. Orders and cash-at-point-of-sale, then mobile-money payments last (provider contract is the long pole, start it early).
5. Profile, notifications, support, hardening and release.

## Status (18 Nov 2026)
- **Phase 0 done:** shared Flutter package `packages/lutiano_core`; the Seller app uses it.
- **Phase 1 done (backend):** migration V29; customer tokens (`aud=lutiano-customer`, role CUSTOMER) that staff endpoints refuse, and the reverse; sign-up by SMS code (claims an administrator-created record by phone, keeping its points), sign-in by code or password, optional second step, forgot password; Google/Apple sign-in (switched on by `LUTIANO_GOOGLE_CLIENT_IDS` / `LUTIANO_APPLE_AUDIENCES`, off by default); profile, photo, phone change, password, sessions, account deletion; favourites; customer inbox, preferences and push devices; the public menu, sales locations, QR lookup and opening hours (admin editor in the location form). Search-backed menu text search needs Elasticsearch (503 without it). Real Google/Apple tokens are untested until client ids exist.
- **Phase 2 done (customer app, `apps/customer-app`):** welcome, sign-up with SMS code, log in by password or code (and the second step), forgot password, Google/Apple buttons (shown only when the server has them on; Apple on iPhone only), Home, menu (search, categories, at a chosen sales point with availability), product screen (size, add-ons, quantity, favourite heart), cart (in memory, one sales point), scan to order (QR or typed code), sales points list and detail with hours, favourites, profile (details, phone change, security: password, second step, sign out other devices, delete account), language, About, Terms and Privacy (draft). Orders and Rewards tabs say they are coming. French and English; icon and splash from the shared brand script. Checked: 12 widget tests, an iOS integration run against the live API (sign-up, menu, product) and an Android debug build. Not checked: real Google/Apple sign-in, push, the photo upload (not built yet), the Android emulator.
- **Phase 3 done (orders):** the customer checks out in cash at the counter and places an order (idempotent); a seller at that sales point sees it in Home → Customer Orders (badge), accepts, marks ready, hands over (records a normal cash sale with stock, commission and loyalty points) or declines; the customer can cancel while it is new, track it, see the receipt and buy again; an order nobody accepts in 30 minutes expires; every step sends the customer an in-app notification and a push (when push is set up); sellers get one for each new order; administrators see all orders (Sales → Customer Orders) and can cancel. Sale prices at hand-over are today's central prices, so a price change between order and hand-over changes the total. Mobile money is not built.
- **Phase 4 done (loyalty and rewards):** Rewards tab with points, tier and progress (tiers follow 12-month spending), the customer's loyalty QR (for earning in store), a rewards catalogue with why a reward is locked, redeeming into a wallet (points spent at once, valid 30 days), the wallet with a QR per reward, points history, tiers, how points work with an estimate. A seller scans the reward QR (Home → Scan Reward), sees what it is, and gives it (a free product leaves that sales point's stock). Unused rewards expire (hourly sweep) and the customer is told. Not built: applying a reward or discount voucher inside an order (a voucher is applied by the seller on the bill for now), reward photos, warnings before a reward or points expire.
- **Phase 5 done (notifications, support, account):** the customer inbox (bell with unread badge on Home, categories, mark read, open what a message is about), notification settings (in-app and push per category, order updates always on), push taps open the order, reward or reply they are about; Help & Support (FAQ, contact form with an optional photo, my requests, the team's reply) feeding the same admin inbox as sellers (new Customers filter); administrators can send an announcement to customers who accepted offers. Not built: data export, in-app support chat, push on iOS (needs the Apple account).
- **Next:** hardening and release (Phase 6): reviews of both apps and the backend, design-fidelity check, accessibility, Android signing and store setup for the customer app, Firebase registration, legal review.
