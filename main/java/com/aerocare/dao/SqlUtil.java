package com.aerocare.dao;

import com.aerocare.model.enums.DbEnum;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Fungsi bantu JDBC untuk seluruh DAO (binding parameter, konversi tanggal, LIKE). */
final class SqlUtil {

    /** Join progres terakhir suatu penugasan (alias penugasan harus "p", hasil di alias "pr"). */
    static final String JOIN_PROGRES_TERAKHIR = """
            LEFT JOIN progres_penanganan pr ON pr.id_progres = (
                SELECT pr2.id_progres FROM progres_penanganan pr2
                WHERE pr2.PENUGASAN_id_penugasan = p.id_penugasan
                ORDER BY pr2.waktu_update DESC, pr2.id_progres DESC LIMIT 1)
            """;

    private SqlUtil() {
    }

    static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            int idx = i + 1;
            if (p instanceof DbEnum e) {
                ps.setString(idx, e.getLabel());
            } else if (p instanceof LocalDate d) {
                ps.setDate(idx, Date.valueOf(d));
            } else if (p instanceof LocalDateTime t) {
                ps.setTimestamp(idx, Timestamp.valueOf(t));
            } else {
                ps.setObject(idx, p);
            }
        }
    }

    static LocalDate date(ResultSet rs, String column) throws SQLException {
        Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }

    static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime();
    }

    static Integer intOrNull(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Pola LIKE "%kata%" dengan karakter khusus (% _ \) di-escape. */
    static String like(String keyword) {
        String k = keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + k + "%";
    }

    /** Menambahkan filter periode tanggal_laporan pada alias tabel laporan (boleh kosong). */
    static void periode(String alias, LocalDate dari, LocalDate sampai, StringBuilder sql, List<Object> params) {
        if (dari != null) {
            sql.append(" AND ").append(alias).append(".tanggal_laporan >= ?");
            params.add(dari);
        }
        if (sampai != null) {
            sql.append(" AND ").append(alias).append(".tanggal_laporan <= ?");
            params.add(sampai);
        }
    }
}
