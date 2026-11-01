# AeroCare - Airport Facility Management System (Java Swing + MySQL)

Status: **Tahap 3** (koneksi database, model, DAO, aset logo). GUI dibuat di Tahap 4.

## Menjalankan uji koneksi
1. Import `aero.sql` ke MySQL (database `aerocare`). Struktur tabel tidak diubah.
2. Ubah `src/main/resources/db.properties` (url, user, password), atau set variabel lingkungan
   `AEROCARE_DB_URL`, `AEROCARE_DB_USER`, `AEROCARE_DB_PASSWORD`.
3. Jalankan: `mvn compile exec:java` (kelas `com.aerocare.ConnectionTest`, hanya membaca data).

## Struktur
- `config`  : DatabaseConfig, DBConnection, Transaction (commit/rollback)
- `model`   : Pengguna (supertype) + Admin/Supervisor/Petugas, Fasilitas, LaporanKerusakan, Penugasan, DetailPenugasan, ProgresPenanganan
  - `enums` : label identik dengan ENUM database; `dto`: hasil JOIN dan statistik
- `dao`     : PenggunaDAO, FasilitasDAO, LaporanDAO, PenugasanDAO, ProgresDAO, RekapDAO (semua PreparedStatement)
- `service`, `view`, `util` : diisi pada Tahap 4

## Catatan desain penting
- Database tidak diubah. Kolom `laporan_kerusakan.PENUGASAN_id_penugasan` NOT NULL + FK, padahal laporan dibuat
  sebelum ada penugasan. Solusi: laporan baru disimpan dengan **sentinel 0** (belum ditugaskan) di
  `LaporanDAO.insertBaru` (FK check dimatikan hanya selama INSERT itu pada sesi koneksi, lalu dinyalakan lagi).
  `LaporanDAO.assignPenugasan` mengganti 0 dengan id penugasan asli saat Supervisor menugaskan.
  Keterbatasan: mengimpor ulang dump dengan FK aktif dapat menolak baris yang masih bernilai 0.
- Status mengikuti ENUM database: penugasan membuat laporan `Diproses` dan detail `Aktif`;
  progres `Selesai` membuat laporan `Selesai`, fasilitas `Baik`, detail `Selesai`.
- `penugasan.id_pengguna` diisi petugas pertama yang dipilih Supervisor.

## Logo
Aset ada di `src/main/resources/images/` (PNG transparan + SVG). Dibuat ulang dengan `python3 tools/generate_logo.py`.
