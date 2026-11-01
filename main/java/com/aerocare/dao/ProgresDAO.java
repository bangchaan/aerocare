package com.aerocare.dao;

import com.aerocare.config.DBConnection;
import com.aerocare.model.ProgresPenanganan;
import com.aerocare.model.dto.ProgresRow;
import com.aerocare.model.enums.StatusProgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Akses tabel PROGRES_PENANGANAN. */
public class ProgresDAO {

    /** Dipakai di dalam transaksi. Mengembalikan id_progres baru. */
    public int insert(Connection c, ProgresPenanganan p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO progres_penanganan (waktu_update, status_progres, tindakan_penanganan, PENUGASAN_id_penugasan) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(p.waktuUpdate()));
            ps.setString(2, p.statusProgres().getLabel());
            ps.setString(3, SqlUtil.nullIfBlank(p.tindakanPenanganan()));
            ps.setInt(4, p.penugasanId());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        }
    }

    private static ProgresRow map(ResultSet rs) throws SQLException {
        return new ProgresRow(rs.getInt("id_progres"), rs.getInt("PENUGASAN_id_penugasan"),
                SqlUtil.dateTime(rs, "waktu_update"), StatusProgres.fromLabel(rs.getString("status_progres")),
                rs.getString("tindakan_penanganan"), rs.getString("nama_fasilitas"));
    }

    /** Riwayat progres satu penugasan, urut waktu naik. */
    public List<ProgresRow> findByPenugasan(int idPenugasan) throws SQLException {
        List<ProgresRow> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT id_progres, PENUGASAN_id_penugasan, waktu_update, status_progres, tindakan_penanganan,
                            NULL AS nama_fasilitas
                     FROM progres_penanganan WHERE PENUGASAN_id_penugasan = ?
                     ORDER BY waktu_update, id_progres""")) {
            ps.setInt(1, idPenugasan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    /** Riwayat progres pada penugasan yang melibatkan petugas ini, terbaru dulu. */
    public List<ProgresRow> findRiwayatPetugas(int idPetugas) throws SQLException {
        List<ProgresRow> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT pr.id_progres, pr.PENUGASAN_id_penugasan, pr.waktu_update, pr.status_progres,
                            pr.tindakan_penanganan, f.nama_fasilitas
                     FROM progres_penanganan pr
                     JOIN penugasan p ON p.id_penugasan = pr.PENUGASAN_id_penugasan
                     JOIN laporan_kerusakan l ON l.id_laporan = p.LAPORAN_KERUSAKAN_id_laporan
                     JOIN fasilitas f ON f.id_fasilitas = l.FASILITAS_id_fasilitas
                     WHERE EXISTS (SELECT 1 FROM detail_penugasan d
                                   WHERE d.PENUGASAN_id_penugasan = p.id_penugasan AND d.PETUGAS_id_pengguna = ?)
                     ORDER BY pr.waktu_update DESC, pr.id_progres DESC""")) {
            ps.setInt(1, idPetugas);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }
}
