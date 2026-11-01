package com.aerocare.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Membaca db.properties dari classpath. Dapat ditimpa dengan variabel lingkungan AEROCARE_DB_URL/USER/PASSWORD. */
public final class DatabaseConfig {

    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConfig() {
        Properties p = new Properties();
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca db.properties: " + e.getMessage(), e);
        }
        this.url = env("AEROCARE_DB_URL", p.getProperty("db.url", "jdbc:mysql://localhost:3306/aerocare"));
        this.user = env("AEROCARE_DB_USER", p.getProperty("db.user", "root"));
        this.password = env("AEROCARE_DB_PASSWORD", p.getProperty("db.password", ""));
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    public static DatabaseConfig get() {
        return INSTANCE;
    }

    public String url() { return url; }
    public String user() { return user; }
    public String password() { return password; }
}
