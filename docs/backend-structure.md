# Backend structure (`backend/lutiano-api`, package `com.lutiano.api`)

```
config/        AppProperties (every lutiano.* setting, typed) + AppPropertiesConfig, SecurityConfig, JwtConfig, CorsConfig,
               JacksonConfig, SchedulingConfig, OpenApiConfig, SearchHandlers (the search handler beans)
controller/<feature>/   *Controller: bind, authorise, return ResponseEntity<GenericApiResponse<T>> (downloads: ResponseEntity<byte[]>)
service/<feature>/      *Service: the rules, transactions, audit, notifications
repository/<feature>/   *Repository: all SQL (JdbcTemplate); References and SearchOutbox live here too
entity/                 typed rows shared across layers (EarningLine, PricedAddon); more tables move here over time
models/request/<feature>/   request bodies (public records); models/request/common: ReasonRequest, NoteRequest, ...; PageRequest
models/response/<feature>/  response records (same JSON keys as before); GenericApiResponse, PageResponse
definitions/enums/      Role, SearchType, RefundStatus, ... (names are the values stored in the database)
proxy/         calls to outside systems: SmsUtil, PushSender, SocialVerifier, ObjectStorage, ImageStorage, EvidenceStorage
security/      CurrentUser, CurrentCustomer, Actor, Roles
exception/     ApiErrors (maps exceptions to the error envelope)
utils/         Validate, Rows, Lists, Periods, Phones, SqlFilter, Candidates, Enums, Responses
```
Features: account, audit, auth, business, campaign, cash, catalogue, customer, dashboard, earnings, health, inventory, loyalty, media, notification,
operation, order, otp, payment, payout, product, reference, refund, reports, sales, search, staff.

## Rules (enforced by `ArchitectureTest`)
- Every endpoint returns a `ResponseEntity`; JSON is wrapped by `utils.Responses` (`ok`, `created`, `accepted`, `page`, `list`, `noContent`).
- Controllers never use repositories or `JdbcTemplate`; services never use controllers; repositories never use services or controllers.
- Request and response models are records.

## Responses are typed at the API boundary
Services and repositories still build row maps. `Responses.ok(row, XResponse.class)` converts the map to the endpoint's response record. Each record keeps
the original JSON keys (`@JsonProperty`) and has an `other` any-setter/any-getter so a key the sample missed is never dropped. About 143 endpoints (the
GETs that had sample data) have a record; the other ~180 (mostly writes) are wrapped as `Map<String, Object>` until a sample is recorded and
`infrastructure/dev/contract/typed_responses.py` is run again. Making repositories and services return typed records and entities directly is the next step, domain by domain.

## Proof that nothing changed
`infrastructure/dev/contract/compare.py` (see `infrastructure/dev/README.md`) diffs the running API against the build made before the restructure:
1,131 GET responses by value and 71 write/error flows by shape. `ApiContractTest` and `OpenApiContractTest` run in the normal test suite;
`packages/api-contracts/openapi.yaml` is generated (`UPDATE_OPENAPI=true ./gradlew test --tests '*OpenApiContractTest'`).
