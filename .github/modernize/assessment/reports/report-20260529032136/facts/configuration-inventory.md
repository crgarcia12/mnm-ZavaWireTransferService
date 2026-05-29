# Configuration & Externalized Settings Inventory

The application relies on a compact configuration model combining property files, environment variable overrides, and container runtime defaults.

## Configuration Sources

| Source | Type | Path/Location | Notes |
|---|---|---|---|
| Gradle build file | Build configuration | /build.gradle | Declares Java 11 target and dependencies |
| Servlet deployment descriptor | Web runtime configuration | /src/main/webapp/WEB-INF/web.xml | Declares servlets and URL mappings |
| Application properties | Runtime defaults | /src/main/resources/wire-transfer.properties | Holds DB, service URL, and RabbitMQ defaults |
| Docker image definition | Container runtime settings | /Dockerfile | Defines ENV defaults and deployment port |

## Build Profiles

| Profile | Activation | Purpose | Key Dependencies/Plugins |
|---|---|---|---|
| Default Gradle build | Standard Gradle invocation | Compile Java and package WAR | java plugin, war plugin |

## Runtime Profiles

| Profile | Activation Method | Config Files | Key Overrides |
|---|---|---|---|
| Default runtime | Container start / JVM start | wire-transfer.properties | DB, external service, and RabbitMQ defaults |
| Environment override mode | Process environment variables | ENV variables resolved by WireConfig | Overrides corresponding property defaults |

## Properties Inventory

| Property Key | Default | Profiles | Source |
|---|---|---|---|
| db.host | sqlserver | Default | wire-transfer.properties |
| db.port | 1433 | Default | wire-transfer.properties |
| db.name | ZavaBankDB | Default | wire-transfer.properties |
| db.user | sa | Default | wire-transfer.properties |
| db.password | [MASKED] | Default | wire-transfer.properties |
| currency.service.baseUrl | http://zava-currency-service:8080 | Default | wire-transfer.properties |
| ledger.service.baseUrl | http://zava-ledger:8080 | Default | wire-transfer.properties |
| ledger.settlement.accountId | 1 | Default | wire-transfer.properties |
| rabbitmq.host | rabbitmq | Default | wire-transfer.properties |
| rabbitmq.port | 5672 | Default | wire-transfer.properties |
| rabbitmq.user | guest | Default | wire-transfer.properties |
| rabbitmq.password | [MASKED] | Default | wire-transfer.properties |
| rabbitmq.exchange | wire.events | Default | wire-transfer.properties |
| rabbitmq.routing.key.prefix | wire | Default | wire-transfer.properties |

## Startup Parameters & Resource Requirements

| Service | JVM/Runtime Options | Memory | Instance Count |
|---|---|---|---|
| ZavaWireTransferService | No explicit JVM flags detected in repository | Not specified | Not specified |

## Startup Dependency Chain

1. ZavaWireTransferService starts in Tomcat and initializes `WireBootstrapServlet`.
2. `WireBootstrapServlet` requires SQL Server connectivity to create the WireTransfers table if absent.
3. Transfer operations then depend on currency service, ledger service, and RabbitMQ availability.

## Secrets & Sensitive Configuration

| Secret Reference | Type | Storage (masked) |
|---|---|---|
| DB_PASSWORD / db.password | Database credential | Environment variable or properties file value [MASKED] |
| RABBITMQ_PASSWORD / rabbitmq.password | Messaging credential | Environment variable or properties file value [MASKED] |

### Secrets Provisioning Workflow

Secrets are provided either by process environment variables or by static property defaults. At runtime, the application resolves environment values first and falls back to file-based defaults when variables are absent. No external secret manager integration or identity-based retrieval workflow was detected.

## Feature Flags

| Flag Name | Default | Controlled By |
|---|---|---|
| None detected | N/A | N/A |

## Framework & Runtime Versions

| Component | Version | Source |
|---|---|---|
| Java | 11 | build.gradle |
| Servlet API | 4.0.1 | build.gradle |
| Gradle | build-script defined (no wrapper pinned) | build.gradle |
| SQL Server JDBC Driver | 12.6.3.jre11 | build.gradle |
| RabbitMQ Java Client | 5.20.0 | build.gradle |
| JSON library | 20140107 | build.gradle |
| Docker build image | gradle:7.6-jdk11 | Dockerfile |
| Docker runtime image | tomcat:9-jdk11 | Dockerfile |
