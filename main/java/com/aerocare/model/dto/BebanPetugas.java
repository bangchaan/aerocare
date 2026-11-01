package com.aerocare.model.dto;

/** Jumlah penugasan dan laporan yang ditangani seorang petugas pada suatu periode. */
public record BebanPetugas(int idPetugas, String namaLengkap, int jumlahPenugasan, int jumlahLaporan) {
}
