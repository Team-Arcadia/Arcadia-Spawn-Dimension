package com.arcadia.spawn.tablist;

import com.arcadia.lib.ServerContext;
import com.arcadia.lib.data.DatabaseManager;
import com.arcadia.lib.data.TableDefinition;
import com.arcadia.spawn.ArcadiaSpawnMod;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cross-server presence backed by arcadia-lib's shared database.
 *
 * Schema (auto-created on server boot):
 *   arcadia_tablist_servers(
 *     server_id   VARCHAR(64)  PRIMARY KEY,
 *     display_name VARCHAR(64) NOT NULL DEFAULT '',
 *     online      INT          NOT NULL DEFAULT 0,
 *     max_players INT          NOT NULL DEFAULT 0,
 *     updated_at  BIGINT       NOT NULL DEFAULT 0
 *   )
 *
 * Each server UPSERTs its own row every heartbeat. Peers are read by selecting
 * all rows where updated_at is within peer_timeout_seconds of "now".
 *
 * All DB access goes through DatabaseManager.executeAsync / supplyAsync so the
 * main server thread never blocks on JDBC.
 */
public final class CrossServerDb implements TableDefinition {

    public static final CrossServerDb INSTANCE = new CrossServerDb();

    private static final String TABLE = "arcadia_tablist_servers";

    private CrossServerDb() {}

    @Override public String moduleId() { return "arcadia_spawn.tablist"; }

    @Override
    public List<String> createTableStatements() {
        return List.of(
                "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                        "  server_id    VARCHAR(64)  NOT NULL PRIMARY KEY," +
                        "  display_name VARCHAR(64)  NOT NULL DEFAULT ''," +
                        "  online       INT          NOT NULL DEFAULT 0," +
                        "  max_players  INT          NOT NULL DEFAULT 0," +
                        "  updated_at   BIGINT       NOT NULL DEFAULT 0" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    }

    public static boolean isAvailable() {
        try {
            return DatabaseManager.isDatabaseActive();
        } catch (Throwable t) {
            return false;
        }
    }

    public static String localServerId() {
        String id = ServerContext.SERVER_ID;
        return (id == null || id.isBlank()) ? "unknown" : id;
    }

    /** Async UPSERT of this server's row. Fail-soft. */
    public static void heartbeat(String displayName, int online, int max) {
        if (!isAvailable()) return;
        DatabaseManager.executeAsync(() -> {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO " + TABLE + " (server_id, display_name, online, max_players, updated_at) " +
                                 "VALUES (?, ?, ?, ?, ?) " +
                                 "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), " +
                                 "online = VALUES(online), max_players = VALUES(max_players), " +
                                 "updated_at = VALUES(updated_at)")) {
                ps.setString(1, localServerId());
                ps.setString(2, displayName == null ? "" : displayName);
                ps.setInt(3, online);
                ps.setInt(4, max);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (Exception e) {
                ArcadiaSpawnMod.LOGGER.debug("tablist heartbeat failed: {}", e.getMessage());
            }
        });
    }

    /** Async best-effort read of all peers. Returns immediately with an unmodifiable empty list when DB is down. */
    public static java.util.concurrent.CompletableFuture<List<PeerSnapshot>> fetchPeers(long timeoutMs) {
        if (!isAvailable()) {
            return java.util.concurrent.CompletableFuture.completedFuture(Collections.emptyList());
        }
        return DatabaseManager.supplyAsync(() -> {
            List<PeerSnapshot> out = new ArrayList<>();
            long cutoff = System.currentTimeMillis() - timeoutMs;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT server_id, display_name, online, max_players, updated_at " +
                                 "FROM " + TABLE)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        boolean alive = rs.getLong("updated_at") >= cutoff;
                        out.add(new PeerSnapshot(
                                rs.getString("server_id"),
                                rs.getString("display_name"),
                                rs.getInt("online"),
                                rs.getInt("max_players"),
                                rs.getLong("updated_at"),
                                alive
                        ));
                    }
                }
            } catch (Exception e) {
                ArcadiaSpawnMod.LOGGER.debug("tablist fetchPeers failed: {}", e.getMessage());
            }
            return out;
        });
    }

    /** Async cleanup of this server's row on shutdown. */
    public static void cleanup() {
        if (!isAvailable()) return;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM " + TABLE + " WHERE server_id = ?")) {
            ps.setString(1, localServerId());
            ps.executeUpdate();
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.debug("tablist cleanup failed: {}", e.getMessage());
        }
    }

    public record PeerSnapshot(String serverId, String displayName, int online, int max,
                               long updatedAt, boolean alive) {}
}
