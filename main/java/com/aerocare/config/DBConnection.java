package com.aerocare.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Pembuat koneksi JDBC ke database aerocare. Setiap pemanggilan menghasilkan koneksi baru. */
public final class DBConnection {

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        DatabaseConfig cfg = DatabaseConfig.get();
        return DriverManager.getConnection(cfg.url(), cfg.user(), cfg.password());
    }

    /** Mengembalikan true bila koneksi berhasil dibuka dan valid. */
    public static boolean test() {
        try (Connection c = getConnection()) {
            return c.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }
}
