package com.zavabank.wiretransfer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class WireConnectionFactory {
    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("SQL Server JDBC driver not found", exception);
        }
    }

    private WireConnectionFactory() {
    }

    public static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
            WireConfig.getDbUrl(),
            WireConfig.getDbUser(),
            WireConfig.getDbPassword()
        );
    }
}
