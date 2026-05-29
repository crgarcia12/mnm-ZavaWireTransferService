# Modernization Plan: Modernize Zava Wire Transfer Service for Azure

**Project**: ZavaWireTransferService

---

## Technical Framework

- **Language**: Java 11
- **Framework**: Java EE Servlet (javax.servlet 4.0)
- **Build Tool**: Gradle (war packaging)
- **Database**: Microsoft SQL Server
- **Key Dependencies**: javax.servlet-api, mssql-jdbc, org.json, RabbitMQ Java client

---

## Overview

> This migration modernizes the Zava Wire Transfer Service and deploys it to Azure. The
> application currently runs as a Java servlet-based WAR with on-prem style dependencies.
> The new architecture will:
>
> - Upgrade the application runtime/framework baseline to a current supported stack
> - Move core messaging/data/security integrations to Azure-native managed services
> - Deploy the modernized service to Azure using a cloud-native deployment target
>
> The migration follows a phased approach: runtime upgrade first, service migrations next,
> then security remediation, and finally Azure deployment.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| ZavaWireTransferService | Java 11/Java EE Servlet | Latest Java/Jakarta baseline | N/A | Modernize to latest framework baseline |
| ZavaWireTransferService | RabbitMQ | Azure Service Bus | Managed Identity | Cloud-native messaging modernization |
| ZavaWireTransferService | SQL Server | Azure SQL Database | Managed Identity | Cloud-native data platform modernization |
| ZavaWireTransferService | Secret/config in env/properties | Azure Key Vault | Managed Identity | Centralized secret management |
| ZavaWireTransferService | Local deployment | Azure Container Apps | Managed Identity | Azure deployment target |

---

## Security Compliance

**Description**: Scan all project dependencies for known CVEs and remediate any
identified vulnerabilities to ensure the application is secure before deployment.

**Requirements**:
- Upgrade vulnerable dependencies to minimally patched versions.
- If remediation needs a major upgrade, document dependency, version jump, and risk.
- Verify the project builds and tests pass after remediation.
