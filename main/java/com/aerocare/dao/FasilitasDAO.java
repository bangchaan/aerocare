package com.aerocare.dao;

import com.aerocare.config.DBConnection;
import com.aerocare.model.Fasilitas;
import com.aerocare.model.enums.Kondisi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Akses tabel FASILITAS. */
public class FasilitasDAO {

    private static final String SELECT =
            "SELECT id_fasilitas, nama_fasilitas, jenis_fasilitas, lokasi, kondisi, keterangan, ADMIN_id_pengguna FROM fasilitas";

    private static Fasilitas map(ResultSet rs) throws SQLException {
        return new Fasilitas(rs.getInt("id_fasilitas"), rs.getString("nama_fasilitas"), rs.getString("jenis_fasilitas"),
                rs.getString("lokasi"), Kondisi.fromLabel(rs.getString("kondisi")), rs.getString("keterangan"),
                rs.getInt("ADMIN_id_pengguna"));
    }

    /** Pencarian fasilitas; keyword mencakup nama, jenis, dan lokasi. Semua filter boleh null. */
    public List<Fasilitas> search(String keyword, Kondisi kondisi, String jenis) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (nama_fasilitas LIKE ? OR jenis_fasilitas LIKE ? OR lokasi LIKE ?)");
            String k = SqlUtil.like(keyword);
            params.add(k);
            params.add(k);
            params.add(k);
        }
        if (kondisi != null) {
            sql.append(" AND kondisi = ?");
            params.add(kondisi);
        }
        if (jenis != null && !jenis.isBlank()) {
            sql.append(" AND jenis_fasilitas = ?");
            params.add(jenis);
        }
        sql.append(" ORDER BY id_fasilitas");
        List<Fasilitas> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public Optional<Fasilitas> findById(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + " WHERE id_fasilitas = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Jenis fasilitas yang sudah ada di database (untuk combo box). */
    public List<String> findJenisDistinct() throws SQLException {
        List<String> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT jenis_fasilitas FROM fasilitas ORDER BY jenis_fasilitas")) {
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        }
        return result;
    }

    /** Mengembalikan id_fasilitas baru. */
    public int insert(Fasilitas f) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO fasilitas (nama_fasilitas, jenis_fasilitas, lokasi, kondisi, keterangan, ADMIN_id_pengguna) VALUES (?,?,?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.namaFasilitas());
            ps.setString(2, f.jenisFasilitas());
            ps.setString(3, f.lokasi());
            ps.setString(4, f.kondisi().getLabel());
            ps.setString(5, SqlUtil.nullIfBlank(f.keterangan()));
            ps.setInt(6, f.adminIdPengguna());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        }
    }

    public int update(Fasilitas f) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE fasilitas SET nama_fasilitas = ?, jenis_fasilitas = ?, lokasi = ?, kondisi = ?, keterangan = ? WHERE id_fasilitas = ?")) {
            ps.setString(1, f.namaFasilitas());
            ps.setString(2, f.jenisFasilitas());
            ps.setString(3, f.lokasi());
            ps.setString(4, f.kondisi().getLabel());
            ps.setString(5, SqlUtil.nullIfBlank(f.keterangan()));
            ps.setInt(6, f.idFasilitas());
            return ps.executeUpdate();
        }
    }

    /** True bila fasilitas sudah memiliki riwayat laporan kerusakan (tidak boleh dihapus). */
    public boolean hasLaporan(int idFasilitas) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM laporan_kerusakan WHERE FASILITAS_id_fasilitas = ? LIMIT 1")) {
            ps.setInt(1, idFasilitas);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int delete(int idFasilitas) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM fasilitas WHERE id_fasilitas = ?")) {
            ps.setInt(1, idFasilitas);
            return ps.executeUpdate();
        }
    }

    /** Dipakai di dalam transaksi (catat laporan = Rusak, progres selesai = Baik). */
    public int updateKondisi(Connection c, int idFasilitas, Kondisi kondisi) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE fasilitas SET kondisi = ? WHERE id_fasilitas = ?")) {
            ps.setString(1, kondisi.getLabel());
            ps.setInt(2, idFasilitas);
            return ps.executeUpdate();
        }
    }

    public int count() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM fasilitas")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Jumlah fasilitas per kondisi (semua kondisi ENUM selalu ada, minimal 0). */
    public Map<String, Integer> countByKondisi() throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Kondisi k : Kondisi.values()) {
            result.put(k.getLabel(), 0);
        }
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT kondisi, COUNT(*) FROM fasilitas GROUP BY kondisi")) {
            while (rs.next()) {
                result.put(rs.getString(1), rs.getInt(2));
            }
        }
        return result;
    }
}
