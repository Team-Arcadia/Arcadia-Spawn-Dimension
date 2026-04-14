# Project Rules & AI/IDE Instructions

## 1. Project Identity

| Field | Value |
|-------|-------|
| Project Name | Arcadia Spawn |
| Mod ID | `arcadia_spawn` |
| Package | `com.arcadia.spawn` |
| Tech Stack | Java 21, NeoForge 1.21.1, Mixin, arcadia-lib |
| Author | vyrriox |
| License | All Rights Reserved |
| Dependencies | NeoForge 21.1.219+, arcadia-lib 1.2.0+ |

## 2. Git Workflow

- **main** — stable releases only
- **feat/<name>** — new features
- **fix/<name>** — bug fixes
- **hotfix/<name>** — urgent patches
- Commit format: `type: descriptive message` (feat, fix, refactor, docs, chore)
- Push immediately after each change

## 3. Code Conventions

- **Language**: All code, variables, logs in English
- **Communication**: French with user
- **Package**: `com.arcadia.spawn` (NOT `com.vyrriox`)
- **Naming**: PascalCase classes, camelCase methods/fields, UPPER_SNAKE constants
- **Comments**: English, minimal
- **Config paths**: `config/arcadia/spawn/`
- **Lobby data**: `config/arcadia/spawn/lobbies/`
- **Command prefix**: `/arcadia_spawn` for admin, `/lobby` `/spawn` `/arcadiartp` for players
- **Messages**: Use `ArcadiaMessages` from arcadia-lib for all player messages
- **Teleportation**: Use `TeleportHelper` (fires EntityTeleportEvent for FTB /back compat)
- **Localization**: Both EN and FR required, use `LocalizationManager`
- **DO NOT**: Use `player.teleportTo()` directly — always go through TeleportHelper

## 4. Project Structure

```
src/main/java/com/arcadia/spawn/
├── ArcadiaSpawnMod.java         — Main mod class, config registration, hub card
├── commands/
│   ├── SpawnCommands.java       — All admin & player commands
│   ├── RTPCommand.java          — Random teleport logic
│   ├── DebugCommands.java       — 13 debug subcommands
│   └── TeleportHelper.java      — TP wrapper (fires NeoForge events)
├── config/
│   ├── SpawnConfig.java         — Dimension + gameplay config
│   └── SlotBypassConfig.java    — Slot bypass config
├── data/
│   └── RTPData.java             — Player RTP attachment data
├── events/
│   ├── ModEvents.java           — Command registration, first join, respawn
│   ├── MobSpawnHandler.java     — Mob category filtering in spawn dim
│   └── SlotBypassHandler.java   — Permission-based slot bypass
├── lobby/
│   ├── LobbyLocation.java       — Warp point record
│   ├── LobbyManager.java        — JSON persistence for lobbies
│   ├── LobbyMenu.java           — Chest GUI menu
│   └── LocalizationManager.java — EN/FR translation loader
├── mixin/
│   ├── MixinMinecraftServer.java — Fake max slots in server list
│   └── MixinPlayerList.java      — Hide join/leave messages
├── registry/
│   └── AttachmentRegistry.java   — RTP data attachment
└── world/
    ├── DimensionRegistry.java    — Spawn dimension registration
    └── SpawnData.java            — Spawn point persistence (with dimension)
```

## 5. Adding a New Feature (Step by Step)

1. Create feature branch: `git checkout -b feat/<name>`
2. Add config options to `SpawnConfig.java` if needed
3. Add localization keys to both `en_us.json` and `fr_fr.json`
4. Implement logic using existing patterns (events, commands)
5. Use `TeleportHelper` for any teleportation, `ArcadiaMessages` for messages
6. Add debug command if applicable
7. Update CHANGELOG.md (EN + FR)
8. Test on both client and server
9. Commit and push

## 6. Testing Checklist

- [ ] `/lobby` opens GUI with all configured warps
- [ ] `/spawn` teleports to configured spawn (correct dimension)
- [ ] `/arcadiartp` finds safe position and teleports
- [ ] `/arcadia_spawn debug status` shows correct info
- [ ] First join teleports new player to spawn
- [ ] Respawn without bed sends to spawn (if configured)
- [ ] FTB Essentials `/back` works after teleport
- [ ] Slot bypass kicks non-VIP when full
- [ ] French client sees French messages
- [ ] English client sees English messages

## 7. Environment Setup

```bash
git clone <repo>
# Place arcadia-lib-1.2.0.jar in libs/
./gradlew build
```

## 8. AI Assistant Instructions

1. Package is `com.arcadia.spawn` — never use `com.vyrriox`
2. Never modify version numbers unless explicitly asked
3. Always use `TeleportHelper` for teleportation (FTB compat)
4. Always use `ArcadiaMessages` for player-facing messages
5. All new features must be bilingual (EN + FR)
6. Config paths under `config/arcadia/spawn/`
7. Admin commands under `/arcadia_spawn`, player commands standalone
8. Push to git after every change
9. Update CHANGELOG.md for every feature/fix
