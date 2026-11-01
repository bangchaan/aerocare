package com.aerocare;

import com.aerocare.config.DBConnection;
import com.aerocare.config.DatabaseConfig;
import com.aerocare.dao.FasilitasDAO;
import com.aerocare.dao.LaporanDAO;
import com.aerocare.dao.PenggunaDAO;
import com.aerocare.dao.PenugasanDAO;
import com.aerocare.dao.RekapDAO;
import com.aerocare.model.dto.CariCriteria;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Uji koneksi dan DAO (Tahap 3). Jalankan:
 *   mvn compile exec:java
 * Hanya membaca data (tidak ada INSERT/UPDATE/DELETE).
 */
public class ConnectionTest {

    private static final String[] TABEL = {
        "pengguna", "admin", "supervisor", "petugas", "fasilitas",
        "laporan_kerusakan", "penugasan", "detail_penugasan", "progres_penanganan"
    };

    public static void main(String[] args) {
        System.out.println("URL : " + DatabaseConfig.get().url());
        if (!DBConnection.test()) {
            System.out.println("GAGAL: tidak dapat terhubung. Periksa db.properties dan pastikan MySQL berjalan.");
            return;
        }
        System.out.println("Koneksi MySQL: BERHASIL");
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement()) {
            for (String t : TABEL) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + t)) {
                    rs.next();
                    System.out.printf("  %-20s %d baris%n", t, rs.getInt(1));
                }
            }
            System.out.println("Uji DAO:");
            System.out.println("  PenggunaDAO.search          : " + new PenggunaDAO().search(null, null, null).size());
            System.out.println("  PenggunaDAO.findPetugasAktif: " + new PenggunaDAO().findPetugasAktif().size());
            System.out.println("  FasilitasDAO.search         : " + new FasilitasDAO().search(null, null, null).size());
            System.out.println("  LaporanDAO.search           : " + new LaporanDAO().search(CariCriteria.kosong(null)).size());
            System.out.println("  LaporanDAO.belum ditugaskan : " + new LaporanDAO().findMenungguBelumDitugaskan().size());
            System.out.println("  PenugasanDAO.findPantau     : " + new PenugasanDAO().findPantau().size());
            System.out.println("  RekapDAO.countByStatus      : " + new RekapDAO().countByStatus(null, null));
            System.out.println("  RekapDAO.fasilitas berulang : " + new RekapDAO().countPerFasilitas(null, null, true).size());
        } catch (SQLException e) {
            System.out.println("Galat SQL: " + e.getMessage());
        }
    }
}
