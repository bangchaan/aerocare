package com.aerocare.model.dto;

import com.aerocare.model.enums.Spesialisasi;
import com.aerocare.model.enums.StatusKeterlibatan;

/** Petugas yang terlibat dalam suatu penugasan. */
public record PetugasPenugasan(int idPetugas, String namaLengkap, Spesialisasi spesialisasi,
                               StatusKeterlibatan statusKeterlibatan) {
}
