package com.aerocare.dao;

import com.aerocare.config.DBConnection;
import com.aerocare.model.Penugasan;
import com.aerocare.model.dto.LaporanRef;
import com.aerocare.model.dto.PenugasanRingkas;
import com.aerocare.model.dto.PetugasPenugasan;
import com.aerocare.model.dto.TugasPetugas;
import com.aerocare.model.enums.Prioritas;
import com.aerocare.model.enums.Spesialisasi;
import com.aerocare.model.enums.StatusKeterlibatan;
import com.aerocare.model.enums.StatusLaporan;
import com.aerocare.model.enums.StatusProgres;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Akses tabel PENUGASAN dan DETAIL_PENUGASAN. */
public class PenugasanDAO {

    /** Dipakai di dalam transaksi. Mengembalikan id_penugasan baru. */
    public int insert(Connection c, Penugasan p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO penugasan (waktu_penugasan, catatan_penugasan, SUPERVISOR_id_pengguna,
                                       LAPORAN_KERUSAKAN_id_laporan, id_pengguna)
                VALUES (?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(p.waktuPenugasan()));
            ps.setString(2, SqlUtil.nullIfBlank(p.catatanPenugasan()));
            ps.setInt(3, p.supervisorIdPengguna());
            ps.setInt(4, p.laporanId());
            ps.setInt(5, p.idPengguna());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        }
    }

    /** Satu baris DETAIL_PENUGASAN per petugas terpilih (dalam transaksi). */
    public void insertDetail(Connection c, int idPenugasan, int idPetugas, StatusKeterlibatan status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO detail_penugasan (status_keterlibatan, PETUGAS_id_pengguna, PENUGASAN_id_penugasan) VALUES (?,?,?)")) {
            ps.setString(1, status.getLabel());
            ps.setInt(2, idPetugas);
            ps.setInt(3, idPenugasan);
            ps.executeUpdate();
        }
    }

    /** Menandai semua keterlibatan Aktif pada penugasan ini menjadi Selesai (dalam transaksi). */
    public int selesaikanDetail(Connection c, int idPenugasan) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE detail_penugasan SET status_keterlibatan = ? WHERE PENUGASAN_id_penugasan = ? AND status_keterlibatan = ?")) {
            ps.setString(1, StatusKeterlibatan.SELESAI.getLabel());
            ps.setInt(2, idPenugasan);
            ps.setString(3, StatusKeterlibatan.AKTIF.getLabel());
            return ps.executeUpdate();
        }
    }

    /** Rujukan laporan + fasilitas dari sebuah penugasan (dalam transaksi). */
    public Optional<LaporanRef> findLaporanRef(Connection c, int idPenugasan) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT l.id_laporan, l.FASILITAS_id_fasilitas
                FROM penugasan p JOIN laporan_kerusakan l ON l.id_laporan = p.LAPORAN_KERUSAKAN_id_laporan
                WHERE p.id_penugasan = ?""")) {
            ps.setInt(1, idPenugasan);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new LaporanRef(rs.getInt(1), rs.getInt(2))) : Optional.empty();
            }
        }
    }

    /** True bila petugas terlibat Aktif pada penugasan yang laporannya belum Selesai (hak update progres). */
    public boolean petugasBolehUpdate(Connection c, int idPetugas, int idPenugasan) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM detail_penugasan d
                JOIN penugasan p ON p.id_penugasan = d.PENUGASAN_id_penugasan
                JOIN laporan_kerusakan l ON l.id_laporan = p.LAPORAN_KERUSAKAN_id_laporan
                WHERE d.PETUGAS_id_pengguna = ? AND d.PENUGASAN_id_penugasan = ?
                  AND d.status_keterlibatan = ? AND l.status <> ? LIMIT 1""")) {
            ps.setInt(1, idPetugas);
            ps.setInt(2, idPenugasan);
            ps.setString(3, StatusKeterlibatan.AKTIF.getLabel());
            ps.setString(4, StatusLaporan.SELESAI.getLabel());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<PetugasPenugasan> findPetugasByPenugasan(int idPenugasan) throws SQLException {
        List<PetugasPenugasan> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT pg.id_pengguna, pg.nama_lengkap, t.spesialisasi, d.status_keterlibatan
                     FROM detail_penugasan d
                     JOIN pengguna pg ON pg.id_pengguna = d.PETUGAS_id_pengguna
                     JOIN petugas t ON t.id_pengguna = d.PETUGAS_id_pengguna
                     WHERE d.PENUGASAN_id_penugasan = ?
                     ORDER BY d.id_detail""")) {
            ps.setInt(1, idPenugasan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new PetugasPenugasan(rs.getInt(1), rs.getString(2),
                            Spesialisasi.fromLabel(rs.getString(3)), StatusKeterlibatan.fromLabel(rs.getString(4))));
                }
            }
        }
        return result;
    }

    /** Pantau progres (Supervisor): seluruh penugasan beserta petugas dan progres terakhir. */
    public List<PenugasanRingkas> findPantau() throws SQLException {
        String sql = """
                SELECT p.id_penugasan, p.waktu_penugasan, p.catatan_penugasan, l.id_laporan, l.status,
                       f.nama_fasilitas, f.lokasi,
                       (SELECT GROUP_CONCAT(CONCAT(pg.nama_lengkap,
                                    IF(d.status_keterlibatan = 'Digantikan', ' (digantikan)', ''))
                                    ORDER BY pg.nama_lengkap SEPARATOR ', ')
                          FROM detail_penugasan d
                          JOIN pengguna pg ON pg.id_pengguna = d.PETUGAS_id_pengguna
                         WHERE d.PENUGASAN_id_penugasan = p.id_penugasan) AS petugas,
                       pr.status_progres, pr.waktu_update
                FROM penugasan p
                JOIN laporan_kerusakan l ON l.id_laporan = p.LAPORAN_KERUSAKAN_id_laporan
                JOIN fasilitas f ON f.id_fasilitas = l.FASILITAS_id_fasilitas
                """ + SqlUtil.JOIN_PROGRES_TERAKHIR + " ORDER BY p.waktu_penugasan DESC, p.id_penugasan DESC";
        List<PenugasanRingkas> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String sp = rs.getString("status_progres");
                result.add(new PenugasanRingkas(rs.getInt("id_penugasan"), SqlUtil.date(rs, "waktu_penugasan"),
                        rs.getInt("id_laporan"), rs.getString("nama_fasilitas"), rs.getString("lokasi"),
                        StatusLaporan.fromLabel(rs.getString("status")), rs.getString("petugas"),
                        rs.getString("catatan_penugasan"), sp == null ? null : StatusProgres.fromLabel(sp),
                        SqlUtil.dateTime(rs, "waktu_update")));
            }
        }
        return result;
    }

    /** Tugas aktif petugas: keterlibatan Aktif dan status laporan bukan Selesai; urut prioritas. */
    public List<TugasPetugas> findTugasPetugas(int idPetugas) throws SQLException {
        String sql = """
                SELECT p.id_penugasan, l.id_laporan, f.nama_fasilitas, f.lokasi, l.prioritas, l.status,
                       p.catatan_penugasan, l.deskripsi_kerusakan, pr.status_progres
                FROM detail_penugasan d
                JOIN penugasan p ON p.id_penugasan = d.PENUGASAN_id_penugasan
                JOIN laporan_kerusakan l ON l.id_laporan = p.LAPORAN_KERUSAKAN_id_laporan
                JOIN fasilitas f ON f.id_fasilitas = l.FASILITAS_id_fasilitas
                """ + SqlUtil.JOIN_PROGRES_TERAKHIR + """
                WHERE d.PETUGAS_id_pengguna = ? AND d.status_keterlibatan = ? AND l.status <> ?
                ORDER BY FIELD(l.prioritas, 'Tinggi', 'Sedang', 'Rendah'), p.id_penugasan
                """;
        List<TugasPetugas> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPetugas);
            ps.setString(2, StatusKeterlibatan.AKTIF.getLabel());
            ps.setString(3, StatusLaporan.SELESAI.getLabel());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String sp = rs.getString("status_progres");
                    result.add(new TugasPetugas(rs.getInt("id_penugasan"), rs.getInt("id_laporan"),
                            rs.getString("nama_fasilitas"), rs.getString("lokasi"),
                            Prioritas.fromLabel(rs.getString("prioritas")), StatusLaporan.fromLabel(rs.getString("status")),
                            rs.getString("catatan_penugasan"), rs.getString("deskripsi_kerusakan"),
                            sp == null ? null : StatusProgres.fromLabel(sp)));
                }
            }
        }
        return result;
    }

    public int count() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM penugasan")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
