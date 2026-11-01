package com.aerocare.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Waktu penyelesaian satu laporan: waktu_update progres "Selesai" dikurangi tanggal_laporan (dalam hari). */
public record WaktuPenyelesaian(int idLaporan, String namaFasilitas, LocalDate tanggalLaporan,
                                LocalDateTime waktuSelesai) {

    public long hari() {
        return ChronoUnit.DAYS.between(tanggalLaporan, waktuSelesai.toLocalDate());
    }

    public static double rataRataHari(List<WaktuPenyelesaian> data) {
        return data.stream().mapToLong(WaktuPenyelesaian::hari).average().orElse(0);
    }
}
