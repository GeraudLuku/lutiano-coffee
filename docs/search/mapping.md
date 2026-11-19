# Index mapping

File: `backend/lutiano-api/src/main/resources/search/index-mapping.json`. One index per generation (`lutiano-search-<timestamp>`) behind the alias `lutiano-search`; `dynamic: strict` at every level.

- `title`: analysed text, accent-folded, French and English. Sub-fields `title.autocomplete` (edge n-grams, as-you-type only) and `title.stem` (light French and English stemming, lower weight).
- `subtitle`, `content`: analysed text (`content.stem` too).
- `reference`: keyword with a lowercase and accent-folding normalizer, for exact and prefix matches on references, SKUs and codes. `tags`: keyword, for variant SKUs and linked references.
- `meta`: a closed object of typed filter fields: `status category kind method channel tier role city region locationType provider event eventType operationId sellerId customerId productId` (keyword), `sortOrder` (integer), `enabled` (boolean), `createdAt updatedAt` (date).
- `perm`: `roles`, `sellerId`, `customerId`, `operationIds`, `public`, `status`. Used to narrow candidates; never trusted for the final answer.
- Fuzziness (`AUTO`, `prefix_length` 1) applies only to `title`, `title.stem`, `subtitle` and `content`, never to references, SKUs or codes.

Field allow-lists per type are in `SearchHandlers.java`. Adding a `meta` key means editing the mapping and reindexing.
