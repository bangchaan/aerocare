package com.aerocare.config;

import java.sql.Connection;
import java.sql.SQLException;

/** Pembungkus transaksi JDBC: commit bila sukses, rollback bila gagal. */
public final class Transaction {

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private Transaction() {
    }

    public static <T> T run(Work<T> work) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    c.rollback();
                } catch (SQLException ignored) {
                    // rollback gagal tidak boleh menutupi galat asli
                }
                throw e;
            }
        }
    }
}
