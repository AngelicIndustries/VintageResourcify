package industries.angelic.vintageresourcify.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

import industries.angelic.vintageresourcify.VintageResourcify;

public class ReleaseMigrationTest {

    @Test
    public void rebrandKeepsTheModAndConfigurationIdentity() {
        assertEquals("vintage-resourcify", VintageResourcify.MODID);
        assertEquals("industries.angelic.vintageresourcify", VintageResourcify.MODGROUP);
        assertEquals(
            "vintageresourcify",
            Config.getConfigDirectory()
                .getName());
    }

    @Test
    public void old67MinecraftEndpointResolvesWithoutChangingCustomEndpoints() {
        ConfiguredPlatforms platforms = ConfiguredPlatforms.INSTANCE;
        assertEquals(
            "https://67minecraft.com/api/v2",
            platforms.normalizedApiUrl(" https://67.fentanylsolutions.org/api/v2/ "));
        assertEquals("https://67minecraft.com", platforms.browserBaseUrl("https://67.fentanylsolutions.org/api/v2"));
        assertEquals("https://custom.example/api/v2", platforms.normalizedApiUrl("https://custom.example/api/v2/"));
        assertEquals("https://custom.example", platforms.browserBaseUrl("https://api.custom.example/v2"));
    }

    @Test
    public void configReloadPreservesSettingsAndFormerFolderKeys() throws Exception {
        File file = new File(Config.getConfigDirectory(), "resourcify.json");
        byte[] previous = Files.readAllBytes(file.toPath());
        try {
            String fixture = "{\"enableSounds\":false,\"curseApiToken\":\"test-only-token\","
                + "\"additionalResourcePackFolders\":[\"/test/packs\"],"
                + "\"additionalShaderPackFolders\":[\"/test/shaders\"]}";
            Files.write(file.toPath(), fixture.getBytes(StandardCharsets.UTF_8));
            Config loaded = Config.reload();
            assertFalse(loaded.getEnableSounds());
            assertEquals("test-only-token", loaded.getCurseApiToken());
            String saved = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            assertTrue(saved.contains("additionalResourcePackFolders"));
            assertTrue(saved.contains("/test/packs"));
            assertTrue(saved.contains("additionalShaderPackFolders"));
            assertTrue(saved.contains("/test/shaders"));
        } finally {
            Files.write(file.toPath(), previous);
            Config.reload();
        }
    }
}
