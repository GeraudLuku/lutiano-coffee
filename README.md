# LUTIANO Coffee

Cash-sales platform for LUTIANO Coffee: an **Admin Web Portal**, a **Seller app** shared by the three seller roles, a **Customer app**, and a **Spring Boot API** behind them all. The same repository also holds the infrastructure code (Terraform, Ansible) that runs it on Oracle Cloud, and the CI pipelines.

## Table of contents

1. [Project at a glance](#project-at-a-glance)
2. [Repository layout](#repository-layout)
3. [Run it locally](#run-it-locally)
   - [Local services](#local-services)
   - [Backend](#backend--backendlutiano-api)
   - [Admin web](#admin-web--appsadmin-web)
   - [Seller app](#seller-app--appsseller-app)
   - [Customer app](#customer-app--appscustomer-app)
   - [Test data](#test-data)
4. [Configuration](#configuration)
5. [Git workflow and environments](#git-workflow-and-environments)
6. [CI pipelines](#ci-pipelines)
7. [Docker images](#docker-images)
8. [Infrastructure](#infrastructure)
9. [Documentation index](#documentation-index)
10. [Roadmap](#roadmap)

## Project at a glance

| Part | Technology | Folder |
|---|---|---|
| API | Java 21, Spring Boot, MariaDB, Redis, Elasticsearch, S3-compatible storage | [`backend/lutiano-api`](backend/lutiano-api) |
| Admin web portal | React, TypeScript, Vite | [`apps/admin-web`](apps/admin-web) |
| Seller app | Flutter | [`apps/seller-app`](apps/seller-app) |
| Customer app | Flutter | [`apps/customer-app`](apps/customer-app) |
| Shared Flutter code | Dart package (API client, theme, formatting, widgets) | [`packages/lutiano_core`](packages/lutiano_core) |
| API contract | OpenAPI file generated from the controllers | [`packages/api-contracts/openapi.yaml`](packages/api-contracts/openapi.yaml) |
| Infrastructure | Terraform, Ansible, k3s on Oracle Cloud | [`infrastructure`](infrastructure) |
| Product specs and plans | Markdown and Word files | [`docs`](docs) |
| Visual design boards | Image packs | [`design-assets`](design-assets) |

## Repository layout

```
lutiano-coffee/
├── apps/
│   ├── admin-web/          React admin portal (+ Dockerfile, nginx config)
│   ├── seller-app/         Flutter app for the seller roles
│   └── customer-app/       Flutter app for customers
├── backend/
│   └── lutiano-api/        Spring Boot API (+ Dockerfile)
├── packages/
│   ├── api-contracts/      openapi.yaml
│   └── lutiano_core/       Dart code shared by both Flutter apps
├── infrastructure/
│   ├── docker-compose.yml  Local MariaDB, Redis, storage, Elasticsearch, Kibana
│   ├── dev/                Test-data seeding and API contract checks
│   ├── terraform/          Oracle Cloud network, servers, load balancer, bastion
│   ├── ansible/            Server setup and the k3s cluster
│   └── bastion.sh          Opens SSH tunnels to the private servers
├── docs/                   Specs, plans, checklists
├── design-assets/          Design boards
├── .github/workflows/      CI pipelines
├── .env.example            Template for local settings (copy to .env)
└── README.md
```

## Run it locally

You need Docker, Java 21, Node.js and Flutter 3.27 or newer, depending on which part you work on.

### Local services

```bash
cp .env.example .env            # adjust ports if 3306/6379/9000 are taken
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d
```

This starts MariaDB, Redis, S3-compatible image storage (SeaweedFS), Elasticsearch and Kibana on the ports in `.env`. The API reaches storage through `S3_ENDPOINT` (default `http://localhost:9000`). In shared environments, point it at AWS S3, Cloudflare R2 or another S3-compatible service. Search is described in [`docs/search/README.md`](docs/search/README.md).

### Backend — [`backend/lutiano-api`](backend/lutiano-api)

```bash
cd backend/lutiano-api && ./gradlew bootRun
```

API base: `http://localhost:8080/api/v1`. Set `DB_URL` if MariaDB is not on `localhost:3306`. Flyway migrations run on start. Migration `V2` seeds demo users (`admin@lutiano.local`, `inventory@lutiano.local`, `seller@lutiano.local`, password `Coffee123!`). **Remove that seed before any shared environment.**

Health and metrics (Spring Boot Actuator) are on the separate management port `8081`, so they are never exposed with the public API.

Code is organised by layer, then feature (details in [`docs/backend-structure.md`](docs/backend-structure.md)): `controller/<feature>` (HTTP, role check, `ResponseEntity<GenericApiResponse<T>>`) → `service/<feature>` (business rules, transactions, audit) → `repository/<feature>` (SQL), with `models/` (request and response records), `entity/`, `definitions/enums/`, `proxy/` (SMS, push, storage), `config/` (typed `AppProperties`, security, OpenAPI), `utils/` and `exception/`. The API contract is [`packages/api-contracts/openapi.yaml`](packages/api-contracts/openapi.yaml), generated from the controllers.

Tests:

```bash
cd backend/lutiano-api && ./gradlew test
```

### Admin web — [`apps/admin-web`](apps/admin-web)

```bash
cd apps/admin-web && npm install && npm run dev     # http://localhost:5173
```

In development, set `VITE_API_BASE` (see `.env.example`) to point at another API. In the Docker image, the API address is read when the container starts (see [Docker images](#docker-images)), so one image works in every environment.

Structure: `api/` (HTTP client and one service per domain) · `types/` · `hooks/` · `utils/` · `components/ui` (generic) · `components/charts` · `components/domain` (LUTIANO-specific) · `layout/` · `pages/<feature>/`.

### Seller app — [`apps/seller-app`](apps/seller-app)

```bash
cd apps/seller-app && flutter pub get
flutter run --dart-define=LUTIANO_API=http://10.0.2.2:8080/api/v1
```

`10.0.2.2` is how an Android emulator reaches your computer. On the iOS simulator use `http://localhost:8080/api/v1`. For a plain-HTTP dev API on Android, allow cleartext traffic in the debug manifest. Release builds need the `INTERNET` permission in `android/app/src/main/AndroidManifest.xml`. Release steps are in [`docs/release-checklist.md`](docs/release-checklist.md), and the build plan in [`docs/seller-app-plan.md`](docs/seller-app-plan.md).

Structure: `core/` (config, API client, theme, formatting) · `data/` (models, repositories) · `state/` (session and cart controllers) · `widgets/` (shared UI) · `features/<screen group>/`.

### Customer app — [`apps/customer-app`](apps/customer-app)

```bash
cd apps/customer-app && flutter pub get
flutter run --dart-define=LUTIANO_API=http://10.0.2.2:8080/api/v1
```

Same address rule as the seller app. Screens live in `lib/features/` (sign-in, home, menu, cart, favourites, orders, notifications). What still needs decisions or accounts (payments, store listings, legal text) is in [`docs/customer-app-requirements.md`](docs/customer-app-requirements.md).

Both Flutter apps share [`packages/lutiano_core`](packages/lutiano_core).

### Test data

[`infrastructure/dev/seed_test_data.py`](infrastructure/dev/seed_test_data.py) fills a local database with about 45 days of realistic activity, created through the API so every ledger stays consistent. See [`infrastructure/dev/README.md`](infrastructure/dev/README.md) for the accounts and the steps.

## Configuration

All settings come from environment variables. Copy [`.env.example`](.env.example) to `.env` (which Git ignores) and change what you need. The main groups:

| Group | Examples |
|---|---|
| Database and cache | `DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT` |
| Search | `ELASTICSEARCH_URL`, `ELASTIC_PASSWORD`, `SEARCH_INDEXING_ENABLED` |
| Security | `LUTIANO_JWT_SECRET`, `LUTIANO_JWT_TTL_HOURS`, `LUTIANO_CORS_ORIGINS`, `LUTIANO_MFA_ENABLED` |
| SMS and push | `LUTIANO_SMS_MODE`, `HOMBEE_SMS_*`, `LUTIANO_FIREBASE_CREDENTIALS` |
| Sign-in providers | `LUTIANO_GOOGLE_CLIENT_IDS`, `LUTIANO_APPLE_AUDIENCES` |
| Server | `SERVER_PORT`, `MANAGEMENT_PORT` |
| Business rules | `LUTIANO_SELLER_REQUIRED_DOCUMENTS`, `LUTIANO_REFUNDS_SECOND_APPROVER`, `LUTIANO_ORDERS_ACCEPT_TIMEOUT_MINUTES` |

Never commit real secrets. A secret scan runs on pull requests (see [CI pipelines](#ci-pipelines)).

## Git workflow and environments

| Branch | Meaning |
|---|---|
| `main` | Production. Only reached through a pull request from `develop`. |
| `develop` | Staging and the default branch. Feature pull requests land here. |
| `<type>/<name>` | Short-lived work branches, for example `feat/…`, `fix/…`, `build/…`, `ci/…`, `infra/…`. |

Both long-lived branches are protected: changes arrive by pull request, with the required checks passing, and are squash-merged.

## CI pipelines

Workflows live in [`.github/workflows`](.github/workflows). Each one only runs when its own part of the code changes.

| Workflow | Runs when | What it does |
|---|---|---|
| [`backend.yml`](.github/workflows/backend.yml) | `backend/**` changes | Builds the API and runs its tests |
| [`admin-web.yml`](.github/workflows/admin-web.yml) | `apps/admin-web/**` changes | Installs, lints and builds the portal |
| [`flutter.yml`](.github/workflows/flutter.yml) | Flutter apps or the shared package change | Analyzes and tests the affected apps only |
| [`secrets.yml`](.github/workflows/secrets.yml) | Pull requests and pushes to `develop` and `main` | Scans for committed secrets (gitleaks) |
| [`codeql.yml`](.github/workflows/codeql.yml) | Pull requests, pushes to `develop` and `main`, and every Monday | Code security analysis |

Dependabot ([`dependabot.yml`](.github/dependabot.yml)) proposes dependency updates.

## Docker images

| Image | Source | Notes |
|---|---|---|
| API | [`backend/lutiano-api/Dockerfile`](backend/lutiano-api/Dockerfile) | Layered Spring Boot jar, non-root user, health check. Ports `8080` (API) and `8081` (management) |
| Admin web | [`apps/admin-web/Dockerfile`](apps/admin-web/Dockerfile) | nginx serving the built portal. Set `API_BASE_URL` when you run the container. It is written into `/config.js` at start, so the image does not change between environments |

```bash
docker build -t lutiano-api backend/lutiano-api
docker build -t lutiano-admin apps/admin-web
docker run -e API_BASE_URL=http://localhost:8080/api/v1 -p 8082:8081 lutiano-admin
```

Pushing images to Docker Hub from CI is planned (see [Roadmap](#roadmap)).

## Infrastructure

Production runs on Oracle Cloud's free tier: two private servers in a private network, one public network load balancer, and a bastion for SSH access. [`infrastructure`](infrastructure) holds everything that builds and configures it.

```
Internet ─► Load balancer ─► lutiano-1 (k3s server) ─┐
                             lutiano-2 (k3s agent)  ─┘ private network, no public IPs
You ─► Oracle Bastion ─► SSH tunnels ─► both servers
```

| Step | Folder | What it does | Guide |
|---|---|---|---|
| 1. Create the cloud resources | [`infrastructure/terraform`](infrastructure/terraform) | Network, two ARM servers, load balancer with a fixed public IP, bastion | [README](infrastructure/terraform/README.md) |
| 2. Open access to the private servers | [`infrastructure/bastion.sh`](infrastructure/bastion.sh) | Creates bastion sessions and opens tunnels on local ports 2201 and 2202 (SSH) and 6443 (Kubernetes API) | In both guides |
| 3. Configure the servers | [`infrastructure/ansible`](infrastructure/ansible) | Updates, SSH hardening, firewall, k3s cluster | [README](infrastructure/ansible/README.md) |

The order matters: Terraform first, then the tunnels, then Ansible. Local development uses [`infrastructure/docker-compose.yml`](infrastructure/docker-compose.yml) instead of any of this.

## Documentation index

| Document | About |
|---|---|
| [`docs/backend-structure.md`](docs/backend-structure.md) | Backend layers and where code goes |
| [`docs/search/README.md`](docs/search/README.md) | How Elasticsearch search works with the database |
| [`docs/seller-app-plan.md`](docs/seller-app-plan.md) | Seller app phases and status |
| [`docs/customer-app-requirements.md`](docs/customer-app-requirements.md) | What the customer app needs, and open decisions |
| [`docs/release-checklist.md`](docs/release-checklist.md) | Seller app release steps |
| [`docs/design-fidelity-report.md`](docs/design-fidelity-report.md) | Admin portal against the design boards |
| [`infrastructure/dev/README.md`](infrastructure/dev/README.md) | Test data and API contract checks |
| [`infrastructure/terraform/README.md`](infrastructure/terraform/README.md) | Cloud resources, login, state, bastion |
| [`infrastructure/ansible/README.md`](infrastructure/ansible/README.md) | Server setup and k3s |
| `docs/*.docx` | The original product and operating specifications |

## Roadmap

Done: CI for every part, Actuator health and metrics, Docker images for the API and admin web, Terraform for Oracle Cloud, Ansible and a two-node k3s cluster.

Next, in order:
1. Build and push images to Docker Hub from CI once the other checks pass.
2. Install Argo CD on the cluster, so the cluster pulls its desired state from Git.
3. Add platform services through Argo CD: monitoring, a private secrets vault, a container registry.
4. Deploy the data services and the API and admin web, with staging and production separated.
5. Get a domain and turn on HTTPS.
