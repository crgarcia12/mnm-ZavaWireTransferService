package com.zavabank.wiretransfer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class WireConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try {
            InputStream inputStream = WireConfig.class.getClassLoader().getResourceAsStream("wire-transfer.properties");
            if (inputStream != null) {
                PROPERTIES.load(inputStream);
                inputStream.close();
            }
        } catch (IOException ignored) {
        }
    }

    private WireConfig() {
    }

    public static String getDbUrl() {
        String host = read("DB_HOST", "db.host");
        String port = read("DB_PORT", "db.port");
        String name = read("DB_NAME", "db.name");
        return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + name + ";encrypt=false;trustServerCertificate=true";
    }

    public static String getDbUser() {
        return read("DB_USER", "db.user");
    }

    public static String getDbPassword() {
        return read("DB_PASSWORD", "db.password");
    }

    public static String getCurrencyServiceBaseUrl() {
        return read("CURRENCY_SERVICE_BASE_URL", "currency.service.baseUrl");
    }

    public static String getLedgerServiceBaseUrl() {
        return read("LEDGER_SERVICE_BASE_URL", "ledger.service.baseUrl");
    }

    public static int getLedgerSettlementAccountId() {
        try {
            return Integer.parseInt(read("LEDGER_SETTLEMENT_ACCOUNT_ID", "ledger.settlement.accountId"));
        } catch (Exception ignored) {
            return 1;
        }
    }

    public static String getRabbitHost() {
        return read("RABBITMQ_HOST", "rabbitmq.host");
    }

    public static int getRabbitPort() {
        try {
            return Integer.parseInt(read("RABBITMQ_PORT", "rabbitmq.port"));
        } catch (Exception ignored) {
            return 5672;
        }
    }

    public static String getRabbitUser() {
        return read("RABBITMQ_USER", "rabbitmq.user");
    }

    public static String getRabbitPassword() {
        return read("RABBITMQ_PASSWORD", "rabbitmq.password");
    }

    public static String getRabbitExchange() {
        return read("RABBITMQ_EXCHANGE", "rabbitmq.exchange");
    }

    public static String getRabbitRoutingKeyPrefix() {
        return read("RABBITMQ_ROUTING_KEY_PREFIX", "rabbitmq.routing.key.prefix");
    }

    private static String read(String envKey, String propertyKey) {
        String value = System.getenv(envKey);
        if (value != null && value.trim().length() > 0) {
            return value.trim();
        }
        return PROPERTIES.getProperty(propertyKey);
    }
}
