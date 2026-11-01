package com.aerocare.model.dto;

import com.aerocare.model.enums.Kondisi;

import java.time.LocalDate;
import java.util.List;

/** Detail lengkap satu laporan: fasilitas, penugasan, petugas, dan riwayat progres. */
public record LaporanDetail(LaporanRow ringkas, String jenisFasilitas, Kondisi kondisiFasilitas,
                            String keteranganFasilitas, String dicatatOleh, Integer idPenugasan,
                            LocalDate waktuPenugasan, String catatanPenugasan, String supervisor,
                            List<PetugasPenugasan> petugas, List<ProgresRow> riwayat) {
}
