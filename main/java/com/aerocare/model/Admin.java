package com.aerocare.model;

import com.aerocare.model.enums.Peran;
import com.aerocare.model.enums.StatusAktif;

/** Subtype ADMIN (tabel admin: hak_akses). */
public class Admin extends Pengguna {

    private final String hakAkses;

    public Admin(int idPengguna, String namaLengkap, String username, String password, String email,
                 String noTelepon, StatusAktif statusAktif, String hakAkses) {
        super(idPengguna, namaLengkap, username, password, email, noTelepon, Peran.ADMIN, statusAktif);
        this.hakAkses = hakAkses;
    }

    public String getHakAkses() { return hakAkses; }

    @Override
    public String getDataKhusus() {
        return hakAkses == null ? "-" : hakAkses;
    }
}
