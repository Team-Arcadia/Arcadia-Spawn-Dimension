# Arcadia Spawn — Technical Audit Report

**Date:** 2026-04-14  
**Version:** 1.4.3  
**Author:** vyrriox  

---

## 1. Performance Audit

### PASS — Config Value Access
- All `ModConfigSpec.ConfigValue.get()` calls are O(1) cached lookups.
- No config values read in hot loops or per-tick handlers.

### PASS — Data Structures
- `CopyOnWriteArrayList` for lobby locations: optimal for read-heavy, write-rare pattern.
- `ConcurrentHashMap` used in arcadia-lib for thread-safe registries.
- Lobby count typically < 20, so linear search is acceptable.

### IMPROVED — RTP Chunk Loading
- **Before:** Used `java.util.Random` (synchronized, thread-contention risk).
- **After:** Switched to `ThreadLocalRandom` — zero contention, better performance.
- **Note:** `level.getChunk()` forces synchronous chunk load. This is intentional for RTP safety (need ground check), but max attempts is now configurable (default 50) to limit worst-case tick impact.

### PASS — Mixin Performance
- Both mixins (`MixinMinecraftServer`, `MixinPlayerList`) use `@Inject` at HEAD/RETURN — no bytecode rewriting, minimal overhead.
- Guard clauses (`if (!enabled)`) exit early before any work.

### PASS — JSON Persistence
- File I/O only during `reload()` (admin command) and `addLocation()`/`removeLocation()` (admin commands).
- Never called during gameplay ticks.

---

## 2. Thread Safety Audit

### PASS — Lobby System
- `CopyOnWriteArrayList` is fully thread-safe for concurrent reads.
- Writes (add/remove) copy the array — safe but slightly expensive. Acceptable given write frequency (admin commands only).

### PASS — Localization
- `TRANSLATIONS` map populated once during `init()` (on main thread during FMLCommonSetupEvent).
- Read-only after init — inherently thread-safe.

### PASS — SpawnData
- `SavedData` is accessed via `level.getDataStorage()` — managed by Minecraft's level lock.
- `setDirty()` marks for save on next autosave — standard pattern.

### PASS — RTP Data
- Player attachments are per-player and accessed on main thread (command execution context).
- `Codec`-based serialization is thread-safe.

### PASS — Slot Bypass
- Config reads are atomic. `PermissionAPI.getPermission()` is designed for main-thread use.
- No shared mutable state.

---

## 3. Tick Friendliness Audit

### PASS — No Tick Handlers
- This mod registers **zero** tick event listeners.
- All logic is event-driven (command execution, player join/leave).
- TeleportManager warmup ticking is handled by arcadia-lib (already audited).

### PASS — Mob Spawn Handler
- `EntityJoinLevelEvent` handler has O(1) dimension check as first guard.
- Only processes entities in spawn dimension — zero cost for other dimensions.
- `MobCategory` switch is O(1).

### IMPROVED — Configurable RTP Attempts
- Added `rtp_max_attempts` config (default 50, range 10-200).
- Prevents unbounded chunk loading in degenerate cases.

### PASS — Event Priority
- `SlotBypassHandler` uses `HIGHEST` priority — checked before other handlers, exits early if disabled.

---

## 4. Functionality Audit

### NEW — arcadia-lib Integration
- Hub card registered at sortOrder=1 (first position).
- Uses `ArcadiaMessages` for consistent themed messaging.
- Uses `TeleportManager` for warmup/cooldown teleportation.
- Server actions registered for cross-mod lobby opening.

### NEW — Configurable Warmup & Cooldown
- `/spawn` — configurable warmup (default 3s) and cooldown (default 30s).
- `/lobby` teleport — configurable warmup (default 2s) and cooldown (default 15s).
- `/arcadiartp` — configurable warmup (default 3s) and cooldown (default 60s).
- All use arcadia-lib's TeleportManager with movement cancellation.

### NEW — Debug Commands Suite
- `/arcadia_spawn debug status` — full mod status overview.
- `/arcadia_spawn debug spawn` — spawn dimension details.
- `/arcadia_spawn debug dimension` — dimension config dump.
- `/arcadia_spawn debug lobbies` — list all lobby locations with details.
- `/arcadia_spawn debug rtp` — player RTP data inspection.
- `/arcadia_spawn debug rtp reset` — reset RTP usage counter.
- `/arcadia_spawn debug config` — dump all active config values.
- `/arcadia_spawn debug slots` — slot bypass status.
- `/arcadia_spawn debug player` — player info (UUID, lang, position, tags).
- `/arcadia_spawn debug reload_all` — force reload lobbies + languages.
- `/arcadia_spawn debug reset_visited` — remove first-join tag.
- `/arcadia_spawn debug tps` — server TPS and tick timing.
- `/arcadia_spawn debug lang [key]` — language info and key testing.

### NEW — Enhanced Configuration
- `force_spawn_on_first_join` — toggle first-join teleport.
- `force_spawn_on_respawn` — teleport to spawn on death (no bed/anchor).
- `rtp_cooldown_seconds` — cooldown between RTP uses.
- `rtp_warmup_ticks` — warmup before RTP teleport.
- `rtp_max_attempts` — configurable safe position search limit.
- `spawn_tp_warmup_ticks` / `spawn_tp_cooldown_seconds`.
- `lobby_tp_warmup_ticks` / `lobby_tp_cooldown_seconds`.

### NEW — Command Structure
- `/arcadia_spawn` — admin root (requires op 2).
  - `reload`, `setlobbytp`, `dellobbytp`, `edit`, `tp`, `setspawn`, `debug`.
- `/lobby` — opens GUI menu (no permission required).
- `/spawn` — teleport to spawn (no permission required).
- `/setlobbyspawn` — backward-compatible alias (requires op 2).
- `/arcadiartp` — random teleport (no permission required).

### IMPROVED — Bilingual Kick Message
- Slot bypass kick message now bilingual by default (EN | FR in same message).

### IMPROVED — Lobby Menu UX
- Glass pane borders for cleaner appearance.
- Bilingual lore ("Click to teleport | Cliquez pour vous tp").

### PRESERVED — Full Backward Compatibility
- `/lobby`, `/spawn`, `/setlobbyspawn`, `/arcadiartp` still work as before.
- Config path changed from `arcadia/arcadialobbyspawn/` to `arcadia/spawn/`.
- Lobby data path changed to `arcadia/spawn/lobbies/`.
- Dimension key unchanged: `arcadia:spawn`.

---

## Summary

| Category | Status | Notes |
|----------|--------|-------|
| Performance | ✅ PASS | No hot-path issues. ThreadLocalRandom for RTP. |
| Thread Safety | ✅ PASS | All mutable state properly guarded. |
| Tick Friendly | ✅ PASS | Zero tick handlers. Event-driven only. |
| Functionality | ✅ COMPLETE | Full arcadia-lib integration, debug suite, bilingual. |

---

# Rapport d'Audit — Arcadia Spawn

**Date :** 2026-04-14  
**Version :** 1.4.3  
**Auteur :** vyrriox  

## Résumé

| Catégorie | Statut | Notes |
|-----------|--------|-------|
| Performance | ✅ OK | Aucun problème de performance. ThreadLocalRandom pour RTP. |
| Thread Safety | ✅ OK | Tous les états mutables correctement protégés. |
| Tick Friendly | ✅ OK | Aucun handler de tick. Entièrement événementiel. |
| Fonctionnalité | ✅ COMPLET | Intégration arcadia-lib, suite debug, bilingue EN/FR. |
