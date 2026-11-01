package com.aerocare.model;

import com.aerocare.model.enums.Peran;
import com.aerocare.model.enums.StatusAktif;

/** Subtype SUPERVISOR (tabel supervisor: area_pengawasan). */
public class Supervisor extends Pengguna {

    private final String areaPengawasan;

    public Supervisor(int idPengguna, String namaLengkap, String username, String password, String email,
                      String noTelepon, StatusAktif statusAktif, String areaPengawasan) {
        super(idPengguna, namaLengkap, username, password, email, noTelepon, Peran.SUPERVISOR, statusAktif);
        this.areaPengawasan = areaPengawasan;
    }

    public String getAreaPengawasan() { return areaPengawasan; }

    @Override
    public String getDataKhusus() {
        return areaPengawasan == null ? "-" : areaPengawasan;
    }
}
