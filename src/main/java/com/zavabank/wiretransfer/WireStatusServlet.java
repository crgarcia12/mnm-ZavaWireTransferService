package com.zavabank.wiretransfer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class WireStatusServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.trim().length() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Expected /api/wire/{id}/status.\"}");
            return;
        }

        String[] segments = pathInfo.split("/");
        if (segments.length != 3 || !"status".equalsIgnoreCase(segments[2])) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Expected /api/wire/{id}/status.\"}");
            return;
        }

        long wireTransferId;
        try {
            wireTransferId = Long.parseLong(segments[1]);
        } catch (NumberFormatException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"wire id must be numeric.\"}");
            return;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = WireConnectionFactory.openConnection();
            statement = connection.prepareStatement(
                "SELECT WireTransferID, Status, ReferenceNumber, Amount, CurrencyCode, FxRate, DebitAmountUSD, IsInternational, " +
                    "FailureReason, CreatedDate, UpdatedDate FROM WireTransfers WHERE WireTransferID = ?"
            );
            statement.setLong(1, wireTransferId);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"status\":\"NOT_FOUND\",\"message\":\"Wire transfer not found.\"}");
                return;
            }

            JSONObject result = new JSONObject();
            result.put("wireTransferId", resultSet.getLong("WireTransferID"));
            result.put("status", resultSet.getString("Status"));
            result.put("referenceNumber", resultSet.getString("ReferenceNumber"));
            result.put("amount", resultSet.getBigDecimal("Amount"));
            result.put("currencyCode", resultSet.getString("CurrencyCode"));
            result.put("fxRate", resultSet.getBigDecimal("FxRate"));
            result.put("debitAmountUSD", resultSet.getBigDecimal("DebitAmountUSD"));
            result.put("isInternational", resultSet.getBoolean("IsInternational"));
            result.put("failureReason", resultSet.getString("FailureReason"));
            result.put("createdDate", resultSet.getTimestamp("CreatedDate"));
            result.put("updatedDate", resultSet.getTimestamp("UpdatedDate"));
            response.getWriter().write(result.toString());
        } catch (SQLException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Unable to load wire status.\"}");
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
