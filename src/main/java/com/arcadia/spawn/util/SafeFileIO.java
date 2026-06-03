package com.arcadia.spawn.util;

import com.arcadia.spawn.ArcadiaSpawnMod;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Transaction-safe file IO: atomic write via .tmp + rename, rotated backups.
 * Used by LobbyManager, SpawnData export, CustomDimensionManager.
 */
public final class SafeFileIO {

    private static final int MAX_BACKUPS = 5;
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private SafeFileIO() {}

    /**
     * Writes content to target atomically:
     *   1. backup current target → backups/<name>.<stamp>.bak (if exists)
     *   2. write to target.tmp
     *   3. move target.tmp → target (atomic on same FS)
     *   4. rotate backups (keep last MAX_BACKUPS per filename)
     */
    public static void writeAtomicWithBackup(Path target, String content) throws IOException {
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Path backupDir = (parent != null ? parent : Path.of(".")).resolve("backups");
        if (Files.exists(target)) {
            try {
                Files.createDirectories(backupDir);
                String stamp = LocalDateTime.now().format(STAMP);
                Path backup = backupDir.resolve(target.getFileName().toString() + "." + stamp + ".bak");
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
                rotateBackups(backupDir, target.getFileName().toString());
            } catch (IOException e) {
                ArcadiaSpawnMod.LOGGER.warn("Could not backup {}: {}", target, e.getMessage());
            }
        }

        Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFail) {
            // Fallback for filesystems that don't support ATOMIC_MOVE (some Windows configs)
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void rotateBackups(Path backupDir, String baseName) {
        File[] backups = backupDir.toFile().listFiles((dir, name) ->
                name.startsWith(baseName + ".") && name.endsWith(".bak"));
        if (backups == null || backups.length <= MAX_BACKUPS) return;
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = MAX_BACKUPS; i < backups.length; i++) {
            if (!backups[i].delete()) {
                // Best-effort cleanup; surface persistent failures (permissions / file
                // lock) so a growing backups/ directory is diagnosable.
                ArcadiaSpawnMod.LOGGER.warn("Could not delete old backup: {}", backups[i]);
            }
        }
    }

    public static File findLatestBackup(Path original) {
        Path parent = original.getParent();
        if (parent == null) return null;
        Path backupDir = parent.resolve("backups");
        if (!Files.exists(backupDir)) return null;
        File[] backups = backupDir.toFile().listFiles((dir, name) ->
                name.startsWith(original.getFileName().toString() + ".") && name.endsWith(".bak"));
        if (backups == null || backups.length == 0) return null;
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed());
        return backups[0];
    }
}
