package com.aerocare.model;

import com.aerocare.model.enums.Peran;
import com.aerocare.model.enums.ShiftKerja;
import com.aerocare.model.enums.Spesialisasi;
import com.aerocare.model.enums.StatusAktif;

/** Subtype PETUGAS (tabel petugas: spesialisasi, shift_kerja). */
public class Petugas extends Pengguna {

    private final Spesialisasi spesialisasi;
    private final ShiftKerja shiftKerja;

    public Petugas(int idPengguna, String namaLengkap, String username, String password, String email,
                   String noTelepon, StatusAktif statusAktif, Spesialisasi spesialisasi, ShiftKerja shiftKerja) {
        super(idPengguna, namaLengkap, username, password, email, noTelepon, Peran.PETUGAS, statusAktif);
        this.spesialisasi = spesialisasi;
        this.shiftKerja = shiftKerja;
    }

    public Spesialisasi getSpesialisasi() { return spesialisasi; }
    public ShiftKerja getShiftKerja() { return shiftKerja; }

    @Override
    public String getDataKhusus() {
        return spesialisasi + " - Shift " + shiftKerja;
    }
}
