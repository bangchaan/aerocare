package com.aerocare.model.dto;

import com.aerocare.model.enums.Prioritas;
import com.aerocare.model.enums.StatusLaporan;

import java.time.LocalDate;

/** Satu baris daftar/hasil pencarian laporan. petugas = nama petugas digabung koma (bisa null). */
public record LaporanRow(int idLaporan, LocalDate tanggalLaporan, String deskripsi, Prioritas prioritas,
                         StatusLaporan status, String namaFasilitas, String lokasi, String petugas) {
}
