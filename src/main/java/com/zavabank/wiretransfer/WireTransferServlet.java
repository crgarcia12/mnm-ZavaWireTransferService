package com.zavabank.wiretransfer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class WireTransferServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject payload;
        try {
            payload = new JSONObject(readBody(request));
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Invalid JSON payload.\"}");
            return;
        }

        int fromAccountId = payload.optInt("fromAccountId", 0);
        String toAccountNumber = payload.optString("toAccountNumber", "").trim();
        String toRoutingNumber = payload.optString("toRoutingNumber", "").trim();
        String beneficiaryName = payload.optString("beneficiaryName", "").trim();
        String currencyCode = payload.optString("currencyCode", "USD").trim().toUpperCase();
        BigDecimal amount = BigDecimal.valueOf(payload.optDouble("amount", 0d)).setScale(2, RoundingMode.HALF_UP);

        if (fromAccountId <= 0 || toAccountNumber.length() == 0 || toRoutingNumber.length() == 0 || beneficiaryName.length() == 0
            || amount.compareTo(BigDecimal.ZERO) <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"fromAccountId, toAccountNumber, toRoutingNumber, beneficiaryName and amount are required.\"}");
            return;
        }
        if (!isValidRoutingNumber(toRoutingNumber)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Invalid routing number.\"}");
            return;
        }

        boolean international = !"USD".equals(currencyCode);
        BigDecimal fxRate = BigDecimal.ONE;
        if (international) {
            fxRate = callFxRate(currencyCode);
            if (fxRate.compareTo(BigDecimal.ZERO) <= 0) {
                fxRate = BigDecimal.ONE;
            }
        }
        BigDecimal debitAmountUsd = amount.multiply(fxRate).setScale(2, RoundingMode.HALF_UP);
        String referenceNumber = "WIRE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        boolean ledgerPosted = postLedgerDebit(fromAccountId, debitAmountUsd, referenceNumber, toAccountNumber);
        String status = ledgerPosted ? "POSTED" : "PENDING_LEDGER";
        String failureReason = ledgerPosted ? null : "Ledger posting failed.";

        Connection connection = null;
        long wireTransferId = 0L;
        try {
            connection = WireConnectionFactory.openConnection();
            wireTransferId = insertWireTransfer(
                connection,
                fromAccountId,
                toAccountNumber,
                toRoutingNumber,
                beneficiaryName,
                currencyCode,
                amount,
                fxRate,
                debitAmountUsd,
                international,
                status,
                referenceNumber,
                failureReason
            );
        } catch (SQLException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Unable to persist wire transfer.\"}");
            return;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }

        publishWireEvent(wireTransferId, status, referenceNumber, amount, currencyCode);

        JSONObject result = new JSONObject();
        result.put("wireTransferId", wireTransferId);
        result.put("status", status);
        result.put("referenceNumber", referenceNumber);
        result.put("fxRate", fxRate);
        result.put("debitAmountUSD", debitAmountUsd);
        result.put("ledgerPosted", ledgerPosted);
        response.getWriter().write(result.toString());
    }

    private long insertWireTransfer(
        Connection connection,
        int fromAccountId,
        String toAccountNumber,
        String toRoutingNumber,
        String beneficiaryName,
        String currencyCode,
        BigDecimal amount,
        BigDecimal fxRate,
        BigDecimal debitAmountUsd,
        boolean international,
        String status,
        String referenceNumber,
        String failureReason
    ) throws SQLException {
        PreparedStatement statement = null;
        ResultSet keys = null;
        try {
            statement = connection.prepareStatement(
                "INSERT INTO WireTransfers (FromAccountID, ToAccountNumber, ToRoutingNumber, BeneficiaryName, CurrencyCode, Amount, FxRate, DebitAmountUSD, " +
                    "IsInternational, Status, ReferenceNumber, FailureReason, CreatedDate, UpdatedDate) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())",
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setInt(1, fromAccountId);
            statement.setString(2, toAccountNumber);
            statement.setString(3, toRoutingNumber);
            statement.setString(4, beneficiaryName);
            statement.setString(5, currencyCode);
            statement.setBigDecimal(6, amount);
            statement.setBigDecimal(7, fxRate);
            statement.setBigDecimal(8, debitAmountUsd);
            statement.setBoolean(9, international);
            statement.setString(10, status);
            statement.setString(11, referenceNumber);
            statement.setString(12, failureReason);
            statement.executeUpdate();
            keys = statement.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("Wire transfer ID was not generated.");
        } finally {
            if (keys != null) {
                keys.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    private boolean isValidRoutingNumber(String routingNumber) {
        if (!routingNumber.matches("\\d{9}")) {
            return false;
        }
        int checksum = 0;
        checksum += 3 * digit(routingNumber, 0);
        checksum += 7 * digit(routingNumber, 1);
        checksum += 1 * digit(routingNumber, 2);
        checksum += 3 * digit(routingNumber, 3);
        checksum += 7 * digit(routingNumber, 4);
        checksum += 1 * digit(routingNumber, 5);
        checksum += 3 * digit(routingNumber, 6);
        checksum += 7 * digit(routingNumber, 7);
        checksum += 1 * digit(routingNumber, 8);
        return checksum % 10 == 0;
    }

    private int digit(String value, int index) {
        return Character.digit(value.charAt(index), 10);
    }

    private BigDecimal callFxRate(String toCurrency) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            String endpoint = WireConfig.getCurrencyServiceBaseUrl() + "/api/currency/rate?from=USD&to=" + toCurrency;
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
                reader = new BufferedReader(new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder body = new StringBuilder();
                String line = reader.readLine();
                while (line != null) {
                    body.append(line);
                    line = reader.readLine();
                }
                if (body.length() > 0) {
                    JSONObject response = new JSONObject(body.toString());
                    return BigDecimal.valueOf(response.optDouble("rate", 1d));
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return BigDecimal.ONE;
    }

    private boolean postLedgerDebit(int fromAccountId, BigDecimal amountUsd, String referenceNumber, String beneficiaryAccount) {
        HttpURLConnection connection = null;
        OutputStreamWriter writer = null;
        try {
            String endpoint = WireConfig.getLedgerServiceBaseUrl() + "/api/transactions";
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.setDoOutput(true);

            String xml = "<transactionRequest>" +
                "<debitAccountId>" + fromAccountId + "</debitAccountId>" +
                "<creditAccountId>" + WireConfig.getLedgerSettlementAccountId() + "</creditAccountId>" +
                "<amount>" + amountUsd + "</amount>" +
                "<description>Wire transfer " + referenceNumber + "</description>" +
                "<referenceNumber>" + referenceNumber + "</referenceNumber>" +
                "</transactionRequest>";
            writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(xml);
            writer.flush();
            return connection.getResponseCode() >= 200 && connection.getResponseCode() < 300;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void publishWireEvent(long wireTransferId, String status, String referenceNumber, BigDecimal amount, String currencyCode) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(WireConfig.getRabbitHost());
        connectionFactory.setPort(WireConfig.getRabbitPort());
        connectionFactory.setUsername(WireConfig.getRabbitUser());
        connectionFactory.setPassword(WireConfig.getRabbitPassword());

        com.rabbitmq.client.Connection connection = null;
        Channel channel = null;
        try {
            connection = connectionFactory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(WireConfig.getRabbitExchange(), "topic", true);

            JSONObject event = new JSONObject();
            event.put("wireTransferId", wireTransferId);
            event.put("status", status);
            event.put("referenceNumber", referenceNumber);
            event.put("amount", amount);
            event.put("currencyCode", currencyCode);
            event.put("eventDate", System.currentTimeMillis());

            String routingKey = WireConfig.getRabbitRoutingKeyPrefix() + "." + status.toLowerCase();
            channel.basicPublish(
                WireConfig.getRabbitExchange(),
                routingKey,
                null,
                event.toString().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ignored) {
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (Exception ignored) {
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String readBody(HttpServletRequest request) throws IOException {
        BufferedReader reader = request.getReader();
        StringBuilder body = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            body.append(line);
            line = reader.readLine();
        }
        return body.toString();
    }
}
