# Arcadia Spawn

[Consult the full CurseForge description](./CURSEFORGE_PAGE.md)

Arcadia Spawn is a NeoForge Minecraft mod that turns the spawn experience into a complete, server-ready package. It ships a configurable custom dimension, a dynamic lobby teleport menu, a Random Teleport with safe-position search, a polished tab list with LuckPerms-aware rank sorting, cross-server player counts, slot bypass, and dimension creation on demand. Everything is server-authoritative, JSON-driven, anti-corruption hardened, and fully bilingual (EN + FR).

## Features

- **Custom spawn dimension** — `arcadia:spawn` dimension with configurable biome, flat layers, time lock, mob spawning rules, height, ambient light and ceiling. All in `config/arcadia/spawn/config.toml`.
- **Dynamic lobby menu** — `/lobby` opens a chest GUI listing every configured warp point. Each warp has its own item icon and bilingual description. Add or edit warps live via `/arcadia_spawn setlobbytp` / `edit` / `dellobbytp`.
- **Hub integration** — First card in the Arcadia Hub (row 0, sort 0). Click opens the lobby menu via a custom C2S packet. Rate-limited (5 opens / 10 s / player) to prevent GUI spam.
- **Random Teleport** — `/arcadiartp` finds a safe ground position in the Overworld within a configurable radius. Usage cap per player, warmup with movement cancel, cooldown — all configurable. Callable from any dimension.
- **Dimension-aware spawn** — `/arcadia_spawn setspawn` stores both the coordinates AND the dimension key. `/spawn` always teleports to the right dimension. First join + bedless respawn redirect to spawn when configured.
- **Custom tab list** — Themed header / footer with placeholders (`%server%`, `%online%`, `%tps%`, `%player_ping%`, `%player_playtime%`, `%lp_group%`, `%cross_total%`, `%peers%`), `&`-style color codes, and built-in animations (`%anim_scroll%`, `%anim_pulse%`, `%anim_rainbow%`, `%anim_loading%`, `%anim_blink%`, `%anim_title_color%`, `%anim_bar%`). Refresh interval and templates fully configurable.
- **Grade sorting (LuckPerms or OP fallback)** — Players are bucketed into Minecraft `PlayerTeam`s named `as_<999-weight>_<group>` so the highest LuckPerms weight always sits at the top of the tab. Without LuckPerms: falls back to vanilla op-level grades (Owner → Default). Listens to LuckPerms `UserDataRecalculateEvent` for instant promotion / demotion refresh.
- **Spectator hide** — Players in `/gamemode spectator` are hidden from the tab list of non-spectators. Other spectators still see each other so co-moderation works. Server-authoritative, opt-in via config (default on).
- **Hide ping icons** — Client-side mixin cancels the rendering of the latency signal-bar icons next to player names for a cleaner tab. Opt-in via config.
- **Cross-server player count** — When `cross_server_enabled = true` and the arcadia-lib database is reachable, every server heartbeats its row into a shared `arcadia_tablist_servers` table and reads the others. The `%peers%` footer line auto-expands into one row per server with ALIVE / STALE flags. Stale peers (no heartbeat for `peer_timeout_seconds`) are rendered as offline. All JDBC goes through `DatabaseManager.executeAsync` — the main server thread never blocks.
- **Custom dimensions on demand** — `/arcadia_spawn dimension create <id> [preset] [biome]`, `/arcadia_spawn dimension delete <id> [purge]`, `/arcadia_spawn dimension list`. Definitions are stored as JSON under `config/arcadia/spawn/dimensions/<id>.json` and registered at server startup under the `arcadia_custom:` namespace. Presets ship for `flat`, `void`, `lobby`.
- **Mod-removal manifest** — A `_manifest.json` is auto-written next to dimension files listing every custom dimension owned by the mod. Deleting with `purge=true` writes a `_purge_<id>.marker` so admins can audit world data after shutdown.
- **Slot bypass** — Permission-based slot bypass. Players with `arcadia_spawn.slots.bypass` (LuckPerms) bypass the slot limit when the server is full. Mixin-based fake max slots in the server list and optional join / leave message hiding.
- **Per-command permission nodes** — Each admin subcommand has its own NeoForge `PermissionNode` (LuckPerms-compatible), with an op-level fallback when no permission backend is installed. Hides unauthorized subcommands from tab-completion.
- **Strict input validation** — Lobby names and dimension ids are matched against a regex and a Windows-reserved-name blocklist (`CON`, `NUL`, `AUX`…) before any disk write. Descriptions are length-clamped and stripped of control characters. Prevents path-traversal and filename injection.
- **Transaction-safe persistence** — All JSON writes go through `SafeFileIO.writeAtomicWithBackup`: backup of the previous file → write to `.tmp` → atomic rename. Up to 5 rotated backups are kept per file under `<dir>/backups/`. If a file is unreadable at startup, the latest backup is restored automatically.
- **Anti-corruption defenses** — `SpawnData` validates `dimensionId` (falls back to `arcadia:spawn` on garbage), `DimensionType` clamps `height` and `logicalHeight` to Minecraft hard limits (2032 max, multiple of 16), `LobbyManager` recovers from a corrupted JSON file by restoring from the latest backup.
- **FTB Essentials `/back` compatible** — Every teleport fires `EntityTeleportEvent.TeleportCommand` so other mods can record the pre-teleport position.
- **Warmup + cooldown** — `/spawn`, `/lobby`, `/arcadiartp` support per-action warmup ticks (movement cancels) and cooldowns. All values configurable. LuckPerms meta overrides (`arcadia_spawn.spawn_tp.warmup`, etc.) supported.
- **Debug suite** — 13 diagnostic subcommands under `/arcadia_spawn debug` covering status, spawn, dimension, lobbies, RTP data, config dump, slots, player info, full reload, visited tag reset, TPS, language.
- **Bilingual** — Every user-facing string flows through `LocalizationManager` (EN + FR). Detection per-player via `clientInformation().language()`.

## Commands

### Player

| Command | What it does |
|---|---|
| `/lobby` | Opens the lobby teleport menu (chest GUI). Rate-limited 5 opens / 10 s. |
| `/spawn` | Teleport to the configured spawn (warmup + cooldown apply). |
| `/arcadiartp` | Random teleport in the Overworld, safe-position search. Callable from any dimension. |

### Admin (`/arcadia_spawn`)

| Command | Default OP | What it does |
|---|---|---|
| `reload` | 2 | Reload all lobby JSON files (async). |
| `setlobbytp <name> [item] [desc]` | 2 | Create a warp point at the current position. Name validated. |
| `dellobbytp <name>` | 2 | Delete a warp point. Tab-completes existing warps. |
| `edit <name> description \| item \| location` | 2 | Modify an existing warp. |
| `tp <name>` | 2 | Teleport directly to a warp (admin). |
| `setspawn` | 2 | Set the spawn at the current position (records dimension). |
| `dimension create <id> [preset] [biome]` | 4 | Create a custom dimension. Restart required for it to load. |
| `dimension delete <id> [purge]` | 4 | Delete a custom dimension definition. `purge=true` writes a cleanup marker. |
| `dimension list` | 2 | List all custom dimensions with their load status. |
| `tablist reload` | 2 | Force refresh of header / footer for every online player. |
| `tablist status` | 2 | Show tab list state (enabled, LP detected, DB available, server id). |
| `tablist peers` | 2 | List all cross-server rows in the shared DB with ALIVE / STALE flag. |
| `debug ...` | 2 | 13 diagnostic subcommands (see Debug suite below). |

### Debug (`/arcadia_spawn debug`)

| Subcommand | What it does |
|---|---|
| `status` | Full mod status overview (spawn, slots, players, config). |
| `spawn` | Spawn point details (dimension, position, rotation). |
| `dimension` | Dimension config dump (biome, layers, skylight…). |
| `lobbies` | List all lobby locations with coordinates and items. |
| `rtp` / `rtp reset` | Player RTP data (usages, last position) / reset counter. |
| `config` | Dump all active config values. |
| `slots` | Slot bypass status. |
| `player` | Player info (UUID, language, position, tags). |
| `reload_all` | Force reload of lobbies + languages. |
| `reset_visited` | Remove the first-join tag (next login triggers spawn TP). |
| `tps` | Server TPS + tick timing + spawn-dim chunk + entity counts. |
| `lang [key]` | Language info / test a localization key. |
| `tablist` | Local tablist state diagnostics. |

### Aliases (backward compat)

| Command | Alias of |
|---|---|
| `/setlobbyspawn` | `/arcadia_spawn setspawn` |

## How a teleport flows

```
Player runs /spawn (or /lobby pick, or /arcadiartp)
   |
   v
Cooldown check (CooldownManager from arcadia-lib)
   |  fail → "Teleport on cooldown (Xs)" error, no further work
   v
Fire EntityTeleportEvent.TeleportCommand
   |  cancel → silently abort (FTB /back records original pos)
   v
Apply LuckPerms meta overrides for warmup + cooldown
   |
   v
warmup == 0 ?  yes → instant teleportTo + Enderman sound
   |
   no
   v
Register WarmupTask in TeleportHelper.activeWarmups
   "Teleporting in 3s... Don't move!"
   |
   v
Server tick: TeleportHelper.tick (short-circuits when empty)
   |
   distance from start > 0.3 → "Teleport cancelled — you moved!"
   |
   warmup ticks elapsed → executeTP → set cooldown
```

## Creating a custom dimension

```bash
/arcadia_spawn dimension create myworld lobby
# → config/arcadia/spawn/dimensions/myworld.json created
# → restart the server for arcadia_custom:myworld to be registered

# Later
/arcadia_spawn dimension list
# • arcadia_custom:myworld [LOADED]

/arcadia_spawn dimension delete myworld true
# Definition removed. _purge_myworld.marker written — manually delete
# <world>/dimensions/arcadia_custom/myworld after shutdown.
```

Presets shipped: `flat` (default — bedrock + dirt + grass), `void` (a single air layer in `the_void`), `lobby` (bedrock + stone + smooth quartz floor, time-locked at noon, sky enabled).

After creation, edit the generated JSON to tune any of: `biome`, `layers`, `hasSkylight`, `hasCeiling`, `ultrawarm`, `natural`, `coordinateScale`, `bedWorks`, `respawnAnchorWorks`, `hasRaids`, `piglinSafe`, `minY`, `height`, `logicalHeight`, `infiniburn`, `effects`, `ambientLight`, `monsterSpawnLightLevel`, `monsterSpawnBlockLightLimit`, `timeLocked`, `fixedTime`, `generateFeatures`.

## Tab list quick reference

Configure in `config/arcadia/spawn/tablist.toml`. Enable with `tablist.enabled = true`. Edit `header_lines` and `footer_lines` — each entry is one tab line.

### Placeholders

| Placeholder | Resolves to |
|---|---|
| `%server%` | `server_display_name` (or `ServerContext.SERVER_ID` from arcadia-lib if empty) |
| `%online%`, `%max%` | Local player count / max slots |
| `%tps%`, `%mspt%` | Server TPS / average tick ms |
| `%uptime%` | Server uptime (e.g. `3h 14m`) |
| `%player_name%`, `%player_ping%`, `%player_playtime%` | Per-player info |
| `%lp_group%`, `%lp_prefix%` | LuckPerms primary group display name / prefix |
| `%cross_total%` | Sum of online across all alive peers including self |
| `%peers%` | Special — expands to one footer line per cross-server peer |
| `%anim_<name>%` | Cyclic animation frame (`scroll`, `pulse`, `loading`, `rainbow`, `title_color`, `bar`, `blink`) |

### Group sorting

- **With LuckPerms** — primary group weight (higher = higher in tab). LP prefix rendered before player name. Optional `color` meta recolors the player name with the nearest `ChatFormatting`.
- **Without LuckPerms** — op level fallback: op 4 = Owner (1000), op 3 = Admin (900), op 2 = Mod (800), op 1 = Helper (700), default = 0.

## Cross-server player counts

Cross-server uses the shared arcadia-lib database — no separate DB connection. Set `cross_server_enabled = true` on every server, give each one a unique `SERVER_ID` via arcadia-lib `ServerContext`, then point them all at the same database credentials. The mod creates an `arcadia_tablist_servers` table automatically on first boot.

Every server runs an UPSERT heartbeat into its own row every `heartbeat_interval_seconds` (default 10 s) and reads all rows on each tab refresh. Stale peers (no heartbeat for `peer_timeout_seconds`, default 45 s) are rendered as offline in the `%peers%` line. All JDBC runs through `DatabaseManager.executeAsync` / `supplyAsync` — the main server thread never blocks.

## Requirements

| Dependency | Version |
|------------|---------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219+ |
| Java | 21 |
| Arcadia Lib | bundled in the jar |
| LuckPerms | optional (soft integration) |

## Installation

1. Place `arcadia_spawn-1.5.3.jar` in your `mods/` folder. Arcadia Lib is bundled inside.
2. (Optional) Install [LuckPerms](https://luckperms.net/) for permission-based features and weight-driven grade sorting.
3. Start the server. On first launch, the mod creates `config/arcadia/spawn/config.toml`, `slot_bypass.toml`, `tablist.toml`, and the `lobbies/` folder.
4. Edit `config.toml` to tune the dimension. Edit `tablist.toml` and set `enabled = true` to activate the custom tab list. Run `/arcadia_spawn reload` to apply lobby changes live.

## Documentation

- [CHANGELOG.md](CHANGELOG.md) — Version history.
- [RULES.md](RULES.md) — Project conventions, architecture, AI assistant guidelines.
- [CURSEFORGE_PAGE.md](CURSEFORGE_PAGE.md) — Long-form CurseForge description.
- [AUDIT.md](AUDIT.md) — Performance / thread safety / tick-friendliness audit.

## Credits

Author: vyrriox
Organization: Team Arcadia
License: LGPL-3.0-or-later — see [LICENSE](LICENSE). Forks and derivative works are welcome under the same license, provided you credit "vyrriox / Team Arcadia" and link back to the upstream repository.
Discord: [discord.gg/xjF8Rtzyd4](https://discord.gg/xjF8Rtzyd4)
Website: [arcadia-echoes-of-power.fr](https://arcadia-echoes-of-power.fr/)

---

# Arcadia Spawn (Version Française)

[Consultez la description CurseForge complète](./CURSEFORGE_PAGE.md)

Arcadia Spawn est un mod Minecraft NeoForge qui transforme l'expérience de spawn en un package complet prêt pour la production. Il inclut une dimension personnalisée configurable, un menu lobby dynamique, un téléport aléatoire avec recherche de position sûre, une tab list personnalisée avec tri par grade LuckPerms, des compteurs de joueurs cross-server, un slot bypass et la création de dimensions à la demande. Tout est server-authoritative, piloté par JSON, durci contre la corruption, et entièrement bilingue (EN + FR).

## Caractéristiques

- **Dimension de spawn personnalisée** — Dimension `arcadia:spawn` avec biome, couches de blocs, time lock, règles de spawn de mobs, hauteur, ambient light et plafond configurables. Tout dans `config/arcadia/spawn/config.toml`.
- **Menu lobby dynamique** — `/lobby` ouvre un GUI listant chaque point de warp configuré. Chaque warp a sa propre icône et sa description bilingue. Ajout / édition live via `/arcadia_spawn setlobbytp` / `edit` / `dellobbytp`.
- **Intégration Hub** — Première carte du Hub Arcadia. Cliquer ouvre le menu lobby via un packet C2S dédié. Rate-limit (5 ouvertures / 10 s / joueur) pour éviter le spam GUI.
- **Téléport Aléatoire** — `/arcadiartp` trouve une position sol sûre dans l'Overworld dans un rayon configurable. Limite d'utilisations par joueur, warmup avec annulation au mouvement, cooldown. Invocable depuis toute dimension.
- **Spawn par dimension** — `/arcadia_spawn setspawn` stocke les coordonnées ET la clé de dimension. `/spawn` téléporte toujours dans la bonne dimension. Premier login + respawn sans lit redirigent vers le spawn quand configuré.
- **Tab list personnalisée** — Header / footer thématiques avec placeholders et animations intégrées. Refresh et templates entièrement configurables.
- **Tri par grade (LuckPerms ou OP fallback)** — Les joueurs sont placés dans des `PlayerTeam` Minecraft pour que le weight LuckPerms le plus haut apparaisse en haut de la tab. Sans LuckPerms : fallback op-level (Owner → Default).
- **Masquage spectator** — Les joueurs en `/gamemode spectator` sont masqués de la tab des non-spectateurs. Les autres spectateurs se voient mutuellement.
- **Masquage des icônes de ping** — Mixin client qui supprime l'affichage des barres de signal pour une tab plus propre.
- **Compteur cross-server** — Quand activé et que la DB arcadia-lib est joignable, chaque serveur heartbeat sa ligne dans une table partagée et lit les autres. La ligne `%peers%` du footer s'expand en une ligne par serveur.
- **Dimensions à la demande** — Création / suppression / listing de dimensions custom sous le namespace `arcadia_custom:`. Presets `flat`, `void`, `lobby`.
- **Slot bypass** — Bypass de slot basé permissions. Les joueurs avec `arcadia_spawn.slots.bypass` (LuckPerms) bypass le slot limit quand le serveur est plein.
- **Nodes de permission par commande** — Chaque sous-commande admin a son propre `PermissionNode` NeoForge, avec fallback op-level sans backend.
- **Validation stricte des entrées** — Noms de lobby et identifiants de dimension vérifiés via regex + blocklist Windows (`CON`, `NUL`…) avant écriture disque.
- **Persistance transactionnelle** — Toutes les écritures JSON passent par `SafeFileIO.writeAtomicWithBackup` : backup + écriture `.tmp` + rename atomique. Jusqu'à 5 backups rotatifs par fichier.
- **Anti-corruption** — `SpawnData` valide `dimensionId`, `DimensionType` clamp `height` aux limites Minecraft, `LobbyManager` se restaure automatiquement depuis le dernier backup en cas de fichier illisible.
- **Compatible FTB Essentials `/back`** — Chaque téléport fire `EntityTeleportEvent.TeleportCommand`.
- **Warmup + cooldown** — `/spawn`, `/lobby`, `/arcadiartp` supportent warmup (annulation au mouvement) et cooldown configurables.
- **Suite debug** — 13 sous-commandes de diagnostic sous `/arcadia_spawn debug`.
- **Bilingue** — Toutes les chaînes utilisateur passent par `LocalizationManager` (EN + FR). Détection par joueur via `clientInformation().language()`.

## Prérequis

| Dépendance | Version |
|------------|---------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219+ |
| Java | 21 |
| Arcadia Lib | inclus dans le jar |
| LuckPerms | optionnel (intégration souple) |

## Installation

1. Placez `arcadia_spawn-1.5.3.jar` dans votre dossier `mods/`. Arcadia Lib est inclus.
2. (Optionnel) Installez [LuckPerms](https://luckperms.net/) pour les features basées permissions et le tri par weight.
3. Démarrez le serveur. Au premier lancement, le mod crée `config/arcadia/spawn/config.toml`, `slot_bypass.toml`, `tablist.toml` et le dossier `lobbies/`.
4. Éditez `config.toml` pour régler la dimension. Éditez `tablist.toml` et passez `enabled = true` pour activer la tab list. Lancez `/arcadia_spawn reload` pour appliquer les changements de lobby à chaud.

## Crédits

Auteur : vyrriox
Organisation : Team Arcadia
Licence : LGPL-3.0-or-later — voir [LICENSE](LICENSE). Les forks et travaux dérivés sont les bienvenus sous la même licence, à condition de créditer "vyrriox / Team Arcadia" et de pointer vers le dépôt amont.
Discord : [discord.gg/xjF8Rtzyd4](https://discord.gg/xjF8Rtzyd4)
Site web : [arcadia-echoes-of-power.fr](https://arcadia-echoes-of-power.fr/)
