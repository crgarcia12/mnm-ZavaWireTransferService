# Configuration & Externalized Settings Inventory

This inventory documents configuration sources and runtime settings used by ZavaWireTransferService.

## Configuration Sources

| Source | Type | Path/Location | Notes |
|---|---|---|---|
| wire-transfer.properties | Application properties | src/main/resources/wire-transfer.properties | Default DB, service, and RabbitMQ values |
| Environment variables | Runtime overrides | Process environment | Overrides property defaults in `WireConfig` |
| build.gradle | Build configuration | build.gradle | Java/WAR plugins and dependencies |
| web.xml | Servlet configuration | src/main/webapp/WEB-INF/web.xml | Servlet registrations and URL mappings |
| Dockerfile | Container runtime config | Dockerfile | Base images, exposed port, environment defaults |

## Build Profiles

| Profile | Activation | Purpose | Key Dependencies/Plugins |
|---|---|---|---|
| default Gradle build | `gradle war` | Build deployable WAR | `java`, `war` plugins |

## Runtime Profiles

| Profile | Activation Method | Config Files | Key Overrides |
|---|---|---|---|
| default | implicit | wire-transfer.properties | DB host/port/name/user/password |
| env override mode | env vars | process environment | DB, external service URLs, RabbitMQ settings |

## Properties Inventory

| Property Key | Default | Profiles | Source |
|---|---|---|---|
| db.host | sqlserver | default | wire-transfer.properties |
| db.port | 1433 | default | wire-transfer.properties |
| db.name | ZavaBankDB | default | wire-transfer.properties |
| db.user | sa | default | wire-transfer.properties |
| db.password | [MASKED] | default | wire-transfer.properties |
| currency.service.baseUrl | http://zava-currency-service:8080 | default | wire-transfer.properties |
| ledger.service.baseUrl | http://zava-ledger:8080 | default | wire-transfer.properties |
| ledger.settlement.accountId | 1 | default | wire-transfer.properties |
| rabbitmq.host | rabbitmq | default | wire-transfer.properties |
| rabbitmq.port | 5672 | default | wire-transfer.properties |
| rabbitmq.user | guest | default | wire-transfer.properties |
| rabbitmq.password | [MASKED] | default | wire-transfer.properties |
| rabbitmq.exchange | wire.events | default | wire-transfer.properties |
| rabbitmq.routing.key.prefix | wire | default | wire-transfer.properties |

## Startup Parameters & Resource Requirements

| Service | JVM/Runtime Options | Memory | Instance Count |
|---|---|---|---|
| ZavaWireTransferService | None explicitly defined | Not specified | Not specified |

## Startup Dependency Chain

1. ZavaWireTransferService starts and initializes `WireBootstrapServlet`.
2. Application requires SQL Server reachability for startup table initialization.
3. Transfer processing at runtime depends on currency service, ledger service, and RabbitMQ availability.

## Secrets & Sensitive Configuration

| Secret Reference | Type | Storage (masked) |
|---|---|---|
| DB_PASSWORD / db.password | Database credential | Environment variable or properties file ([MASKED]) |
| RABBITMQ_PASSWORD / rabbitmq.password | Broker credential | Environment variable or properties file ([MASKED]) |

### Secrets Provisioning Workflow

Secrets are supplied either as environment variables (preferred) or fallback property values loaded from `wire-transfer.properties`. The application reads them on demand via `WireConfig` and uses them for DB and RabbitMQ connectivity.

## Feature Flags

No feature flag framework or conditional feature toggles were detected.

## Framework & Runtime Versions

| Component | Version | Source |
|---|---|---|
| Java | 11 target | build.gradle |
| Servlet API | 4.0.1 | build.gradle |
| Gradle plugin model | Legacy apply plugin | build.gradle |
| SQL Server JDBC Driver | 12.6.3.jre11 | build.gradle |
| RabbitMQ Client | 5.20.0 | build.gradle |
| org.json | 20140107 | build.gradle |
| Runtime container | Tomcat 9 JDK11 | Dockerfile |
