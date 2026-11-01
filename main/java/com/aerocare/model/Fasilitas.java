package com.aerocare.model;

import com.aerocare.model.enums.Kondisi;

/** Baris tabel FASILITAS. idFasilitas diabaikan (0) saat INSERT karena AUTO_INCREMENT. */
public record Fasilitas(int idFasilitas, String namaFasilitas, String jenisFasilitas, String lokasi,
                        Kondisi kondisi, String keterangan, int adminIdPengguna) {
}
