package com.aerocare.model;

import java.time.LocalDate;

/** Baris tabel PENUGASAN. idPengguna = kolom penugasan.id_pengguna (diisi petugas pertama yang dipilih). */
public record Penugasan(int idPenugasan, LocalDate waktuPenugasan, String catatanPenugasan,
                        int supervisorIdPengguna, int laporanId, int idPengguna) {
}
