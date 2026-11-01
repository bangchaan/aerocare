package com.aerocare.model.dto;

import com.aerocare.model.enums.Prioritas;
import com.aerocare.model.enums.StatusLaporan;
import com.aerocare.model.enums.StatusProgres;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Satu gangguan pada fasilitas tertentu beserta progres terakhirnya. */
public record RiwayatGangguan(int idLaporan, LocalDate tanggalLaporan, String deskripsi, Prioritas prioritas,
                              StatusLaporan status, Integer idPenugasan, StatusProgres progresTerakhir,
                              LocalDateTime waktuProgresTerakhir) {
}
