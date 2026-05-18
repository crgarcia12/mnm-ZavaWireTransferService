package com.zavabank.wiretransfer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class WireBootstrapServlet extends HttpServlet {
    @Override
    public void init() throws ServletException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = WireConnectionFactory.openConnection();
            statement = connection.prepareStatement(
                "IF OBJECT_ID('WireTransfers', 'U') IS NULL " +
                    "CREATE TABLE WireTransfers (" +
                    "WireTransferID BIGINT IDENTITY(1,1) PRIMARY KEY, " +
                    "FromAccountID INT NOT NULL, " +
                    "ToAccountNumber NVARCHAR(34) NOT NULL, " +
                    "ToRoutingNumber NVARCHAR(20) NOT NULL, " +
                    "BeneficiaryName NVARCHAR(200) NOT NULL, " +
                    "CurrencyCode NVARCHAR(3) NOT NULL, " +
                    "Amount DECIMAL(18,2) NOT NULL, " +
                    "FxRate DECIMAL(18,8) NOT NULL DEFAULT 1, " +
                    "DebitAmountUSD DECIMAL(18,2) NOT NULL, " +
                    "IsInternational BIT NOT NULL DEFAULT 0, " +
                    "Status NVARCHAR(30) NOT NULL, " +
                    "ReferenceNumber NVARCHAR(80) NOT NULL, " +
                    "FailureReason NVARCHAR(500) NULL, " +
                    "CreatedDate DATETIME NOT NULL DEFAULT GETDATE(), " +
                    "UpdatedDate DATETIME NOT NULL DEFAULT GETDATE()" +
                    ");"
            );
            statement.execute();
        } catch (SQLException exception) {
            throw new ServletException("Wire bootstrap failed.", exception);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
