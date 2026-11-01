package com.aerocare.model.enums;

/** ENUM progres_penanganan.status_progres. */
public enum StatusProgres implements DbEnum {
    PEMERIKSAAN("Pemeriksaan"),
    PERBAIKAN("Perbaikan"),
    PENGUJIAN("Pengujian"),
    SELESAI("Selesai");

    private final String label;

    StatusProgres(String label) {
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

    public static StatusProgres fromLabel(String label) {
        return DbEnum.fromLabel(StatusProgres.class, label);
    }
}
