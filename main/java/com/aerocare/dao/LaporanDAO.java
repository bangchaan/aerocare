package com.aerocare.dao;

import com.aerocare.config.DBConnection;
import com.aerocare.model.LaporanKerusakan;
import com.aerocare.model.dto.CariCriteria;
import com.aerocare.model.dto.LaporanDetail;
import com.aerocare.model.dto.LaporanRow;
import com.aerocare.model.dto.PetugasPenugasan;
import com.aerocare.model.dto.ProgresRow;
import com.aerocare.model.enums.Kondisi;
import com.aerocare.model.enums.Prioritas;
import com.aerocare.model.enums.StatusLaporan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Akses tabel LAPORAN_KERUSAKAN (dengan JOIN ke FASILITAS, PENUGASAN, DETAIL_PENUGASAN). */
public class LaporanDAO {

    private static final String PETUGAS_SUBQUERY = """
            (SELECT GROUP_CONCAT(DISTINCT pg.nama_lengkap ORDER BY pg.nama_lengkap SEPARATOR ', ')
               FROM penugasan p2
               JOIN detail_penugasan d ON d.PENUGASAN_id_penugasan = p2.id_penugasan
               JOIN pengguna pg ON pg.id_pengguna = d.PETUGAS_id_pengguna
              WHERE p2.LAPORAN_KERUSAKAN_id_laporan = l.id_laporan)""";

    private static final String BASE_ROWS = """
            SELECT l.id_laporan, l.tanggal_laporan, l.deskripsi_kerusakan, l.prioritas, l.status,
                   f.nama_fasilitas, f.lokasi, %s AS petugas
            FROM laporan_kerusakan l
            JOIN fasilitas f ON f.id_fasilitas = l.FASILITAS_id_fasilitas
            """.formatted(PETUGAS_SUBQUERY);

    private static LaporanRow mapRow(ResultSet rs) throws SQLException {
        return new LaporanRow(rs.getInt("id_laporan"), SqlUtil.date(rs, "tanggal_laporan"),
                rs.getString("deskripsi_kerusakan"), Prioritas.fromLabel(rs.getString("prioritas")),
                StatusLaporan.fromLabel(rs.getString("status")), rs.getString("nama_fasilitas"),
                rs.getString("lokasi"), rs.getString("petugas"));
    }

    private List<LaporanRow> queryRows(String whereAndOrder, List<Object> params) throws SQLException {
        List<LaporanRow> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_ROWS + whereAndOrder)) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /**
     * Menyimpan laporan baru (status Menunggu) milik admin. Karena PENUGASAN_id_penugasan NOT NULL dan ber-FK,
     * laporan disimpan dengan sentinel 0 (belum ditugaskan); pengecekan FK dimatikan hanya selama INSERT ini
     * pada sesi koneksi, lalu dinyalakan kembali. Harus dipanggil di dalam transaksi. Mengembalikan id_laporan.
     */
    public int insertBaru(Connection c, LocalDate tanggal, String deskripsi, Prioritas prioritas,
                          int fasilitasId, int adminId) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
        }
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO laporan_kerusakan (tanggal_laporan, deskripsi_kerusakan, prioritas, status,
                    FASILITAS_id_fasilitas, ADMIN_id_pengguna, PENUGASAN_id_penugasan)
                VALUES (?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, java.sql.Date.valueOf(tanggal));
            ps.setString(2, deskripsi);
            ps.setString(3, prioritas.getLabel());
            ps.setString(4, StatusLaporan.MENUNGGU.getLabel());
            ps.setInt(5, fasilitasId);
            ps.setInt(6, adminId);
            ps.setInt(7, LaporanKerusakan.BELUM_DITUGASKAN);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        } finally {
            try (Statement st = c.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    /** Ubah deskripsi dan prioritas; hanya laporan milik admin tsb yang masih berstatus Menunggu. */
    public int updateByAdmin(int idLaporan, int adminId, String deskripsi, Prioritas prioritas) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     UPDATE laporan_kerusakan SET deskripsi_kerusakan = ?, prioritas = ?
                     WHERE id_laporan = ? AND ADMIN_id_pengguna = ? AND status = ?""")) {
            ps.setString(1, deskripsi);
            ps.setString(2, prioritas.getLabel());
            ps.setInt(3, idLaporan);
            ps.setInt(4, adminId);
            ps.setString(5, StatusLaporan.MENUNGGU.getLabel());
            return ps.executeUpdate();
        }
    }

    /** Dipakai dalam transaksi penugasan: mengisi id penugasan asli (hanya bila masih sentinel 0 dan Menunggu). */
    public int assignPenugasan(Connection c, int idLaporan, int idPenugasan, StatusLaporan statusBaru) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE laporan_kerusakan SET PENUGASAN_id_penugasan = ?, status = ?
                WHERE id_laporan = ? AND PENUGASAN_id_penugasan = ? AND status = ?""")) {
            ps.setInt(1, idPenugasan);
            ps.setString(2, statusBaru.getLabel());
            ps.setInt(3, idLaporan);
            ps.setInt(4, LaporanKerusakan.BELUM_DITUGASKAN);
            ps.setString(5, StatusLaporan.MENUNGGU.getLabel());
            return ps.executeUpdate();
        }
    }

    public int updateStatus(Connection c, int idLaporan, StatusLaporan status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE laporan_kerusakan SET status = ? WHERE id_laporan = ?")) {
            ps.setString(1, status.getLabel());
            ps.setInt(2, idLaporan);
            return ps.executeUpdate();
        }
    }

    public Optional<LaporanKerusakan> findById(int idLaporan) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT id_laporan, tanggal_laporan, deskripsi_kerusakan, prioritas, status,
                            FASILITAS_id_fasilitas, ADMIN_id_pengguna, PENUGASAN_id_penugasan
                     FROM laporan_kerusakan WHERE id_laporan = ?""")) {
            ps.setInt(1, idLaporan);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new LaporanKerusakan(rs.getInt("id_laporan"), SqlUtil.date(rs, "tanggal_laporan"),
                        rs.getString("deskripsi_kerusakan"), Prioritas.fromLabel(rs.getString("prioritas")),
                        StatusLaporan.fromLabel(rs.getString("status")), rs.getInt("FASILITAS_id_fasilitas"),
                        rs.getInt("ADMIN_id_pengguna"), rs.getInt("PENUGASAN_id_penugasan")));
            }
        }
    }

    /** Laporan yang dicatat admin tertentu; hanyaMenunggu = true untuk daftar yang masih dapat diubah. */
    public List<LaporanRow> findByAdmin(int adminId, boolean hanyaMenunggu) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder w = new StringBuilder(" WHERE l.ADMIN_id_pengguna = ?");
        params.add(adminId);
        if (hanyaMenunggu) {
            w.append(" AND l.status = ?");
            params.add(StatusLaporan.MENUNGGU);
        }
        w.append(" ORDER BY l.tanggal_laporan DESC, l.id_laporan DESC");
        return queryRows(w.toString(), params);
    }

    /** Laporan Menunggu yang belum memiliki penugasan (sentinel 0), urut prioritas lalu tanggal. */
    public List<LaporanRow> findMenungguBelumDitugaskan() throws SQLException {
        List<Object> params = new ArrayList<>();
        params.add(StatusLaporan.MENUNGGU);
        params.add(LaporanKerusakan.BELUM_DITUGASKAN);
        return queryRows(" WHERE l.status = ? AND l.PENUGASAN_id_penugasan = ?"
                + " ORDER BY FIELD(l.prioritas, 'Tinggi', 'Sedang', 'Rendah'), l.tanggal_laporan, l.id_laporan", params);
    }

    /** Pencarian laporan dengan kriteria dinamis; batasiPetugasId membatasi hasil untuk peran Petugas. */
    public List<LaporanRow> search(CariCriteria k) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        if (k.idLaporan() != null) {
            w.append(" AND l.id_laporan = ?");
            params.add(k.idLaporan());
        }
        if (k.dari() != null) {
            w.append(" AND l.tanggal_laporan >= ?");
            params.add(k.dari());
        }
        if (k.sampai() != null) {
            w.append(" AND l.tanggal_laporan <= ?");
            params.add(k.sampai());
        }
        if (k.fasilitasLokasi() != null && !k.fasilitasLokasi().isBlank()) {
            w.append(" AND (f.nama_fasilitas LIKE ? OR f.lokasi LIKE ?)");
            String like = SqlUtil.like(k.fasilitasLokasi());
            params.add(like);
            params.add(like);
        }
        if (k.status() != null) {
            w.append(" AND l.status = ?");
            params.add(k.status());
        }
        if (k.prioritas() != null) {
            w.append(" AND l.prioritas = ?");
            params.add(k.prioritas());
        }
        if (k.petugas() != null && !k.petugas().isBlank()) {
            w.append(" ").append("""
                     AND EXISTS (SELECT 1 FROM penugasan px
                                 JOIN detail_penugasan dx ON dx.PENUGASAN_id_penugasan = px.id_penugasan
                                 JOIN pengguna gx ON gx.id_pengguna = dx.PETUGAS_id_pengguna
                                 WHERE px.LAPORAN_KERUSAKAN_id_laporan = l.id_laporan AND gx.nama_lengkap LIKE ?)""");
            params.add(SqlUtil.like(k.petugas()));
        }
        if (k.batasiPetugasId() != null) {
            w.append(" ").append("""
                     AND EXISTS (SELECT 1 FROM penugasan py
                                 JOIN detail_penugasan dy ON dy.PENUGASAN_id_penugasan = py.id_penugasan
                                 WHERE py.LAPORAN_KERUSAKAN_id_laporan = l.id_laporan AND dy.PETUGAS_id_pengguna = ?)""");
            params.add(k.batasiPetugasId());
        }
        w.append(" ORDER BY l.tanggal_laporan DESC, l.id_laporan DESC");
        return queryRows(w.toString(), params);
    }

    /** Detail laporan; batasiPetugasId (opsional) memastikan petugas hanya melihat laporan terkait penugasannya. */
    public Optional<LaporanDetail> findDetail(int idLaporan, Integer batasiPetugasId) throws SQLException {
        String sql = """
                SELECT l.id_laporan, l.tanggal_laporan, l.deskripsi_kerusakan, l.prioritas, l.status,
                       f.nama_fasilitas, f.lokasi, f.jenis_fasilitas, f.kondisi, f.keterangan,
                       adm.nama_lengkap AS dicatat_oleh,
                       p.id_penugasan, p.waktu_penugasan, p.catatan_penugasan, sv.nama_lengkap AS supervisor,
                       %s AS petugas
                FROM laporan_kerusakan l
                JOIN fasilitas f ON f.id_fasilitas = l.FASILITAS_id_fasilitas
                JOIN pengguna adm ON adm.id_pengguna = l.ADMIN_id_pengguna
                LEFT JOIN penugasan p ON p.LAPORAN_KERUSAKAN_id_laporan = l.id_laporan
                LEFT JOIN pengguna sv ON sv.id_pengguna = p.SUPERVISOR_id_pengguna
                WHERE l.id_laporan = ?
                """.formatted(PETUGAS_SUBQUERY);
        List<Object> params = new ArrayList<>();
        params.add(idLaporan);
        if (batasiPetugasId != null) {
            sql += """
                    AND EXISTS (SELECT 1 FROM detail_penugasan d
                                WHERE d.PENUGASAN_id_penugasan = p.id_penugasan AND d.PETUGAS_id_pengguna = ?)
                    """;
            params.add(batasiPetugasId);
        }
        sql += " LIMIT 1";

        LaporanRow ringkas;
        String jenis;
        Kondisi kondisi;
        String keterangan;
        String dicatatOleh;
        Integer idPenugasan;
        LocalDate waktuPenugasan;
        String catatan;
        String supervisor;
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                ringkas = mapRow(rs);
                jenis = rs.getString("jenis_fasilitas");
                kondisi = Kondisi.fromLabel(rs.getString("kondisi"));
                keterangan = rs.getString("keterangan");
                dicatatOleh = rs.getString("dicatat_oleh");
                idPenugasan = SqlUtil.intOrNull(rs, "id_penugasan");
                waktuPenugasan = SqlUtil.date(rs, "waktu_penugasan");
                catatan = rs.getString("catatan_penugasan");
                supervisor = rs.getString("supervisor");
            }
        }
        List<PetugasPenugasan> petugas = new ArrayList<>();
        List<ProgresRow> riwayat = new ArrayList<>();
        if (idPenugasan != null) {
            petugas = new PenugasanDAO().findPetugasByPenugasan(idPenugasan);
            riwayat = new ProgresDAO().findByPenugasan(idPenugasan);
        }
        return Optional.of(new LaporanDetail(ringkas, jenis, kondisi, keterangan, dicatatOleh, idPenugasan,
                waktuPenugasan, catatan, supervisor, petugas, riwayat));
    }
}
