# Architecture Diagram

This document summarizes the application architecture and key component relationships for ZavaWireTransferService.

## Application Architecture

```mermaid
flowchart TD
    subgraph Client["Client Layer"]
        ApiClient["API Client"]
    end
    subgraph App["Application Layer - Java Servlet 4"]
        Health["HealthServlet"]
        WireTransfer["WireTransferServlet"]
        WireStatus["WireStatusServlet"]
        Bootstrap["WireBootstrapServlet"]
        Config["WireConfig"]
    end
    subgraph Data["Data Layer"]
        JDBC["JDBC DriverManager"]
        SQLDB[("SQL Server")]
    end
    subgraph Integration["External Services"]
        Currency["Currency Service HTTP"]
        Ledger["Ledger Service HTTP"]
        Rabbit[("RabbitMQ")]
    end

    ApiClient -->|"POST /api/wire/transfer"| WireTransfer
    ApiClient -->|"GET /api/wire/{id}/status"| WireStatus
    ApiClient -->|"GET /health"| Health
    Bootstrap -->|"create table"| JDBC -->|"T-SQL"| SQLDB
    WireTransfer -->|"insert transfer"| JDBC
    WireStatus -->|"query status"| JDBC
    WireTransfer -->|"GET fx rate"| Currency
    WireTransfer -->|"POST transaction"| Ledger
    WireTransfer -->|"publish event"| Rabbit
    Config -->|"env/properties"| WireTransfer
    Config -->|"env/properties"| WireStatus
```

### Technology Stack Summary

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Presentation | Java Servlet API | 4.0.1 | HTTP endpoints |
| Business | Plain Java services in servlets | Java 11 | Transfer orchestration |
| Data Access | JDBC + SQL Server driver | 12.6.3.jre11 | Persistence of wire transfers |
| Messaging | RabbitMQ Java Client | 5.20.0 | Publish wire status events |

### Data Storage & External Services

The service persists wire transfer records in SQL Server using JDBC. It also depends on external HTTP services for FX rates and ledger posting, and publishes status events to RabbitMQ.

### Key Architectural Decisions

- Uses servlet-based endpoints with manual JSON handling and JDBC statements.
- Keeps configuration externalized through environment variables with property-file fallbacks.
- Publishes asynchronous wire-transfer events after persistence and ledger attempt.

## Component Relationships

```mermaid
flowchart LR
    subgraph Presentation
        HealthServlet["HealthServlet"]
        WireTransferServlet["WireTransferServlet"]
        WireStatusServlet["WireStatusServlet"]
        WireBootstrapServlet["WireBootstrapServlet"]
    end
    subgraph Business["Business Logic"]
        Validation["Routing/amount validation"]
        FxCall["FX rate call"]
        LedgerCall["Ledger debit call"]
        EventPub["RabbitMQ publish"]
    end
    subgraph DataAccess["Data Access"]
        ConnFactory["WireConnectionFactory"]
        SQLInsert["Insert WireTransfers"]
        SQLSelect["Select WireTransfers"]
        SQLDDL["Create WireTransfers table"]
    end
    subgraph Infrastructure
        ConfigReader["WireConfig"]
    end

    WireTransferServlet -->|"uses"| Validation
    WireTransferServlet -->|"calls"| FxCall
    WireTransferServlet -->|"calls"| LedgerCall
    WireTransferServlet -->|"publishes"| EventPub
    WireTransferServlet -->|"persists"| SQLInsert
    WireStatusServlet -->|"reads"| SQLSelect
    WireBootstrapServlet -->|"initializes"| SQLDDL
    SQLInsert -->|"opens connection"| ConnFactory
    SQLSelect -->|"opens connection"| ConnFactory
    SQLDDL -->|"opens connection"| ConnFactory
    ConfigReader -.->|"provides settings"| WireTransferServlet
    ConfigReader -.->|"provides settings"| WireStatusServlet
    ConfigReader -.->|"provides settings"| ConnFactory
```

### Component Inventory

| Component | Layer | Type | Responsibility |
|---|---|---|---|
| WireTransferServlet | Presentation | Servlet | Validates and executes wire transfer flow |
| WireStatusServlet | Presentation | Servlet | Returns persisted wire transfer status |
| HealthServlet | Presentation | Servlet | Basic service health page |
| WireBootstrapServlet | Presentation | Startup Servlet | Ensures table exists at startup |
| WireConnectionFactory | Data Access | Utility | Opens JDBC connections |
| WireConfig | Infrastructure | Config Utility | Resolves env/property configuration |
