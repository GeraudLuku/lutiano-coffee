# Search (Elasticsearch)

MariaDB is the source of truth. Elasticsearch only finds things: it proposes ids, and MariaDB decides what the caller may see.

## How it fits together

```
write (repository) ──► search_outbox row (same transaction)
                              │  every 2 s, bulk, retries with backoff
                              ▼
        SearchIndexer ──► alias lutiano-search ──► index lutiano-search-<timestamp>
                                                        ▲
 search box / list text box ──► SearchService ──────────┘ (candidates)
                                   │
                     SearchPolicy.authorize (MariaDB, batched)  ◄── authority
```

- **Outbox:** `V23__search_outbox.sql`. A repository calls `search.changed(TYPE, id)` after its write. Repeated changes to one record coalesce. The auto-increment id is the revision: documents are written with `external_gte` versioning, so a late or repeated change never overwrites newer data.
- **Documents** are rebuilt from current MariaDB state by a `SearchHandler` (`SearchHandlers.java`), never from the event.
- **Reads** go through `SearchService`. Results, counts and highlights come only from items re-verified by their type's `SearchPolicy`.
- **Lists:** the text box on 10 list pages asks `SearchCandidates` for ids (up to 5,000). The list keeps its own filters, permissions, sorting, paging and totals. A phone, email or provider reference typed in is matched exactly in the database (those are never indexed). More than 5,000 matches sets `truncated` on the page.
- **If Elasticsearch is down:** search endpoints and list pages with a text term return 503 `Search is unavailable`. Everything else keeps working and changes queue in the outbox.

## API (`/api/v1`)

| Call | Who |
|---|---|
| `POST /search`, `POST /search/{type}`, `GET /search/suggestions` | signed-in staff |
| `POST /catalogue/search` | public: active products, categories, add-ons, locations |
| `GET /admin/search/status`, `POST /admin/search/reindex`, `POST /admin/search/retry-failed` | administrators |

Limits: query text 100 characters; page size max 200; per-user Redis rate limits (60 searches, 120 suggestions, 20 exact contact lookups per minute; 30 per minute per IP for the public catalogue).

## Operations

- Start: `docker compose up -d elasticsearch elasticsearch-setup kibana` in `infrastructure/`. Elasticsearch and Kibana are pinned to 8.15.5 to match the Java client. Heap is 512 MB: a development size, not a production one.
- Accounts: the app uses `lutiano_app` (a role limited to `lutiano-search*`). Kibana uses `kibana_system`. The `elastic` superuser is used only by the one-shot setup container. Passwords come from `infrastructure/.env` (see `.env.example`).
- TLS: local runs use password authentication over plain HTTP on localhost-bound ports. In production, enable HTTPS in Elasticsearch and point `ELASTICSEARCH_URL` at it.
- **Reindex** (after a mapping change or to repair): `POST /admin/search/reindex`. It records the outbox watermark, fills a new index, replays changes made meanwhile, checks counts, switches the alias, and deletes all but the previous index (kept for rollback). Progress is in `GET /admin/search/status`.
- **Failed changes:** `GET /admin/search/status` shows pending and failed counts; `POST /admin/search/retry-failed` re-queues them. Done rows are purged after 7 days, failed rows after 30.
- **Mapping change:** edit `src/main/resources/search/index-mapping.json` (it is `dynamic: strict`, so a new field must be added there first), then reindex.
- Kibana: http://localhost:5601, sign in as `elastic`. Data survives container restarts (volume `elasticsearch_data`).

## Adding a type

1. A `SearchHandler` bean in `SearchHandlers.java` (allow-listed fields only; add any new `meta` key to the mapping).
2. A `SearchPolicy` in `SearchPolicies.java` (`TablePolicy` for active-flag tables, `OwnedPolicy` for owned or back-office rows).
3. `search.changed(TYPE, id)` after each write in the repository, and for records that embed another record's name, re-queue the dependents (see `StaffRepository.update`).
4. A policy test in `SearchPolicyTest`.

## Not indexed, on purpose

Phone numbers, emails, amounts and balances, passwords, tokens, and any message that carries a one-time code. Notification templates are not indexed (a composite key and about 34 rows; the Messages tab already filters them).
