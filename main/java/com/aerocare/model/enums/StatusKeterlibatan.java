package com.aerocare.model.enums;

/** ENUM detail_penugasan.status_keterlibatan. */
public enum StatusKeterlibatan implements DbEnum {
    AKTIF("Aktif"),
    DIGANTIKAN("Digantikan"),
    SELESAI("Selesai");

    private final String label;

    StatusKeterlibatan(String label) {
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

    public static StatusKeterlibatan fromLabel(String label) {
        return DbEnum.fromLabel(StatusKeterlibatan.class, label);
    }
}
