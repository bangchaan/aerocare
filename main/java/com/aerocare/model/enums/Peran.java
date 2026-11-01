package com.aerocare.model.enums;

/** ENUM pengguna.peran. */
public enum Peran implements DbEnum {
    ADMIN("Admin"),
    SUPERVISOR("Supervisor"),
    PETUGAS("Petugas");

    private final String label;

    Peran(String label) {
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

    public static Peran fromLabel(String label) {
        return DbEnum.fromLabel(Peran.class, label);
    }
}
