package com.aerocare.model.dto;

import com.aerocare.model.enums.StatusLaporan;
import com.aerocare.model.enums.StatusProgres;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Baris pemantauan progres untuk Supervisor. progresTerakhir/waktuProgresTerakhir bernilai null bila belum ada progres. */
public record PenugasanRingkas(int idPenugasan, LocalDate waktuPenugasan, int idLaporan, String namaFasilitas,
                               String lokasi, StatusLaporan statusLaporan, String petugas, String catatan,
                               StatusProgres progresTerakhir, LocalDateTime waktuProgresTerakhir) {
}
