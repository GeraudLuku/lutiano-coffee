---
name: backend-review
description: Review the Spring Boot backend (backend/lutiano-api) for separation of concerns, duplication, modularity, simplicity, consistency, logging and maintainability, then fix the real problems without changing behaviour. Use when asked to "review the backend", "verify the code quality", "check for duplication", or before starting a new module.
---

# Backend review

Review the existing backend before adding to it, fix what is genuinely wrong, and report honestly. The goal is clean code, not more code.

## What to judge, in priority order

1. **Separation of concerns:** controllers (`controller/<feature>`) only bind and authorise and return `ResponseEntity<GenericApiResponse<T>>`, services (`service/<feature>`) hold rules, repositories (`repository/<feature>`) hold SQL. No SQL in services, no business rules in controllers. `ArchitectureTest` enforces the layering; see `docs/backend-structure.md` for where each kind of class lives (models, entity, definitions/enums, proxy, config, utils).
2. **Reusability:** one place for each shared job (references, validation, paging, images, evidence files, phone numbers, notifications). Check a helper does not already exist before approving a new one.
3. **Modularity:** each package is one domain with few links to others; no circular dependencies.
4. **Simplicity:** no interface with one implementation, no factory, no new package for one class, no abstraction that does not serve a real purpose.
5. **Consistency:** naming, package layout, `Validate` helpers for errors, the `GenericResponse` envelope, `PageRequest` paging with the unique tie-breaker in every ORDER BY.
6. **Logging:** minimal. Only unexpected errors, provider calls and swallowed failures. Never the same fact in two layers. Never secrets, codes, or unmasked phone numbers.
7. **Maintainability and single responsibility:** short methods, focused classes. Flag files over about 300 lines and methods that do unrelated things.

## Rules while fixing

- Preserve business logic and API contracts. Do not touch working code without a reason you can state.
- Do not introduce abstractions for their own sake. Do not create classes, interfaces or utility packages without a real need.
- Never duplicate existing functionality; reuse it.
- A refactor that is large or touches working behaviour is reported, not done, unless the user asks.

## Process

1. **Measure first** (from `backend/lutiano-api/src/main/java/com/lutiano/api`):
   - Largest files: `wc -l */*.java | sort -rn | head`
   - Unused imports: for each `import x.Y;` check `Y` is used in the file.
   - Inline package paths: `grep -rnE "com\.lutiano\.api\.[a-z]+\.[A-Z]|java\.util\.[A-Z]"` excluding import and package lines.
   - Logging: `grep -rn "log\.\(info\|warn\|error\)"` counted per file.
   - Duplicated helpers: `grep -rn "private.*lockIn\|private void event("` and similar copies across services.
   - Swallowed exceptions: `grep -rn "catch (\(Runtime\)\?Exception"`.
2. **Read** the files these point at, and any module added since the last review, in full.
3. **Fix** clear, safe problems (unused imports, inline paths, copy-pasted blocks inside one class, wrong layer). Keep each change small.
4. **Verify no regression:** `./gradlew compileJava -q`; restart the API (`preview_stop`, then `preview_start lutiano-api`); call the main read endpoints for the modules touched and a few unrelated ones, plus the workflow checks for the touched module. There is no automated test suite, so say so.
5. **Report.**

## Report format

- **Fixed:** each change in one line, and whether behaviour changed (it should not).
- **Checked and fine:** the criteria that passed, with the evidence (counts, sizes).
- **Found, not fixed:** real problems you chose not to change, with the reason and a recommendation.
- **Not covered:** what could not be verified (no tests, no live provider, untested screens).

State outcomes plainly. If something failed or was skipped, say so.
