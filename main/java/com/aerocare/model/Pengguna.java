package com.aerocare.model;

import com.aerocare.model.enums.Peran;
import com.aerocare.model.enums.StatusAktif;

/** Supertype untuk tabel PENGGUNA; subtype: Admin, Supervisor, Petugas. */
public abstract class Pengguna {

    private final int idPengguna;
    private final String namaLengkap;
    private final String username;
    private final String password;
    private final String email;
    private final String noTelepon;
    private final Peran peran;
    private final StatusAktif statusAktif;

    protected Pengguna(int idPengguna, String namaLengkap, String username, String password,
                       String email, String noTelepon, Peran peran, StatusAktif statusAktif) {
        this.idPengguna = idPengguna;
        this.namaLengkap = namaLengkap;
        this.username = username;
        this.password = password;
        this.email = email;
        this.noTelepon = noTelepon;
        this.peran = peran;
        this.statusAktif = statusAktif;
    }

    public int getIdPengguna() { return idPengguna; }
    public String getNamaLengkap() { return namaLengkap; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getNoTelepon() { return noTelepon; }
    public Peran getPeran() { return peran; }
    public StatusAktif getStatusAktif() { return statusAktif; }

    public boolean isAktif() {
        return statusAktif == StatusAktif.AKTIF;
    }

    /** Data khusus peran untuk ditampilkan di tabel (hak akses / area / spesialisasi dan shift). */
    public abstract String getDataKhusus();
}
