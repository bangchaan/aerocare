package com.aerocare.model.enums;

/** ENUM petugas.spesialisasi. */
public enum Spesialisasi implements DbEnum {
    MEKANIKAL("Mekanikal"),
    ELEKTRIKAL("Elektrikal"),
    PLUMBING("Plumbing dan Perpipaan"),
    BANGUNAN("Pemeliharaan Bangunan"),
    HVAC("HVAC (Tata Udara)");

    private final String label;

    Spesialisasi(String label) {
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

    public static Spesialisasi fromLabel(String label) {
        return DbEnum.fromLabel(Spesialisasi.class, label);
    }
}
