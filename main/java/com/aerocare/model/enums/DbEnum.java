package com.aerocare.model.enums;

/** Enum Java yang nilainya identik dengan label ENUM di database MySQL. */
public interface DbEnum {

    String getLabel();

    static <E extends Enum<E> & DbEnum> E fromLabel(Class<E> type, String label) {
        if (label == null) {
            return null;
        }
        for (E e : type.getEnumConstants()) {
            if (e.getLabel().equals(label)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Nilai tidak dikenal untuk " + type.getSimpleName() + ": " + label);
    }
}
