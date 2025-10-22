package com.example.botonpanicovertice.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Mapea los tipos de alerta con el identificador esperado por la API.
 */
public final class AlertCategoryMapper {

    private static final Map<String, String> CATEGORY_MAP = new HashMap<>();

    static {
        CATEGORY_MAP.put("SEGURIDAD", "4");
        CATEGORY_MAP.put("SALUD", "1");
        CATEGORY_MAP.put("INCENDIO", "2");
        CATEGORY_MAP.put("ASISTENCIA", "5");
    }

    private AlertCategoryMapper() {
        // Utility class
    }

    public static String getCategoryFor(String alertType) {
        if (alertType == null) {
            return null;
        }
        return CATEGORY_MAP.get(alertType.toUpperCase(Locale.ROOT));
    }
}
