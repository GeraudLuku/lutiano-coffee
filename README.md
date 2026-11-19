# LUTIANO development workspace

Cash-sales MVP for LUTIANO Coffee: an Admin Web Portal, a Seller app shared by the three seller roles,
and a Spring Boot API. The Customer app is still a placeholder. Product specs are in `docs/`, the visual
design boards in `design-assets/`, and the API contract in `packages/api-contracts/openapi.yaml`.

## Local services

```bash
cp .env.example .env            # adjust ports if 3306/6379/9000 are taken
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d
```

MariaDB, Redis and S3-compatible image storage (SeaweedFS) start on the ports in `.env`.
The API reaches storage via `S3_ENDPOINT` (default `http://localhost:9000`); point it at AWS S3,
Cloudflare R2 or another S3-compatible service in shared environments.

## Backend — `backend/lutiano-api` (Java 21, Spring Boot)

```bash
cd backend/lutiano-api && ./gradlew bootRun
```

API base: `http://localhost:8080/api/v1`. Set `DB_URL` if MariaDB is not on `localhost:3306`.
Flyway migrations run on start; `V2` seeds demo users (`admin@lutiano.local`, `inventory@lutiano.local`,
`seller@lutiano.local`, password `Coffee123!`) — remove that seed before any shared environment.

Code is organised by layer, then feature (see `docs/backend-structure.md`): `controller/<feature>` (HTTP, role check, `ResponseEntity<GenericApiResponse<T>>`)
→ `service/<feature>` (business rules, transactions, audit) → `repository/<feature>` (SQL), with `models/` (request and response records), `entity/`,
`definitions/enums/`, `proxy/` (SMS, push, storage), `config/` (typed `AppProperties`, security, OpenAPI), `utils/` and `exception/`.
The API contract is `packages/api-contracts/openapi.yaml`, generated from the controllers.

## Admin Web — `apps/admin-web` (React + TypeScript + Vite)

```bash
cd apps/admin-web && npm install && npm run dev     # http://localhost:5173
```

Set `VITE_API_BASE` (see `.env.example`) to point at another API.
Structure: `api/` (HTTP client + one service per domain) · `types/` · `hooks/` · `utils/` ·
`components/ui` (generic) · `components/charts` · `components/domain` (LUTIANO-specific) ·
`layout/` · `pages/<feature>/`.

## Seller app — `apps/seller-app` (Flutter ≥ 3.27)

The project has no platform folders yet. Create them once, then run:

```bash
cd apps/seller-app && flutter create --platforms=android,ios . && flutter pub get
flutter run --dart-define=LUTIANO_API=http://10.0.2.2:8080/api/v1
```

For a plain-HTTP dev API on Android, allow cleartext traffic in the debug manifest; release builds
need the `INTERNET` permission in `android/app/src/main/AndroidManifest.xml`.
Structure: `core/` (config, API client, theme, formatting) · `data/` (models, repositories) ·
`state/` (session and cart controllers) · `widgets/` (shared UI) · `features/<screen group>/`.
