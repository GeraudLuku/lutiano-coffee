# Local test data

`seed_test_data.py` fills a local database with about 45 days of realistic activity. It creates everything through
the API, so every ledger stays consistent with the business rules. It then moves each simulated day back to
07:00–18:00 (Douala time) on a past date.

```bash
python3 infrastructure/dev/seed_test_data.py
```

To add only the payout data (numbers and payouts) to a database that is already seeded, run it with `payouts`:

```bash
python3 infrastructure/dev/seed_test_data.py payouts
```

It needs the API on `localhost:8080` and the `lutiano-mariadb-1` container. The script refuses to run twice on the
same database. To start again, restore a database that holds only the migration demo data, then run it once more.
A run takes about 10 minutes.

## Accounts

Every account's password is `Coffee123!`.

Administrators sign in in two steps: the password, then a 6-digit SMS code. SMS runs in **dev mode** by default (messages are
written to the API log, not sent), and the dev code `123456` is accepted. The seed script uses it automatically. To send real
messages, set `LUTIANO_SMS_MODE=live`, `HOMBEE_SMS_API_KEY` and `HOMBEE_SMS_SENDER_ID`, and unset `LUTIANO_OTP_DEV_CODE`: the API
refuses to start in live mode while a dev code is set. Codes and sign-in challenges are kept in Redis (`REDIS_PORT=6380` for the
local Docker container). To switch 2FA off locally, set `LUTIANO_MFA_ENABLED=false`.

| Account | Role | Use it to test |
|---|---|---|
| admin@lutiano.local | Administrator | Everything; acknowledges handovers made to the administrator |
| grace.ewane@lutiano.local | Administrator (second) | Approving refunds requested by admin@ (the second-approver rule) |
| inventory@lutiano.local | Inventory manager | Transfers, adjustments, stocktakes |
| joel.tchami@lutiano.local | Inventory manager | Acknowledging cash handovers made to him |
| seller@lutiano.local | Fixed POS barista (Bonapriso Cafe) | Seller app; has an open shift today |
| marie.tabe@, paul.nguena@ | Fixed POS baristas | Verified documents (Marie: eligible) |
| armel.nguema@, sandra.eto@ | Mobile baristas | Rejected ID document (Armel) |
| brice.fokou@, aicha.bello@ | Market agents | Market locations (no brewed drinks) |
| kevin.mbarga@lutiano.local | Mobile barista | No sales for 20 days: shows as an inactive seller |
| ruth.ngo@lutiano.local | Deactivated seller | Deactivation record |

All seller emails end in `@lutiano.local`.

## What is in the data

- **Locations:** 6 locations across Douala and Yaoundé, of every type. Marché Central Agent is deactivated.
- **Catalogue:**
  - 4 categories and 4 add-ons.
  - Products include Cappuccino and Arabica Beans with variants, and Filter Coffee, which isn't stock-tracked.
  - Mocha has a price change on record; the ready-to-drink coffee is deactivated.
- **Inventory:**
  - Supplier receipts, and weekly transfers through the full approval flow.
  - One delivery received short, which creates a stock discrepancy.
  - Damaged-stock adjustments, both applied and waiting for approval.
  - One approved stocktake, and one still being counted.
  - Transfers in every state: waiting for approval, approved, in transit, rejected, plus one requested by a seller.
- **Sales:** about 2,000 cash sales. Over 40% are linked to loyalty members, and some include add-ons.
- **Cash:**
  - One shift per seller per day, with cash outflows (approved, rejected and waiting).
  - Remittances by handover and bank deposit; most acknowledged, some disputed, and the last two days still outstanding.
  - Some shifts closed with a difference: accepted, recount requested, or waiting for review.
  - Today's shifts are still open.
- **Refunds:** every state: paid from a cash drawer or an advance, approved but unpaid, waiting for approval, rejected, cancelled. Some include returned stock.
- **Commissions and targets:**
  - Rules for every product and seller role. Drinks and pastries pay a percentage (4% fixed POS, 6% mobile, 8% market agents); beans and merchandise pay a fixed amount per unit.
  - Iced Latte for mobile baristas was changed to 7%, so it has a rate history.
  - Monthly value targets for every seller for last month and this month, plus two weekly per-unit product targets.
  - Manual adjustments, including a large deduction for Kevin that leaves a recovery balance.
  - Last month is finalised, so frozen statements and a carried recovery exist.
- **Loyalty:**
  - 32 members across the tiers.
  - Earning rules: category rates, a per-unit bonus, a first-purchase bonus, one past promotion and one scheduled promotion.
  - 7 rewards, one of them inactive. Redemptions, including some reversed.
  - Adjustments, some points expiring soon, and some already expired.
- **Payouts:**
  - Payout numbers in every state: verified, waiting for verification (Armel), rejected (Brice) and replaced (Aïcha, who now has a newer verified number).
  - Payouts of last month's earnings in every state. Marie is paid in two parts, cash then MTN MoMo. Paul has one payout reversed and another whose reversal waits for a second administrator. Sandra's is approved and waiting to be paid. Brice's waits for approval, and Aïcha has one rejected and one cancelled.
  - Paid payouts carry a provider reference and an evidence file.
  - The approval threshold is 0, so every payout needs a second administrator. Use admin@ and grace.ewane@ to try the approvals.
- **Notifications:** the Delivery Log (Notifications page, opened from the bell) fills as you use the system: payout and approval
  messages, cash and stock alerts, and the sign-in and payout-number codes. Grace Ewane and the demo administrator receive
  approval alerts; the others can be added in Notifications → Settings.

## API contract check (`contract/`)

Proves the API still answers exactly as before after a change that should not alter behaviour (for example the backend restructure).

```bash
cd infrastructure/dev/contract
ORACLE_JAR=/path/to/lutiano-api-baseline.jar python3 compare.py     # full check: GET responses by value, write flows and errors by shape
python3 enum_check.py                                               # values stored in the database vs the Java enums
```

`compare.py` starts the **original** build (a jar made before the change, port 8091) against the same database and Redis, snapshots it and the running
API (port 8080) with the same tokens, and diffs the two. `snapshot.py` calls every GET route as every role (anonymous, administrator, inventory manager,
the three seller roles, a customer); `flows.py` drives about 70 write and error cases (create and update records, an idempotent sale and order,
uploads, 400/401/403/404/405/409/413/415 responses) and compares the JSON shape. `diff.py` ignores the paths listed in `baseline/volatile.json`.
Principal tokens are cached for 6 hours in `baseline/.tokens.json` so that signing in does not change the data being compared.
The customer used is `+237690000099` (password `Coffee123!`), created once through the sign-up API. `flows.py` creates test records on every run.
