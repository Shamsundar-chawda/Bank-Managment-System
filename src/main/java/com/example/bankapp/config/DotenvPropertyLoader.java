package com.example.bankapp.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.HashMap;
import java.util.Map;

public final class DotenvPropertyLoader {

    private static final String[] ADMIN_KEYS = {"BANK_ADMIN_USERNAME", "BANK_ADMIN_PASSWORD"};

    private DotenvPropertyLoader() {
    }

    public static Map<String, Object> loadAdminDefaults(Dotenv dotenv) {
        Map<String, Object> defaults = new HashMap<>();
        for (String key : ADMIN_KEYS) {
            String value = dotenv.get(key);
            if (value != null && !value.isBlank()) {
                defaults.put(key, value);
            }
        }
        return defaults;
    }
}
