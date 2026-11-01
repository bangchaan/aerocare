package com.aerocare.model;

import com.aerocare.model.enums.StatusProgres;

import java.time.LocalDateTime;

/** Baris tabel PROGRES_PENANGANAN. */
public record ProgresPenanganan(int idProgres, LocalDateTime waktuUpdate, StatusProgres statusProgres,
                                String tindakanPenanganan, int penugasanId) {
}
