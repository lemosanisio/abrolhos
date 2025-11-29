# Copilot / Gemini repository instructions for `abrolhos`

Purpose
- Provide context and usage guidance to AI coding assistants (Copilot, Gemini) when generating or suggesting code for this repository.
- Respect `.aiexclude` and do not touch excluded files unless explicitly requested.

Quick repository summary
- Language: Kotlin (JVM) using Kotlin 1.9.23
- Framework: Spring Boot 3.4.8
- Java toolchain: Java 21
- Build: Gradle (Kotlin DSL) — `build.gradle.kts`, `settings.gradle.kts`
- ORM / DB: Spring Data JPA, Flyway migrations, PostgreSQL runtime
- Templating: Thymeleaf, HTMX integration
- Static analysis: Detekt (config at `config/detekt/detekt.yml`)

Where to focus
- Source code: `src/main/kotlin/` (main application packages under `br.dev.demoraes`)
- Tests: `src/test/kotlin/`
- Configuration: `src/main/resources/application.yml`, `local.env` (local secrets — excluded)
- Database migrations: `src/main/resources/db/migration` (migrations are authoritative — avoid changing unless requested)
- Docker compose: `container/docker-compose.yml` (local dev environment)
# .aiexclude — AI indexing and completion exclusions for this repository
Local build and test commands (use these when producing or verifying changes)
- Build: `./gradlew build`
- Run application locally: `./gradlew bootRun`
- Run tests: `./gradlew test`
- Run detekt: `./gradlew detekt`
- Run Flyway migrations (if needed by commands/tasks in Gradle): `./gradlew flywayMigrate`
- Docker compose (local infra): `docker compose -f container/docker-compose.yml up --build`

Coding conventions and expectations
- Always produce Kotlin code, prefer idiomatic Kotlin (use data classes, null-safety, immutable vals when appropriate).
- Use constructor injection for Spring beans (primary constructor + `@Autowired` not required).
- Prefer repository/service/controller layering consistent with Spring Boot patterns.
- Follow existing project structure and package naming: `br.dev.demoraes`.
- Tests: use JUnit 5 (JUnit Platform) and Kotlin test support found in the project.
- Static analysis: ensure new code passes Detekt rules; reference `config/detekt/detekt.yml` for hints.

Security and secrets
- Do NOT leak or suggest content from `local.env`, `.env`, or other excluded files.
- Avoid hardcoding credentials, API keys, or secrets in suggested code.
- If a change requires secrets for verification, provide instructions on how to create placeholder values and where to store them locally (e.g., `local.env`) rather than embedding real secrets.

When changing code
- Provide a short summary of the change (one or two sentences).
- List files to be changed and the purpose of each change.
- Include a minimal patch or code snippet with context (do not rewrite unrelated files).
- Add or update unit tests for new behavior. At minimum, include one happy-path test and one edge-case test when applicable.
- Run `./gradlew build` and `./gradlew test` locally and report the results. If you can't run them, state why and provide the commands the user should run.

What not to modify
- Do not alter generated files, build outputs, or binary artifacts (anything under `build/`, `out/`, or `kotlin/` cache directories).
- Do not edit Flyway migration files unless explicitly requested — migrations are a source of truth for the DB schema.
- Do not change `local.env`, `.aiexclude`, `copilot-instructions.md` unless asked.

How to present suggestions
- When proposing code changes, follow this structure:
  1. Summary: 1–2 sentence description of the change and rationale.
  2. Files changed: bullet list of file paths with short notes.
  3. Patch: concise unified diff or code block for each changed file.
  4. Tests: list of tests added/modified and instructions to run them.
  5. Verification: commands the user should run locally (e.g., `./gradlew build && ./gradlew test`) and expected outcomes.

Helpful shortcuts for reviewers
- If a change requires running the application, recommend using `./gradlew bootRun` and, when DB is needed, the provided `container/docker-compose.yml`.
- Prefer small, well-tested PRs that add one clear behavior or fix one bug.

Fallback guidance
- If unsure how to implement a feature, propose multiple options (short pros/cons) and indicate which files would change for each option.
- When suggesting a database change, always include a Flyway migration or instructions to create one.

Respect `.aiexclude`
- This repository contains a `.aiexclude` that lists sensitive and generated files. Do not read from or make suggestions that reference files matched by `.aiexclude` unless the user explicitly requests it.

Contacting the maintainer
- If a suggestion requires privileged access (secrets, DB dumps, or files excluded by `.aiexclude`), ask the user to provide sanitized input or grant temporary access.

End of instructions — follow these guidelines when producing code or suggestions for this repository.
# Keep sensitive, generated, or large binary files out of AI models and suggestions.

# Version control and IDE metadata
.git/
**/.git/
.idea/
*.iml
.vscode/

# Build outputs and caches (Gradle, Kotlin, JVM artifacts)
build/
**/build/
.gradle/
.gradle/**
out/

# Gradle wrapper caches and generated files
.gradle/
**/libs/
**/*.jar
**/*.war
**/*.ear
**/*.class
**/*.kotlin_module

# Kotlin/IDE/compiler caches and temporary build artifacts
kotlin/
kotlin/**
**/tmp/
**/tmp/**

# Local environment, secrets, and keystores
local.env
.env
.env.*
*.keystore
*.jks
*.p12
secrets.json
*.pem

# Logs, crash dumps, and diagnostic files
*.log
hs_err_pid*.log
container/hs_err_pid*.log
build/**/hs_err_pid*.log

# OS and editor cruft
.DS_Store
Thumbs.db

# Docker / Compose generated containers and data (local-only)
**/docker-compose.override.yml
**/docker-compose.*.override.yml
**/data/**

# Binary assets, large files and generated reports
**/*.db
**/*.sqlite
**/*.sqlite3
**/*.tar
**/*.gz
**/*.zip
**/*.7z
**/*.bin

# Tests / reports that are generated and not useful for model training
**/reports/
**/reports/**
**/tmp/**

# Explicitly exclude build artifacts found in this repo
build/libs/
build/tmp/

# Please do not index or suggest changes for these excluded paths.
# If you need an excluded file for a task, request explicit access in the conversation.
