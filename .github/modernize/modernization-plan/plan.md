# Modernization Plan: modernization-plan

**Project**: mnm-ZavaWireTransferService

---

## Technical Framework

- **Language**: Java 11
- **Framework**: Java Servlet WAR application
- **Build Tool**: Gradle
- **Database**: Microsoft SQL Server (JDBC)
- **Key Dependencies**: mssql-jdbc, RabbitMQ Java client, org.json

---

## Overview

> This modernization plan establishes the initial migration scope for moving
> the Java wire transfer service toward Azure-ready operations.
>
> - Establish a clear security baseline by remediating known dependency CVEs.
> - Preserve current application behavior while preparing for phased migration.
> - Keep implementation details deferred to execution tasks and follow-on scope.
>
> The migration follows a phased approach, beginning with security
> hardening before additional service migration tasks are introduced.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| mnm-ZavaWireTransferService | Current runtime/services | TBD in follow-on scope | Managed Identity | Initial plan baseline created |

---

## Security Compliance

**Description**: Scan all project dependencies for known CVEs and remediate any identified vulnerabilities to ensure the application is secure before deployment.

**Requirements**:
Upgrade vulnerable dependencies to the minimum patched version. If a CVE fix requires a major version upgrade, document the affected dependency, the current version, the upgraded major version, and the breaking change risk. Verify that the project builds and all tests pass after remediation.

**Environment Configuration**:
Runtime environment and Gradle build configuration from current project setup.

**App Scope**:
The full application source set and Gradle dependency graph.

**Skills**:
- Skill Name: validate-cves-and-fix
  - Skill Location: builtin
