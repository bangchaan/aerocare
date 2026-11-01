package com.aerocare.model.dto;

import com.aerocare.model.enums.Prioritas;
import com.aerocare.model.enums.StatusLaporan;

import java.time.LocalDate;

/** Kriteria pencarian laporan; field null/kosong = tidak difilter. batasiPetugasId != null membatasi hasil untuk Petugas. */
public record CariCriteria(Integer idLaporan, LocalDate dari, LocalDate sampai, String fasilitasLokasi,
                           StatusLaporan status, Prioritas prioritas, String petugas, Integer batasiPetugasId) {

    public static CariCriteria kosong(Integer batasiPetugasId) {
        return new CariCriteria(null, null, null, null, null, null, null, batasiPetugasId);
    }
}
