package com.example.bankapp.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DotenvPropertyLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsAdminCredentialsFromLocalEnvFile() throws Exception {
        Files.writeString(tempDir.resolve(".env"),
                "BANK_ADMIN_USERNAME=local-admin\nBANK_ADMIN_PASSWORD=local-secret\n");

        Dotenv dotenv = Dotenv.configure()
                .directory(tempDir.toString())
                .ignoreIfMissing()
                .load();

        Map<String, Object> defaults = DotenvPropertyLoader.loadAdminDefaults(dotenv);

        assertEquals("local-admin", defaults.get("BANK_ADMIN_USERNAME"));
        assertEquals("local-secret", defaults.get("BANK_ADMIN_PASSWORD"));
    }

    @Test
    void ignoresMissingOrBlankCredentials() {
        Dotenv dotenv = Dotenv.configure()
                .directory(tempDir.toString())
                .ignoreIfMissing()
                .load();

        assertFalse(DotenvPropertyLoader.loadAdminDefaults(dotenv).containsKey("BANK_ADMIN_USERNAME"));
        assertFalse(DotenvPropertyLoader.loadAdminDefaults(dotenv).containsKey("BANK_ADMIN_PASSWORD"));
    }
}
