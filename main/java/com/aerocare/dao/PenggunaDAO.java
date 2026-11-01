package com.aerocare.dao;

import com.aerocare.config.DBConnection;
import com.aerocare.model.Admin;
import com.aerocare.model.Pengguna;
import com.aerocare.model.Petugas;
import com.aerocare.model.Supervisor;
import com.aerocare.model.enums.Peran;
import com.aerocare.model.enums.ShiftKerja;
import com.aerocare.model.enums.Spesialisasi;
import com.aerocare.model.enums.StatusAktif;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Akses tabel PENGGUNA beserta subtype ADMIN, SUPERVISOR, PETUGAS. */
public class PenggunaDAO {

    private static final String BASE = """
            SELECT p.id_pengguna, p.nama_lengkap, p.username, p.password, p.email, p.no_telepon,
                   p.peran, p.status_aktif, a.hak_akses, s.area_pengawasan, t.spesialisasi, t.shift_kerja
            FROM pengguna p
            LEFT JOIN admin a ON a.id_pengguna = p.id_pengguna
            LEFT JOIN supervisor s ON s.id_pengguna = p.id_pengguna
            LEFT JOIN petugas t ON t.id_pengguna = p.id_pengguna
            """;

    private static Pengguna map(ResultSet rs) throws SQLException {
        Peran peran = Peran.fromLabel(rs.getString("peran"));
        int id = rs.getInt("id_pengguna");
        String nama = rs.getString("nama_lengkap");
        String user = rs.getString("username");
        String pass = rs.getString("password");
        String email = rs.getString("email");
        String telp = rs.getString("no_telepon");
        StatusAktif status = StatusAktif.fromLabel(rs.getString("status_aktif"));
        return switch (peran) {
            case ADMIN -> new Admin(id, nama, user, pass, email, telp, status, rs.getString("hak_akses"));
            case SUPERVISOR -> new Supervisor(id, nama, user, pass, email, telp, status, rs.getString("area_pengawasan"));
            case PETUGAS -> new Petugas(id, nama, user, pass, email, telp, status,
                    Spesialisasi.fromLabel(rs.getString("spesialisasi")), ShiftKerja.fromLabel(rs.getString("shift_kerja")));
        };
    }

    public Optional<Pengguna> findByUsername(String username) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + " WHERE p.username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Pengguna> findById(int idPengguna) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + " WHERE p.id_pengguna = ?")) {
            ps.setInt(1, idPengguna);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Daftar pengguna; semua parameter filter boleh null. */
    public List<Pengguna> search(String keyword, Peran peran, StatusAktif status) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (p.nama_lengkap LIKE ? OR p.username LIKE ?)");
            String k = SqlUtil.like(keyword);
            params.add(k);
            params.add(k);
        }
        if (peran != null) {
            sql.append(" AND p.peran = ?");
            params.add(peran);
        }
        if (status != null) {
            sql.append(" AND p.status_aktif = ?");
            params.add(status);
        }
        sql.append(" ORDER BY p.peran, p.nama_lengkap");
        List<Pengguna> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    /** Petugas yang statusnya Aktif (untuk pilihan penugasan). */
    public List<Petugas> findPetugasAktif() throws SQLException {
        List<Petugas> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + " WHERE p.peran = ? AND p.status_aktif = ? ORDER BY t.spesialisasi, p.nama_lengkap")) {
            ps.setString(1, Peran.PETUGAS.getLabel());
            ps.setString(2, StatusAktif.AKTIF.getLabel());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add((Petugas) map(rs));
                }
            }
        }
        return result;
    }

    /** True bila username sudah dipakai; excludeId (opsional) dikecualikan saat mengubah data. */
    public boolean usernameExists(String username, Integer excludeId) throws SQLException {
        String sql = "SELECT 1 FROM pengguna WHERE username = ?" + (excludeId != null ? " AND id_pengguna <> ?" : "") + " LIMIT 1";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            if (excludeId != null) {
                ps.setInt(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** INSERT ke PENGGUNA (status_aktif = Aktif) lalu ke tabel subtype dengan id yang sama. Mengembalikan id baru. */
    public int insert(Connection c, Pengguna p) throws SQLException {
        int id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO pengguna (nama_lengkap, username, password, email, no_telepon, peran, status_aktif) VALUES (?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNamaLengkap());
            ps.setString(2, p.getUsername());
            ps.setString(3, p.getPassword());
            ps.setString(4, SqlUtil.nullIfBlank(p.getEmail()));
            ps.setString(5, p.getNoTelepon());
            ps.setString(6, p.getPeran().getLabel());
            ps.setString(7, StatusAktif.AKTIF.getLabel());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                id = k.getInt(1);
            }
        }
        if (p instanceof Admin a) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO admin (id_pengguna, hak_akses) VALUES (?,?)")) {
                ps.setInt(1, id);
                ps.setString(2, SqlUtil.nullIfBlank(a.getHakAkses()));
                ps.executeUpdate();
            }
        } else if (p instanceof Supervisor s) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO supervisor (id_pengguna, area_pengawasan) VALUES (?,?)")) {
                ps.setInt(1, id);
                ps.setString(2, SqlUtil.nullIfBlank(s.getAreaPengawasan()));
                ps.executeUpdate();
            }
        } else if (p instanceof Petugas t) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO petugas (id_pengguna, spesialisasi, shift_kerja) VALUES (?,?,?)")) {
                ps.setInt(1, id);
                ps.setString(2, t.getSpesialisasi().getLabel());
                ps.setString(3, t.getShiftKerja().getLabel());
                ps.executeUpdate();
            }
        }
        return id;
    }

    /** UPDATE PENGGUNA (nama, password, email, telepon; peran dan username tetap) dan tabel subtype. */
    public void update(Connection c, Pengguna p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE pengguna SET nama_lengkap = ?, password = ?, email = ?, no_telepon = ? WHERE id_pengguna = ?")) {
            ps.setString(1, p.getNamaLengkap());
            ps.setString(2, p.getPassword());
            ps.setString(3, SqlUtil.nullIfBlank(p.getEmail()));
            ps.setString(4, p.getNoTelepon());
            ps.setInt(5, p.getIdPengguna());
            ps.executeUpdate();
        }
        if (p instanceof Admin a) {
            try (PreparedStatement ps = c.prepareStatement("UPDATE admin SET hak_akses = ? WHERE id_pengguna = ?")) {
                ps.setString(1, SqlUtil.nullIfBlank(a.getHakAkses()));
                ps.setInt(2, a.getIdPengguna());
                ps.executeUpdate();
            }
        } else if (p instanceof Supervisor s) {
            try (PreparedStatement ps = c.prepareStatement("UPDATE supervisor SET area_pengawasan = ? WHERE id_pengguna = ?")) {
                ps.setString(1, SqlUtil.nullIfBlank(s.getAreaPengawasan()));
                ps.setInt(2, s.getIdPengguna());
                ps.executeUpdate();
            }
        } else if (p instanceof Petugas t) {
            try (PreparedStatement ps = c.prepareStatement("UPDATE petugas SET spesialisasi = ?, shift_kerja = ? WHERE id_pengguna = ?")) {
                ps.setString(1, t.getSpesialisasi().getLabel());
                ps.setString(2, t.getShiftKerja().getLabel());
                ps.setInt(3, t.getIdPengguna());
                ps.executeUpdate();
            }
        }
    }

    public int updateStatus(int idPengguna, StatusAktif status) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE pengguna SET status_aktif = ? WHERE id_pengguna = ?")) {
            ps.setString(1, status.getLabel());
            ps.setInt(2, idPengguna);
            return ps.executeUpdate();
        }
    }
}
