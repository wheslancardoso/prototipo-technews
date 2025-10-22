# TechNews — Quick Start

Run a presentable MVP locally in minutes.

## Requirements
- Java 17+
- Maven 3.9+
- Internet access to download dependencies

## Build
- `mvn -q -DskipTests package`

## Run (Dev)
- `java -jar target/technews-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=8083`

## Access
- Home: `http://localhost:8083/`
- Newsletter demo: `http://localhost:8083/newsletter.html`
- H2 Console (if enabled): `http://localhost:8083/h2-console`

## Notes
- API details: see `README-API.md`
- MVP walkthrough: see `README-MVP.md`
- Mailgun smoke requires env vars: `MAILGUN_DOMAIN`, `MAILGUN_API_KEY`, `MAIL_FROM`, `MAILGUN_TEST_TO`
  - Use PowerShell loader: `./scripts/load-env.ps1`
- Ports: default config can be overridden via `--server.port` or `PORT` env

## Troubleshooting
- If a port is busy, change `--server.port` (e.g., `8080`).
- On Windows PowerShell, prefer separate commands (avoid `&&`).