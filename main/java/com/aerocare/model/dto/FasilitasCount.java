package com.aerocare.model.dto;

/** Jumlah laporan per fasilitas (rekap dan deteksi gangguan berulang). */
public record FasilitasCount(int idFasilitas, String namaFasilitas, String jenisFasilitas, String lokasi, int jumlah) {
}
