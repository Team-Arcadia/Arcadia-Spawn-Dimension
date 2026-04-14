# Project Rules & AI/IDE Instructions

## 1. Project Identity

| Field | Value |
|-------|-------|
| Project Name | Arcadia Spawn |
| Mod ID | `arcadia_spawn` |
| Package | `com.arcadia.spawn` |
| Tech Stack | Java 21, NeoForge 1.21.1, Mixin, arcadia-lib |
| Author | vyrriox |
| Organization | Team Arcadia |
| License | All Rights Reserved |
| Minecraft Version | 1.21.1 |
| NeoForge Version | 21.1.219 |
| Java Version | 21 |
| Group ID | `com.arcadia.spawn` |
| Archive Name | `arcadia_spawn` |
| Dependency | Arcadia Lib in `libs/arcadia-lib-*.jar` |
| Side | BOTH (client + server) |

## 2. Git Workflow

### Branch Strategy
```
main          Production-ready releases (protected — NEVER push directly)
  |
staging       Pre-release testing & QA
  |
develop       Active development, feature integration
  |
feat/*        Feature branches (from develop)
fix/*         Bug fix branches (from develop)
hotfix        Critical production fixes (from main)
```

| Branch | Purpose | Merges into |
|--------|---------|-------------|
| `main` | Stable releases, tagged versions | - |
| `staging` | Testing before production | `main` |
| `develop` | Feature integration, daily work | `staging` |
| `feat/*` | New features | `develop` |
| `fix/*` | Bug fixes | `develop` |
| `hotfix` | Critical production patches | `main` + `develop` |

### Commit Conventions
Follow [Conventional Commits](https://www.conventionalcommits.org/):
```
feat: add new feature
fix: resolve bug
refactor: restructure code
docs: update documentation
perf: improve performance
chore: maintenance tasks
```

### Release Process
1. Merge `develop` → `staging` for QA
2. After QA passes: merge `staging` → `main`
3. Tag release: `git tag v1.5.0`
4. GitHub Actions auto-builds and publishes release

## 3. Code Conventions

### Language Policy
- **Code**: English only (variables, classes, methods, logs, comments)
- **Communication**: French with user
- **UI Text**: Must use `LocalizationManager` with both EN and FR translations
- **Documentation**: English first, then French section below

### Naming Conventions
| Type | Convention | Example |
|------|-----------|---------|
| Classes | `PascalCase` | `TeleportHelper`, `SpawnCommands` |
| Methods/Fields | `camelCase` | `openLobbyForPlayer()`, `warmupTicks` |
| Constants | `UPPER_SNAKE_CASE` | `MOD_ID`, `SPAWN_LEVEL_KEY` |
| Config Keys | `snake_case` | `rtp_max_usage`, `spawn_tp_warmup_ticks` |
| Command Literals | `snake_case` | `arcadia_spawn`, `setlobbytp` |

### Architecture Rules
- Use `ArcadiaMessages` from Arcadia Lib for all player-facing chat messages
- Use `TeleportHelper` for ALL teleportation — fires `EntityTeleportEvent` for FTB /back compat
- Use `CooldownManager` from Arcadia Lib for cooldown tracking
- Use `LocalizationManager` for all translatable text (EN + FR)
- LuckPerms access MUST be isolated in separate inner classes (prevent `NoClassDefFoundError`)
- Config options go in `SpawnConfig.java` (COMMON) or `SlotBypassConfig.java` (SERVER)
- Config paths under `config/arcadia/spawn/`
- Lobby data stored in `config/arcadia/spawn/lobbies/`
- Thread-safe collections (`CopyOnWriteArrayList`, `ConcurrentHashMap`) for shared state
- Never block the server thread

### What NOT to do
- DO NOT use `player.teleportTo()` directly — always go through `TeleportHelper`
- DO NOT reference LuckPerms classes directly in method bodies — isolate in inner classes
- DO NOT modify Arcadia Lib without permission
- DO NOT hardcode strings — use `LocalizationManager`
- DO NOT register custom `MenuType` via `DeferredRegister` — use `MenuType.GENERIC_9x3`
- DO NOT add `Co-Authored-By: Claude` in commits — keep Claude refs in CLAUDE.md only

## 4. Project Structure

```
src/main/java/com/arcadia/spawn/
├── ArcadiaSpawnMod.java              — Entry point, config, hub card registration
├── commands/
│   ├── SpawnCommands.java            — Admin (/arcadia_spawn) + player (/lobby, /spawn) commands
│   ├── RTPCommand.java               — Random teleport (/arcadiartp) logic
│   ├── DebugCommands.java            — 13 debug subcommands
│   └── TeleportHelper.java           — Bilingual TP wrapper (events, warmup, cooldown, LuckPerms meta)
├── config/
│   ├── SpawnConfig.java              — Dimension + gameplay config (COMMON)
│   └── SlotBypassConfig.java         — Slot bypass config (SERVER)
├── data/
│   └── RTPData.java                  — Player RTP attachment data (Codec serialized)
├── events/
│   ├── ModEvents.java                — Commands, first join, respawn, tick, disconnect
│   ├── MobSpawnHandler.java          — Mob category filtering in spawn dimension
│   └── SlotBypassHandler.java        — Permission-based slot bypass (LuckPerms compatible)
├── lobby/
│   ├── LobbyLocation.java            — Warp point record
│   ├── LobbyManager.java             — JSON persistence for lobbies (CopyOnWriteArrayList)
│   ├── LobbyMenu.java                — Chest GUI menu (9x3) with glass pane borders
│   ├── LobbyTabHandler.java          — DashboardTabHandler for prestige integration
│   └── LocalizationManager.java      — EN/FR translation loader
├── mixin/
│   ├── MixinMinecraftServer.java      — Fake max slots in server list
│   └── MixinPlayerList.java           — Hide join/leave messages
├── network/
│   ├── C2SOpenLobby.java             — Client→Server packet for hub card click
│   └── SpawnNetworking.java          — Packet registration
├── registry/
│   └── AttachmentRegistry.java        — RTP data attachment (copyOnDeath)
└── world/
    ├── DimensionRegistry.java         — Spawn dimension + flat world generation
    └── SpawnData.java                 — Spawn point persistence (with dimension ID)
```

## 5. Adding a New Feature (Step by Step)

1. `git checkout develop && git pull`
2. `git checkout -b feat/my-feature`
3. Add config options to `SpawnConfig.java` if needed
4. Add localization keys to both `en_us.json` and `fr_fr.json`
5. Implement logic using existing patterns:
   - Commands → `SpawnCommands.java` or new file in `commands/`
   - Events → `events/` package
   - Teleportation → use `TeleportHelper` (never `player.teleportTo()`)
   - Messages → use `ArcadiaMessages` from arcadia-lib
6. Add debug command if applicable in `DebugCommands.java`
7. Update `CHANGELOG.md` (EN + FR sections)
8. Test: `./gradlew build` + in-game testing
9. Commit: `git commit -m "feat: description"`
10. Push: `git push origin feat/my-feature`
11. PR: `feat/my-feature` → `develop`

## 6. Testing Checklist

- [ ] `/lobby` opens GUI with all configured warps
- [ ] `/spawn` teleports to configured spawn (correct dimension)
- [ ] `/arcadiartp` finds safe position and teleports
- [ ] `/arcadia_spawn debug status` shows correct info
- [ ] First join teleports new player to spawn
- [ ] Respawn without bed sends to spawn (if configured)
- [ ] FTB Essentials `/back` works after all teleports
- [ ] Slot bypass kicks non-VIP when full
- [ ] French client sees French messages (warmup, cooldown, cancelled)
- [ ] English client sees English messages
- [ ] Hub card click opens lobby menu (not pets/cosmetics)
- [ ] Warmup cancels on movement
- [ ] Cooldown prevents spam
- [ ] LuckPerms meta overrides work (when LuckPerms is present)
- [ ] No crash without LuckPerms (singleplayer)
- [ ] `./gradlew build` passes with no errors

## 7. Environment Setup

```bash
# Clone
git clone https://github.com/Team-Arcadia/Arcadia-Spawn-Dimension.git
cd Arcadia-Spawn-Dimension

# Ensure arcadia-lib jar is in libs/
ls libs/arcadia-lib-*.jar

# Build
./gradlew build

# Run client (dev)
./gradlew runClient

# Run server (dev)
./gradlew runServer
```

### IDE Setup
- **IntelliJ**: Import as Gradle project, run `./gradlew genIntellijRuns`
- **VS Code**: Use `.vscode/launch.json` if present
- **Eclipse**: Run `./gradlew genEclipseRuns`

## 8. AI Assistant Instructions

1. Package is `com.arcadia.spawn` — NEVER use `com.vyrriox`
2. NEVER modify version numbers unless explicitly asked
3. ALWAYS use `TeleportHelper` for teleportation (fires EntityTeleportEvent for FTB /back)
4. ALWAYS use `ArcadiaMessages` for player-facing messages
5. All new features MUST be bilingual (EN + FR via LocalizationManager)
6. Config paths under `config/arcadia/spawn/`
7. Admin commands under `/arcadia_spawn`, player commands as standalone (`/lobby`, `/spawn`, `/arcadiartp`)
8. Push to git after every change (auto-push workflow)
9. Update `CHANGELOG.md` for every feature/fix (EN + FR)
10. LuckPerms class references MUST be isolated in separate inner classes
11. NEVER add `Co-Authored-By: Claude` or any Claude mention in commits
12. Test builds with `./gradlew build` before pushing
13. Use `CooldownManager` from arcadia-lib for cooldowns
14. Hub card: row=0, sortOrder=0, uses C2SOpenLobby packet (not tab system)
15. Mixin config file: `arcadia_spawn.mixins.json` (not `arcadiaspawn.mixins.json`)
