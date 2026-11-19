# Who can find what

Elasticsearch narrows candidates with `SearchPolicy.filter`; MariaDB then confirms each one with `SearchPolicy.authorize` (one batched query per type), so a revoked assignment or a deactivated record disappears immediately, before the outbox catches up. A type with no policy, or an unknown role, matches nothing.

| Type(s) | Administrator | Inventory manager | Seller | Public |
|---|---|---|---|---|
| PRODUCT, CATEGORY, ADDON | all | all | active | active |
| OPERATION (location) | all | all | assigned, active | active |
| SALE | all | all | **own only** (a shared location grants nothing) | none |
| PAYMENT, REFUND, STAFF, CUSTOMER, PAYOUT, PAYOUT_DESTINATION, TARGET, COMMISSION_RULE, EARNING_RULE, REDEMPTION, POINTS_ADJUSTMENT, DELIVERY | all | none | none | none |
| CASH_OUTFLOW, CASH_REMITTANCE | all | none | own | none |
| STOCKTAKE, ADJUSTMENT, TRANSFER | all | all | at assigned locations | none |
| DISCREPANCY | all | all | none | none |
| REWARD | all | all | active | active |

Customer self-service search (a customer's own sales and redemptions) is designed for (`perm.customerId`) but has no endpoint until customer sign-in is specified.

Phone and email: only administrators, by exact match, 20 lookups per minute (`ContactLookup`).
