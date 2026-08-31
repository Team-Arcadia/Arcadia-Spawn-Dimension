package com.arcadia.spawn.world;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

/**
 * Turns the definitions held by {@link CustomDimensionManager} into a real server data
 * pack, and hands that pack to the server on startup.
 *
 * Why a data pack: {@code dimension_type} and {@code dimension} (level stem) are data
 * pack registries. They are rebuilt from packs on every world load, and NeoForge only
 * fires {@code RegisterEvent} for the static registries of {@code BuiltInRegistries} —
 * so registering a level stem there does nothing at all. Emitting JSON and injecting it
 * through {@link AddPackFindersEvent} is the supported path, and it is exactly how the
 * built-in {@code arcadia:spawn} dimension already ships.
 *
 * The pack directory is regenerated from scratch whenever it is written, so a deleted
 * definition can never leave a stale dimension behind.
 */
@EventBusSubscriber(modid = ArcadiaSpawnMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CustomDimensionPack {

    private static final String PACK_ID = "arcadia_spawn_custom_dimensions";

    private static final Path PACK_ROOT =
            FMLPaths.CONFIGDIR.get().resolve("arcadia/spawn/generated/custom_dimensions");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private CustomDimensionPack() {}

    public static String packRoot() {
        return PACK_ROOT.toString();
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;

        CustomDimensionManager.loadAll();
        if (!regenerate()) return;

        event.addRepositorySource(consumer -> {
            PackLocationInfo location = new PackLocationInfo(
                    PACK_ID,
                    Component.literal("Arcadia custom dimensions"),
                    PackSource.BUILT_IN,
                    // No KnownPack: the contents change whenever an admin creates or
                    // deletes a dimension, so a client must never reuse a cached copy.
                    Optional.empty());

            Pack pack = Pack.readMetaAndCreate(
                    location,
                    BuiltInPackSource.fromName(info -> new PathPackResources(info, PACK_ROOT)),
                    PackType.SERVER_DATA,
                    // required=true: the pack is not listed in level.dat, so it has to be
                    // force-enabled or the dimensions would vanish on the next world load.
                    new PackSelectionConfig(true, Pack.Position.TOP, false));

            if (pack == null) {
                ArcadiaSpawnMod.LOGGER.error("Could not read the generated custom dimension pack at {}", PACK_ROOT);
                return;
            }
            consumer.accept(pack);
        });
    }

    /**
     * Rewrites the whole generated pack from the current definitions.
     * Returns false if nothing usable could be written.
     */
    static synchronized boolean regenerate() {
        try {
            Path dataRoot = PACK_ROOT.resolve("data").resolve(CustomDimensionManager.CUSTOM_NAMESPACE);
            Path stemDir = dataRoot.resolve("dimension");
            Path typeDir = dataRoot.resolve("dimension_type");

            // Full wipe before rewrite: this is what makes a deleted definition actually
            // disappear instead of lingering as an orphaned JSON the game keeps loading.
            deleteRecursively(PACK_ROOT.resolve("data"));
            Files.createDirectories(stemDir);
            Files.createDirectories(typeDir);
            writePackMeta();

            int count = 0;
            for (CustomDimensionDef def : CustomDimensionManager.list()) {
                JsonObject type = CustomDimensionManager.toDimensionTypeJson(def);
                JsonObject stem = CustomDimensionManager.toLevelStemJson(def);
                Files.writeString(typeDir.resolve(def.id + ".json"), GSON.toJson(type), StandardCharsets.UTF_8);
                Files.writeString(stemDir.resolve(def.id + ".json"), GSON.toJson(stem), StandardCharsets.UTF_8);
                count++;
            }

            ArcadiaSpawnMod.LOGGER.info("Generated custom dimension data pack with {} dimension(s).", count);
            return true;
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to generate the custom dimension data pack", e);
            return false;
        }
    }

    private static void writePackMeta() throws IOException {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
        pack.addProperty("description", "Custom dimensions generated by Arcadia Spawn. Do not edit by hand.");

        JsonObject root = new JsonObject();
        root.add("pack", pack);

        Files.createDirectories(PACK_ROOT);
        Files.writeString(PACK_ROOT.resolve("pack.mcmeta"), GSON.toJson(root), StandardCharsets.UTF_8);
    }

    // ── World data purge ───────────────────────────────────────────────────

    /**
     * Deletes the save data of every dimension whose purge is still pending.
     *
     * Called at two points where no {@code ServerLevel} can hold the files open: just
     * before the levels are created (which also recovers a purge the server crashed on)
     * and right after they are all closed.
     */
    public static void runPendingPurges(MinecraftServer server) {
        var pending = CustomDimensionManager.pendingPurges();
        if (pending.isEmpty()) return;

        Path dimensionsRoot = server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions")
                .resolve(CustomDimensionManager.CUSTOM_NAMESPACE)
                .normalize();

        for (String id : pending) {
            Path target = dimensionsRoot.resolve(id).normalize();
            if (!target.startsWith(dimensionsRoot)) {
                ArcadiaSpawnMod.LOGGER.error("Refusing to purge {}: resolved outside the dimensions directory.", id);
                continue;
            }
            try {
                if (Files.exists(target)) {
                    deleteRecursively(target);
                    ArcadiaSpawnMod.LOGGER.info("Purged world data of custom dimension {}.", id);
                }
                CustomDimensionManager.cancelPurge(id);
            } catch (IOException e) {
                // Keep the marker so the next lifecycle point retries instead of leaving
                // orphaned region files behind forever.
                ArcadiaSpawnMod.LOGGER.error("Could not purge world data of custom dimension {}, will retry.", id, e);
            }
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
