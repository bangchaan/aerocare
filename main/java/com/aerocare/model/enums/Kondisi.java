package com.aerocare.model.enums;

/** ENUM fasilitas.kondisi. */
public enum Kondisi implements DbEnum {
    BAIK("Baik"),
    RUSAK("Rusak"),
    PERLU_PERBAIKAN("Perlu Perbaikan");

    private final String label;

    Kondisi(String label) {
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

    public static Kondisi fromLabel(String label) {
        return DbEnum.fromLabel(Kondisi.class, label);
    }
}
