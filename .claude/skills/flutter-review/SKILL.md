---
name: flutter-review
description: Review the whole Flutter Seller app (apps/seller-app) after a phase or track for reusability, simplicity, modularity, architecture, state and lifecycle, design fidelity, localisation, accessibility and UX resilience, fix the safe problems without changing behaviour, verify on the iOS simulator, and report. Use when asked to "review the Flutter app", "review the seller app", "audit the Flutter code", or after a seller-app phase is built.
---

# Flutter review

Review the whole app after a phase, fix what is genuinely wrong and safe, verify, and report honestly. The goal is a maintainable app, not more code. Act as a critical senior Flutter architect: code that compiles and works is not automatically approved.

## Decision priority (applies to every judgement)

1. Correctness and security.
2. Simplicity.
3. Maintainability.
4. Reusability.
5. Modularity.
6. Extensibility, only where justified.
7. Performance, based on actual requirements.

Never add complexity to gain theoretical reusability or modularity. Do not trade away security, correctness or testability for simplicity.

## Judge the project against its own conventions, not a template

This project is deliberately pragmatic: `ChangeNotifier` controllers (`AppState`, `CartController`) shared through an inherited scope (`AppScope`), one `ApiClient` over the `http` package that unwraps the `GenericResponse` envelope, repositories per API area, `Navigator` routes, feature folders under `lib/features/`. These are sound for the app's size.

The usual "recommended" stack (Riverpod, Dio, GoRouter, Freezed, Clean Architecture with use cases, MVVM) is a **reference, not a requirement**. Judge whether the current approach is used consistently and appropriately. A different package is never a finding by itself. If the app has outgrown its approach (for example scattered route strings, or state that several features mutate), report it as a **recommendation with the reason**, severity INFO or MEDIUM, and **never migrate** state management, networking or navigation as part of a review. Introduce use cases or extra layers only where logic is complex, reused, or spans several repositories; never for simple reads.

## What to judge

### 1. Reusability and duplication
Look for duplicate business logic, API calls, widgets, validation, data transformation, utilities, constants and configuration, and for two implementations of one responsibility.
- Search for an existing implementation before accepting a new one (`lib/widgets`, `lib/core`, other features).
- Reuse shared widgets and utilities; extract only what is genuinely reusable; keep feature logic in its feature.
- **Accidental vs intentional duplication:** two similar pieces with different business responsibilities must not be merged. Do not create a generic abstraction to save a few lines.
- Do not create shared dependencies that couple features without need.

### 2. Simplicity
Flag unnecessary abstraction layers, interfaces with one implementation, inheritance, design patterns, dependency-injection machinery, use cases that only forward a call, over-fragmented files, third-party dependencies that the SDK or an existing one covers, over-complicated state, premature optimisation, speculative configuration. Before recommending an abstraction ask: does it solve a real problem now, does it improve maintainability or testability, does it add complexity, would something simpler do?

### 3. Modularity and separation of responsibilities
- Each feature owns its functionality (`features/<name>`); dependency direction is features, then shared (`widgets`, `core`, `data`), never feature to feature internals; no circular imports.
- A feature exposes only what others need; `core` and `widgets` must not become a dumping ground (each shared item has a clear owner and at least two users).
- Widgets render and call controller or repository methods; no HTTP, JSON maps, SQL-like logic or business rules in widgets; no raw JSON outside `data/`.
- Responsibilities are separated enough that a feature can be tested without the others.

### 4. Architecture and data
- Presentation, state and data layers are distinguishable; API models parsed and validated in `data/`; UI state immutable where practical; one source of truth for each piece of state; no duplicated state.
- Errors flow: data source, repository, `ApiException` (a message safe to show), controller, screen. No swallowed exceptions, no raw exception text or tokens shown or logged, no generic `catch` without handling.
- Networking: one configured client, timeouts, token handling, expired-session handling, no secrets or production URLs in code (`AppConfig` and `--dart-define` only).
- Security: token storage, no sensitive logging, input validation, UI hiding is never authorisation (the server is authoritative).
- Navigation: route definitions and arguments not scattered as ad-hoc strings; unknown-route and signed-out handling.

### 5. State, lifecycle and async (Flutter-specific)
- Every `TextEditingController`, `FocusNode`, `AnimationController`, `ScrollController`, stream subscription and listener is disposed.
- No `setState` or `context` use after an `await` without a `mounted` check.
- No network calls, parsing or heavy work inside `build()`; loaders keyed so they do not re-run on every rebuild; rebuild scope is as small as is practical; `const` constructors where possible; lists use lazy builders with pagination (the API paginates; default page size 5, max 200).
- Idempotency keys reused for retries (sales), double-tap protection on submit buttons, no race between overlapping loads.

### 6. Design fidelity
- Compare each built screen with its board in `design-assets/LUTIANO_Seller_and_Customer_PWA_Design_Pack/Seller_Operations_PWA/` (overview `00_*` and batches `01` to `08`). Check layout, sections, labels, order and states, using the iOS simulator screenshots. Boards are layout and style references only.
- **Never hardcode board sample data** (names, amounts, points, targets, thresholds). Every number and name comes from the API or the signed-in user. Search for suspicious literals and fix or report them.
- Missing boards (batch 04 Cash) are reported as not assessable, not as failures.

### 7. Localisation and accessibility
- Strings: French (default) and English. Report hardcoded user-facing strings; after the l10n setup exists, every string is in both ARB files, and dates, numbers and XAF amounts use the shared formatters.
- Accessibility: semantic labels on icon-only buttons and images, tap targets at least 44 pt (48 dp), text scales with the system setting without overflow, sufficient contrast (the yellow on white and the navy palette), meaningful focus order, no information conveyed by colour alone.

### 8. UX states and resilience
- Every screen that loads data has loading, empty and error (with retry) states and behaves on a slow or failed network.
- Expired or revoked sessions return to sign-in without crashing; staff accounts are refused with a clear message.
- Sales cannot be recorded twice (idempotency); nothing completes offline silently.
- Destructive or money actions confirm first and show the result.

### 9. Testing
Check presence and quality, not file counts: unit tests for controllers and repositories, widget tests for screens and their loading, empty and error states, an integration test for each critical flow. Business logic testable without the UI; success and failure paths covered.

### 10. Dart and Flutter standards
Effective Dart, null safety, `final` and `const`, small focused classes and methods, no god widgets or god controllers, no deeply nested conditionals, meaningful names, no leftover `print`, no unused code or imports, lints clean.

## Rules while fixing

- **Fix clear, safe problems** (unused imports and code, a missing `dispose`, a missing `mounted` check, a copy-pasted block inside one file, a hardcoded string or colour that has a shared home, a raw `Navigator` call where a helper already exists). Keep each change small and behaviour-preserving.
- **Report, do not do:** large refactors, anything touching working money or stock flows beyond a clearly safe fix, state-management or networking or navigation migrations, new layers, new dependencies, anything needing a product decision.
- Preserve business logic and API contracts. Do not modify unrelated files. Do not add a dependency without stating why.
- Never invent a file path, class, method or problem. Separate **confirmed** findings (you opened the code) from **needs inspection**.
- Do not report a problem from a folder name alone; inspect the implementation.

## Process

1. **Discover** (from `apps/seller-app`): read `pubspec.yaml`, `analysis_options.yaml`, `lib/main.dart`, `lib/app.dart`, `lib/state/`, `lib/core/`, `lib/data/`, the shell and routes, `test/`, `integration_test/`. Record the real Flutter and Dart versions (`flutter --version`) and the actual state, networking and navigation approach. Only report what you inspected.
2. **Measure** (whole app, every time):
   - `flutter analyze` and `dart format --output=none --set-exit-if-changed lib test`
   - Largest files: `find lib -name '*.dart' | xargs wc -l | sort -rn | head -15`. Flag files over about 300 lines and widgets or methods doing unrelated jobs.
   - Layering leaks: `grep -rn "http\.\|jsonDecode\|Map<String, dynamic>" lib/features lib/widgets` (should be only in `lib/data` and `lib/core`).
   - Lifecycle: list each `Controller(`, `FocusNode(`, `AnimationController(`, `.listen(`, `addListener(` and check the matching `dispose`/`remove`/`cancel`; `grep -rn "await" lib/features` near `setState(` and `context` for missing `mounted`.
   - Work in `build()`: `grep -rn "Future\|\.then(" ` inside `build` methods; loaders not keyed.
   - Hardcoded user text: `grep -rnE "Text\\(\s*'[A-Za-z]" lib/features lib/widgets` (count per file) and any ARB files.
   - Hardcoded sample data and colours: `grep -rnE "Color\\(0x|Colors\\.[a-z]+" lib/features` versus `core/theme.dart`; suspicious literals such as names, `XAF`, point values.
   - Duplicates: repeated widget trees, repeated formatting code, repeated request code across repositories; `grep -rn "class .*Repository"` and look for overlapping responsibilities.
   - Swallowed errors: `grep -rn "catch (" lib` and `catch (_)`; `grep -rn "print(" lib`.
   - Navigation: `grep -rnc "Navigator\." lib/features` and ad-hoc route strings.
   - Tests: `find test integration_test -name '*_test.dart'`, then read what they assert.
3. **Read** the files these point at, and every screen group added or changed by the phase, in full.
4. **Compare with the boards** for the screens the phase covers (see section 6), after a simulator run.
5. **Fix** the safe problems, one small change at a time.
6. **Verify** (all three, in this order; say plainly if one could not run):
   - `flutter analyze` and `flutter test` must pass.
   - **iOS integration run** against the live API. The API must be running (`preview_start lutiano-api`, health at `http://localhost:8080/api/v1/health`) and a simulator booted (`xcrun simctl boot "iPhone 17"`). Then:
     `flutter test integration_test -d <simulator-udid> --dart-define=LUTIANO_API=http://localhost:8080/api/v1`
   - **Screenshots** of the screens the phase touched: `flutter run -d <udid> --dart-define=LUTIANO_API=http://localhost:8080/api/v1` in the background, then the simulator tool (`attach`, `screenshot`, `tap`). Notes from experience: a debug build cannot be launched standalone from the simulator (blank screen), use `flutter run`; typing through the simulator tool drops characters in Flutter text fields, so for sign-in and forms prefer the integration test and keep manual typing to short strings; tap coordinates are in points (402 x 874 on iPhone 17), screenshots come back about 3x larger. Seeded seller: `seller@lutiano.local`, password `Coffee123!`.
   - If a phase added backend endpoints, say that this review did not cover them and recommend the `backend-review` skill.
7. **Report.**

## Findings: required fields

Every finding gives: **file path and class or method**; **description**; **why it matters** (impact on maintainability, correctness, security or UX); **principle violated** (reusability, simplicity, modularity, separation of concerns, lifecycle, fidelity, accessibility, resilience, testing); **recommended correction**; **severity**.

Severity: **CRITICAL** security flaws, threats to data integrity or money correctness, crashes on common paths. **HIGH** major architectural violation, serious coupling, a leak or lifecycle bug that will bite, missing error handling on a money or stock flow. **MEDIUM** duplication, inconsistency, testing gaps, missing states. **LOW** readability and small quality items. **INFO** optional suggestions and stack observations.

## Report format

# Flutter review: <phase or date>

1. **Executive summary:** overall assessment in a few lines, the most important findings, and whether the app is ready for the next phase.
2. **Project overview:** Flutter and Dart versions, state, networking, navigation, structure, test inventory. Inspected facts only.
3. **Assessment table:** one row per area (structure, feature separation, presentation, data layer, state management, dependency wiring, API integration, navigation, error handling, security, state and lifecycle, design fidelity, localisation, accessibility, UX states, testing) with **PASS**, **NEEDS IMPROVEMENT**, **FAIL** or **NOT ASSESSED**, and the evidence (counts, file sizes, commands run).
4. **Fixed:** each change in one line, and whether behaviour changed (it should not).
5. **Findings not fixed:** grouped by severity, each with the required fields; mark confirmed versus needs inspection.
6. **Reuse, simplicity and modularity:** the three-row table (dimension, verdict, evidence), noting accidental versus intentional duplication that was left alone and why.
7. **Testing assessment:** what is covered, what is missing, which flows lack an integration test.
8. **Remediation roadmap:** critical fixes, architectural corrections, code quality, testing, performance, optional enhancements, in priority order; never recommend a rewrite without strong justification.
9. **Not covered:** what could not be verified (no device features such as camera or push, Android not run, backend not reviewed, boards missing, screens not opened).

State outcomes plainly. If something failed or was skipped, say so. Do not call something verified unless you ran it.
