package com.aerocare.model;

import com.aerocare.model.enums.Prioritas;
import com.aerocare.model.enums.StatusLaporan;

import java.time.LocalDate;

/**
 * Baris tabel LAPORAN_KERUSAKAN.
 * Kolom PENUGASAN_id_penugasan bersifat NOT NULL di database, sehingga laporan yang belum ditugaskan
 * disimpan dengan nilai sentinel {@link #BELUM_DITUGASKAN} (0).
 */
public record LaporanKerusakan(int idLaporan, LocalDate tanggalLaporan, String deskripsiKerusakan,
                               Prioritas prioritas, StatusLaporan status, int fasilitasId,
                               int adminIdPengguna, int penugasanId) {

    public static final int BELUM_DITUGASKAN = 0;

    public boolean belumDitugaskan() {
        return penugasanId == BELUM_DITUGASKAN;
    }
}
