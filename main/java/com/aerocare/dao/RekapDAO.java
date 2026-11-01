package com.aerocare.dao;

import com.aerocare.config.DBConnection;
import com.aerocare.model.dto.BebanPetugas;
import com.aerocare.model.dto.FasilitasCount;
import com.aerocare.model.dto.RiwayatGangguan;
import com.aerocare.model.dto.WaktuPenyelesaian;
import com.aerocare.model.enums.Prioritas;
import com.aerocare.model.enums.StatusLaporan;
import com.aerocare.model.enums.StatusProgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Query statistik untuk dashboard dan rekap. Parameter dari/sampai (tanggal_laporan) boleh null. */
public class RekapDAO {

    private Map<String, Integer> countGroup(String column, Iterable<String> urutan,
                                            LocalDate dari, LocalDate sampai) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String label : urutan) {
            result.put(label, 0);
        }
        StringBuilder sql = new StringBuilder("SELECT l." + column + ", COUNT(*) FROM laporan_kerusakan l WHERE 1=1");
        List<Object> params = new ArrayList<>();
        SqlUtil.periode("l", dari, sampai, sql, params);
        sql.append(" GROUP BY l.").append(column);
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getInt(2));
                }
            }
        }
        return result;
    }

    public Map<String, Integer> countByStatus(LocalDate dari, LocalDate sampai) throws SQLException {
        List<String> urutan = new ArrayList<>();
        for (StatusLaporan s : StatusLaporan.values()) {
            urutan.add(s.getLabel());
        }
        return countGroup("status", urutan, dari, sampai);
    }

    public Map<String, Integer> countByPrioritas(LocalDate dari, LocalDate sampai) throws SQLException {
        List<String> urutan = new ArrayList<>();
        for (Prioritas p : new Prioritas[]{Prioritas.TINGGI, Prioritas.SEDANG, Prioritas.RENDAH}) {
            urutan.add(p.getLabel());
        }
        return countGroup("prioritas", urutan, dari, sampai);
    }

    public int countLaporan(LocalDate dari, LocalDate sampai) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM laporan_kerusakan l WHERE 1=1");
        List<Object> params = new ArrayList<>();
        SqlUtil.periode("l", dari, sampai, sql, params);
        return scalar(sql.toString(), params);
    }

    /** Jumlah penugasan pada laporan dalam periode. */
    public int countPenugasan(LocalDate dari, LocalDate sampai) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM penugasan p
                JOIN laporan_kerusakan l ON l.id_laporan = p.LAPORAN_KERUSAKAN_id_laporan WHERE 1=1""");
        List<Object> params = new ArrayList<>();
        SqlUtil.periode("l", dari, sampai, sql, params);
        return scalar(sql.toString(), params);
    }

    private int scalar(String sql, List<Object> params) throws SQLException {
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Jumlah laporan per jenis fasilitas, terbanyak dulu. */
    public Map<String, Integer> countPerJenis(LocalDate dari, LocalDate sampai) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("""
                SELECT f.jenis_fasilitas, COUNT(*) AS jml FROM laporan_kerusakan l
                JOIN fasilitas f ON f.id_fasilitas = l.FASILITAS_id_fasilitas WHERE 1=1""");
        List<Object> params = new ArrayList<>();
        SqlUtil.periode("l", dari, sampai, sql, params);
        sql.append(" GROUP BY f.jenis_fasilitas ORDER BY jml DESC, f.jenis_fasilitas");
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getInt(2));
                }
            }
        }
        return result;
    }

    /** Jumlah laporan per fasilitas; hanyaBerulang = true hanya fasilitas dengan lebih dari satu laporan. */
    public List<FasilitasCount> countPerFasilitas(LocalDate dari, LocalDate sampai, boolean hanyaBerulang) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT f.id_fasilitas, f.nama_fasilitas, f.jenis_fasilitas, f.lokasi, COUNT(l.id_laporan) AS jml
                FROM fasilitas f JOIN laporan_kerusakan l ON l.FASILITAS_id_fasilitas = f.id_fasilitas WHERE 1=1""");
        List<Object> params = new ArrayList<>();
        SqlUtil.periode("l", dari, sampai, sql, params);
        sql.append(" GROUP BY f.id_fasilitas, f.nama_fasilitas, f.jenis_fasilitas, f.lokasi");
        if (hanyaBerulang) {
            sql.append(" HAVING COUNT(l.id_laporan) > 1");
        }
        sql.append(" ORDER BY jml DESC, f.nama_fasilitas");
        List<FasilitasCount> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new FasilitasCount(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5)));
                }
            }
        }
        return result;
    }

    /** Jumlah penugasan dan laporan yang ditangani tiap petugas (melalui DETAIL_PENUGASAN). */
    public List<BebanPetugas> bebanPetugas(LocalDate dari, LocalDate sampai) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT pg.id_pengguna, pg.nama_lengkap,
                       COUNT(DISTINCT CASE WHEN l.id_laporan IS NOT NULL THEN d.PENUGASAN_id_penugasan END) AS jml_penugasan,
                       COUNT(DISTINCT l.id_laporan) AS jml_laporan
                FROM petugas t
                JOIN pengguna pg ON pg.id_pengguna = t.id_pengguna
                LEFT JOIN detail_penugasan d ON d.PETUGAS_id_pengguna = t.id_pengguna
                LEFT JOIN penugasan p ON p.id_penugasan = d.PENUGASAN_id_penugasan
                LEFT JOIN laporan_kerusakan l ON l.id_laporan = p.LAPORAN_KERUSAKAN_id_laporan""");
        List<Object> params = new ArrayList<>();
        SqlUtil.periode("l", dari, sampai, sql, params);
        sql.append(" GROUP BY pg.id_pengguna, pg.nama_lengkap ORDER BY jml_laporan DESC, pg.nama_lengkap");
        List<BebanPetugas> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new BebanPetugas(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4)));
                }
            }
        }
        return result;
    }

    /** Laporan yang progres "Selesai"-nya tercatat: waktu_update terakhir "Selesai" dibanding tanggal_laporan. */
    public List<WaktuPenyelesaian> waktuPenyelesaian(LocalDate dari, LocalDate sampai) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT l.id_laporan, f.nama_fasilitas, l.tanggal_laporan, MAX(pr.waktu_update) AS selesai
                FROM laporan_kerusakan l
                JOIN fasilitas f ON f.id_fasilitas = l.FASILITAS_id_fasilitas
                JOIN penugasan p ON p.LAPORAN_KERUSAKAN_id_laporan = l.id_laporan
                JOIN progres_penanganan pr ON pr.PENUGASAN_id_penugasan = p.id_penugasan AND pr.status_progres = ?
                WHERE 1=1""");
        List<Object> params = new ArrayList<>();
        params.add(StatusProgres.SELESAI);
        SqlUtil.periode("l", dari, sampai, sql, params);
        sql.append(" GROUP BY l.id_laporan, f.nama_fasilitas, l.tanggal_laporan ORDER BY l.id_laporan");
        List<WaktuPenyelesaian> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            SqlUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new WaktuPenyelesaian(rs.getInt(1), rs.getString(2),
                            SqlUtil.date(rs, "tanggal_laporan"), SqlUtil.dateTime(rs, "selesai")));
                }
            }
        }
        return result;
    }

    /** Riwayat gangguan dan penanganan satu fasilitas, terbaru dulu. */
    public List<RiwayatGangguan> riwayatFasilitas(int idFasilitas) throws SQLException {
        String sql = """
                SELECT l.id_laporan, l.tanggal_laporan, l.deskripsi_kerusakan, l.prioritas, l.status,
                       p.id_penugasan, pr.status_progres, pr.waktu_update
                FROM laporan_kerusakan l
                LEFT JOIN penugasan p ON p.LAPORAN_KERUSAKAN_id_laporan = l.id_laporan
                """ + SqlUtil.JOIN_PROGRES_TERAKHIR + """
                WHERE l.FASILITAS_id_fasilitas = ?
                ORDER BY l.tanggal_laporan DESC, l.id_laporan DESC
                """;
        List<RiwayatGangguan> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFasilitas);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String sp = rs.getString("status_progres");
                    result.add(new RiwayatGangguan(rs.getInt("id_laporan"), SqlUtil.date(rs, "tanggal_laporan"),
                            rs.getString("deskripsi_kerusakan"), Prioritas.fromLabel(rs.getString("prioritas")),
                            StatusLaporan.fromLabel(rs.getString("status")), SqlUtil.intOrNull(rs, "id_penugasan"),
                            sp == null ? null : StatusProgres.fromLabel(sp), SqlUtil.dateTime(rs, "waktu_update")));
                }
            }
        }
        return result;
    }
}
