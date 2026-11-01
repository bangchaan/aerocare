package com.aerocare.model.dto;

import com.aerocare.model.enums.StatusProgres;

import java.time.LocalDateTime;

/** Baris riwayat progres. namaFasilitas hanya terisi pada riwayat milik petugas (boleh null). */
public record ProgresRow(int idProgres, int idPenugasan, LocalDateTime waktuUpdate, StatusProgres statusProgres,
                         String tindakanPenanganan, String namaFasilitas) {
}
