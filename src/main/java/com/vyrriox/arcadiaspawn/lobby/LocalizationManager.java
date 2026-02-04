package com.vyrriox.arcadiaspawn.lobby;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.vyrriox.arcadiaspawn.ArcadiaSpawnMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LocalizationManager {
    private static final Map<String, Map<String, String>> TRANSLATIONS = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().create();

    public static void init() {
        loadLanguage("en_us");
        loadLanguage("fr_fr");
    }

    private static void loadLanguage(String langCode) {
        String path = "/assets/arcadia_spawn/lang/" + langCode + ".json";
        try (InputStream stream = LocalizationManager.class.getResourceAsStream(path)) {
            if (stream == null) {
                ArcadiaSpawnMod.LOGGER.error("Could not find language file: {}", path);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Map<String, String> map = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {
                }.getType());
                TRANSLATIONS.put(langCode, map);
                ArcadiaSpawnMod.LOGGER.info("Loaded language: {}", langCode);
            }
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to load language file: {}", path, e);
        }
    }

    public static net.minecraft.network.chat.MutableComponent getComponent(ServerPlayer player, String key,
            Object... args) {
        String lang = "en_us";
        if (player != null) {
            // Note: clientInformation().language() returns e.g. "fr_fr" or "en_us"
            // It might be "fr_FR", so we lowercase it.
            try {
                // For NeoForge 1.21, we assume clientInformation() exists or similar.
                // If direct access isn't available, we might need a mixin or event listening,
                // but typically ServerPlayer holds this data.
                // Let's rely on standard method names. If it fails build, we fix.
                lang = player.clientInformation().language().toLowerCase();
            } catch (Exception e) {
                // Fallback
            }
        }

        Map<String, String> langMap = TRANSLATIONS.getOrDefault(lang, TRANSLATIONS.get("en_us"));
        if (langMap == null)
            langMap = TRANSLATIONS.get("en_us"); // Absolute fallback

        String template = langMap.getOrDefault(key, key);
        try {
            return Component.literal(String.format(template, args));
        } catch (Exception e) {
            return Component.literal(template);
        }
    }
}
