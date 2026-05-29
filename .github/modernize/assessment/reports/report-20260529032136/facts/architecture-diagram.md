# Architecture Diagram

This document summarizes the current application architecture and key runtime components for ZavaWireTransferService.

## Application Architecture

```mermaid
flowchart TD
    subgraph Client["Client Layer"]
        Browser["Web Client or API Consumer"]
    end

    subgraph App["Application Layer - Java Servlet WAR"]
        HealthServlet["HealthServlet"]
        WireTransferServlet["WireTransferServlet"]
        WireStatusServlet["WireStatusServlet"]
        WireBootstrapServlet["WireBootstrapServlet"]
        Config["WireConfig"]
    end

    subgraph Data["Data Layer"]
        JDBC["JDBC via DriverManager"]
        SQLDB[("SQL Server")]
    end

    subgraph Messaging["Messaging"]
        Rabbit[("RabbitMQ Topic Exchange")]
    end

    subgraph External["External Services"]
        FX["Currency Service"]
        Ledger["Ledger Service"]
    end

    Browser -->|"HTTP requests"| WireTransferServlet
    Browser -->|"HTTP requests"| WireStatusServlet
    Browser -->|"Health check"| HealthServlet
    WireBootstrapServlet -->|"startup DDL"| JDBC
    WireTransferServlet -->|"persist transfer"| JDBC
    WireStatusServlet -->|"lookup status"| JDBC
    JDBC -->|"SQL queries"| SQLDB
    WireTransferServlet -->|"GET FX rate"| FX
    WireTransferServlet -->|"POST ledger debit"| Ledger
    WireTransferServlet -->|"publish event"| Rabbit
    Config -->|"configuration"| WireTransferServlet
    Config -->|"configuration"| WireStatusServlet
    Config -->|"configuration"| WireBootstrapServlet
```

### Technology Stack Summary

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Presentation | Java Servlet API | 4.0.1 | Exposes HTTP endpoints |
| Application | Java 11 | 11 | Core service runtime |
| Data Access | JDBC + SQL Server driver | 12.6.3.jre11 | Database connectivity |
| Messaging | RabbitMQ Java client | 5.20.0 | Event publishing |
| Build/Packaging | Gradle + WAR plugin | project-defined | Produces deployable WAR for Tomcat |

### Data Storage & External Services

The service stores wire transfer records in SQL Server using direct JDBC statements. It integrates with an external currency-rate API for FX conversion, a ledger API for debit posting, and RabbitMQ for asynchronous wire-transfer event publication.

### Key Architectural Decisions

- Uses classic servlet-based endpoints instead of a full Spring stack.
- Persists transactional data with explicit SQL statements and manual resource handling.
- External integrations are direct HTTP calls and RabbitMQ publish operations inside servlet flow.

## Component Relationships

```mermaid
flowchart LR
    subgraph Presentation
        Health["HealthServlet"]
        Transfer["WireTransferServlet"]
        Status["WireStatusServlet"]
    end

    subgraph Business["Business Logic"]
        Validation["Routing and payload validation"]
        Fx["FX rate retrieval"]
        LedgerPost["Ledger debit posting"]
        EventPub["Wire event publication"]
    end

    subgraph DataAccess["Data Access"]
        Bootstrap["WireBootstrapServlet"]
        ConnectionFactory["WireConnectionFactory"]
        ConfigComp["WireConfig"]
        WireTable["WireTransfers table"]
    end

    subgraph Infrastructure
        CurrencySvc["Currency Service"]
        LedgerSvc["Ledger Service"]
        RabbitMq["RabbitMQ"]
        SqlServer["SQL Server"]
    end

    Transfer -->|"validates"| Validation
    Transfer -->|"requests rate"| Fx
    Transfer -->|"posts debit"| LedgerPost
    Transfer -->|"emits event"| EventPub
    Transfer -->|"opens JDBC"| ConnectionFactory
    Status -->|"opens JDBC"| ConnectionFactory
    Bootstrap -->|"opens JDBC"| ConnectionFactory
    ConnectionFactory -->|"uses"| ConfigComp
    ConnectionFactory -->|"connects"| SqlServer
    Bootstrap -->|"creates schema"| WireTable
    Transfer -->|"inserts"| WireTable
    Status -->|"queries"| WireTable
    Fx -->|"HTTP"| CurrencySvc
    LedgerPost -->|"HTTP"| LedgerSvc
    EventPub -->|"AMQP topic"| RabbitMq
```

### Component Inventory

| Component | Layer | Type | Responsibility |
|---|---|---|---|
| HealthServlet | Presentation | Servlet | Returns service health page |
| WireTransferServlet | Presentation | Servlet | Validates transfer requests and orchestrates transfer processing |
| WireStatusServlet | Presentation | Servlet | Reads persisted transfer status by id |
| WireBootstrapServlet | Data Access | Servlet initializer | Creates WireTransfers table at startup if missing |
| WireConnectionFactory | Data Access | Utility | Opens SQL Server JDBC connections |
| WireConfig | Data Access | Configuration utility | Resolves environment/property-based settings |
