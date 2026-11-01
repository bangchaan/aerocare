package com.aerocare.model.enums;

/** ENUM pengguna.status_aktif. */
public enum StatusAktif implements DbEnum {
    AKTIF("Aktif"),
    TIDAK_AKTIF("Tidak Aktif");

    private final String label;

    StatusAktif(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static StatusAktif fromLabel(String label) {
        return DbEnum.fromLabel(StatusAktif.class, label);
    }
}
