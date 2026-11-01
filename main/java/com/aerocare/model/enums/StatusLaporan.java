package com.aerocare.model.enums;

/** ENUM laporan_kerusakan.status. */
public enum StatusLaporan implements DbEnum {
    MENUNGGU("Menunggu"),
    DIPROSES("Diproses"),
    SELESAI("Selesai");

    private final String label;

    StatusLaporan(String label) {
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

    public static StatusLaporan fromLabel(String label) {
        return DbEnum.fromLabel(StatusLaporan.class, label);
    }
}
