package com.arcadia.spawn.lobby;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalizationManager {
    // ConcurrentHashMap: written on init() and on /spawn reload (force-reload-all), read from
    // command/teleport message paths. A reload putting a language while a reader does
    // getOrDefault on a plain HashMap could observe a torn internal table. The per-language
    // inner maps are published once via put() and never mutated, so they stay plain HashMaps.
    private static final Map<String, Map<String, String>> TRANSLATIONS = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().create();

    public static void init() {
        loadLanguage("en_us");
        loadLanguage("fr_fr");
    }

    private static void loadLanguage(String langCode) {
        String path = "/assets/arcadia_spawn/lang/" + langCode + ".json";
        try (InputStream stream = LocalizationManager.class.getResourceAsStream(path)) {
            if (stream == null) {
                ArcadiaSpawnMod.LOGGER.error("Language file not found: {}", path);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Map<String, String> map = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                if (map == null) {
                    // Empty or malformed file → Gson returns null. ConcurrentHashMap rejects
                    // null values, and a null lang map would break getOrDefault fallbacks.
                    ArcadiaSpawnMod.LOGGER.error("Language file {} parsed to null (empty/malformed) — skipping.", path);
                    return;
                }
                TRANSLATIONS.put(langCode, map);
                ArcadiaSpawnMod.LOGGER.info("Loaded language: {}", langCode);
            }
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to load language: {}", path, e);
        }
    }

    public static String getPlayerLang(ServerPlayer player) {
        if (player == null) return "en_us";
        try {
            return player.clientInformation().language().toLowerCase();
        } catch (Exception e) {
            return "en_us";
        }
    }

    public static MutableComponent getComponent(ServerPlayer player, String key, Object... args) {
        String lang = getPlayerLang(player);
        Map<String, String> langMap = TRANSLATIONS.getOrDefault(lang, TRANSLATIONS.get("en_us"));
        if (langMap == null) langMap = TRANSLATIONS.get("en_us");
        if (langMap == null) return Component.literal(key);

        String template = langMap.getOrDefault(key, key);
        try {
            return Component.literal(String.format(template, args));
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.warn("Translation format failed for key '{}' (template '{}', args {}): {}",
                    key, template, java.util.Arrays.toString(args), e.getMessage());
            return Component.literal(template);
        }
    }

    public static String getString(ServerPlayer player, String key, Object... args) {
        String lang = getPlayerLang(player);
        Map<String, String> langMap = TRANSLATIONS.getOrDefault(lang, TRANSLATIONS.get("en_us"));
        if (langMap == null) langMap = TRANSLATIONS.get("en_us");
        if (langMap == null) return key;

        String template = langMap.getOrDefault(key, key);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.warn("Translation format failed for key '{}' (template '{}', args {}): {}",
                    key, template, java.util.Arrays.toString(args), e.getMessage());
            return template;
        }
    }
}
