package com.aerocare.model.enums;

/** ENUM laporan_kerusakan.prioritas. */
public enum Prioritas implements DbEnum {
    RENDAH("Rendah"),
    SEDANG("Sedang"),
    TINGGI("Tinggi");

    private final String label;

    Prioritas(String label) {
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

    public static Prioritas fromLabel(String label) {
        return DbEnum.fromLabel(Prioritas.class, label);
    }
}
