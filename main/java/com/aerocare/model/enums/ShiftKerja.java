package com.aerocare.model.enums;

/** ENUM petugas.shift_kerja. */
public enum ShiftKerja implements DbEnum {
    PAGI("Pagi"),
    MALAM("Malam");

    private final String label;

    ShiftKerja(String label) {
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

    public static ShiftKerja fromLabel(String label) {
        return DbEnum.fromLabel(ShiftKerja.class, label);
    }
}
