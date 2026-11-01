package com.aerocare.model;

import com.aerocare.model.enums.StatusKeterlibatan;

/** Baris tabel DETAIL_PENUGASAN. */
public record DetailPenugasan(int idDetail, StatusKeterlibatan statusKeterlibatan,
                              int petugasIdPengguna, int penugasanId) {
}
