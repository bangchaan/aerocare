package com.aerocare.model.dto;

import com.aerocare.model.enums.Prioritas;
import com.aerocare.model.enums.StatusLaporan;
import com.aerocare.model.enums.StatusProgres;

/** Tugas aktif milik seorang Petugas. */
public record TugasPetugas(int idPenugasan, int idLaporan, String namaFasilitas, String lokasi,
                           Prioritas prioritas, StatusLaporan statusLaporan, String catatanPenugasan,
                           String deskripsi, StatusProgres progresTerakhir) {
}
