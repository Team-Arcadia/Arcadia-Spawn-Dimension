<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-brightgreen?style=for-the-badge&logo=mojangstudios" alt="Minecraft 1.21.1"/>
  <img src="https://img.shields.io/badge/NeoForge-21.1+-orange?style=for-the-badge" alt="NeoForge"/>
  <img src="https://img.shields.io/badge/Java-21-red?style=for-the-badge&logo=openjdk" alt="Java 21"/>
  <img src="https://img.shields.io/github/v/release/Team-Arcadia/Arcadia-Spawn-Dimension?style=for-the-badge&label=Version&color=blue" alt="Version"/>
  <img src="https://img.shields.io/github/actions/workflow/status/Team-Arcadia/Arcadia-Spawn-Dimension/build.yml?style=for-the-badge&label=Build" alt="Build"/>
  <img src="https://img.shields.io/github/license/Team-Arcadia/Arcadia-Spawn-Dimension?style=for-the-badge" alt="License"/>
</p>

<h1 align="center">Arcadia Spawn</h1>

<p align="center">
  <b>Complete spawn dimension management, dynamic lobby, and RTP system for Minecraft</b><br/>
  <i>Powered by <a href="https://github.com/Team-Arcadia">Arcadia Lib</a> | Built for NeoForge 1.21.1</i>
</p>

<p align="center">
  <a href="#features">Features</a> |
  <a href="#commands">Commands</a> |
  <a href="#installation">Installation</a> |
  <a href="#configuration">Configuration</a> |
  <a href="#luckperms-meta-overrides">LuckPerms Meta</a> |
  <a href="#contributing">Contributing</a> |
  <a href="#version-fran%C3%A7aise">Francais</a>
</p>

---

## Overview

Arcadia Spawn is a feature-rich spawn dimension manager designed for the Arcadia ecosystem. It provides a fully configurable custom dimension, a dynamic lobby teleport menu integrated into the Arcadia Hub, random teleportation with warmup/cooldown, slot bypass, and comprehensive debug tools. All teleport messages are bilingual (EN/FR) and all teleports are compatible with FTB Essentials `/back`.

## Features

| Feature | Description |
|---|---|
| **Hub Integration** | First card in the Arcadia Hub (row 0). Opens lobby menu via custom C2S packet |
| **Dynamic Lobby Menu** | GUI with configurable warp points, custom item icons, descriptions, glass pane borders |
| **Spawn Dimension** | Fully configurable custom dimension (biome, layers, time, weather, mob spawning) |
| **Random Teleport (RTP)** | `/arcadiartp` with configurable radius, usage limits, safe position finder |
| **Bilingual Warmup** | All teleport messages in EN/FR with movement cancellation |
| **LuckPerms Meta** | Override warmup/cooldown per group via LuckPerms meta values |
| **Slot Bypass** | Permission-based server slot limiting with fake max display |
| **FTB /back** | All teleports fire `EntityTeleportEvent` for full FTB Essentials compatibility |
| **Debug Suite** | 13 diagnostic commands for complete server inspection |
| **Bilingual** | Automatic language detection (English/French) based on client settings |
| **Dimension-Aware Spawn** | Spawn point stores dimension ID — works across any dimension |
| **Respawn Control** | Configurable respawn to spawn point when player has no bed/anchor |
| **Optimized** | Zero tick handlers, ThreadLocalRandom, CopyOnWriteArrayList, event-driven only |

## Commands

### Player Commands
| Command | Permission | Description |
|---|---|---|
| `/lobby` | All | Opens the lobby teleport menu |
| `/spawn` | All | Teleport to the configured spawn point |
| `/arcadiartp` | All | Random teleport in Overworld |

### Admin Commands (`/arcadia_spawn`)
| Command | Permission | Description |
|---|---|---|
| `reload` | Op Level 2 | Reload lobby configuration from disk |
| `setlobbytp <name> [item] [desc]` | Op Level 2 | Create a warp point at current position |
| `dellobbytp <name>` | Op Level 2 | Delete a warp point |
| `edit <name> description\|item\|location` | Op Level 2 | Modify an existing warp point |
| `tp <name>` | Op Level 2 | Teleport directly to a warp point |
| `setspawn` | Op Level 2 | Set spawn at current position (stores dimension) |
| `/setlobbyspawn` | Op Level 2 | Alias for setspawn (backward compat) |

### Debug Commands (`/arcadia_spawn debug`)
| Command | Description |
|---|---|
| `status` | Full mod status overview (spawn, slots, players, config) |
| `spawn` | Spawn point details (dimension, position, rotation) |
| `dimension` | Dimension config dump (biome, layers, skylight, etc.) |
| `lobbies` | List all lobby locations with coordinates and items |
| `rtp` | Player RTP data (usages, last position, config) |
| `rtp reset` | Reset RTP usage counter for current player |
| `config` | Dump all active config values (RTP, TP, mobs, slots) |
| `slots` | Slot bypass status and configuration |
| `player` | Player info (UUID, language, dimension, position, tags) |
| `reload_all` | Force reload lobbies + language files |
| `reset_visited` | Remove first-join tag (re-triggers spawn TP) |
| `tps` | Server TPS, average/max tick times, chunk counts |
| `lang [key]` | Language info and translation key testing |

## Installation

### Requirements
- Minecraft **1.21.1**
- NeoForge **21.1+**
- [Arcadia Lib](https://github.com/Team-Arcadia) **>= 1.2.0**

### Steps
1. Download the latest release from the [Releases](https://github.com/Team-Arcadia/Arcadia-Spawn-Dimension/releases) page
2. Place `arcadia-lib-1.2.0.jar` in your `mods/` folder
3. Place `arcadia_spawn-1.5.0.jar` in your `mods/` folder
4. Start the server

### Client Installation
Installing on the client enables the Arcadia Hub card integration. The mod works without client installation (server-only commands still function).

## Configuration

### Config Files
| File | Type | Description |
|---|---|---|
| `config/arcadia/spawn/config.toml` | COMMON | Dimension properties, RTP, teleport warmup/cooldown, mob spawning |
| `config/arcadia/spawn/slot_bypass.toml` | SERVER | Slot bypass, fake max slots, join/leave messages |
| `config/arcadia/spawn/lobbies/*.json` | Data | Lobby warp point storage (one file per dimension) |

### Key Config Options
| Option | Default | Description |
|---|---|---|
| `rtp_radius` | 50000 | Max radius for random teleport |
| `rtp_max_usage` | 1 | Max RTP uses before reusing last position |
| `rtp_warmup_ticks` | 60 | Warmup before RTP (20 = 1s) |
| `rtp_cooldown_seconds` | 60 | Cooldown between RTP uses |
| `spawn_tp_warmup_ticks` | 60 | Warmup for /spawn |
| `lobby_tp_warmup_ticks` | 40 | Warmup for lobby teleport |
| `force_spawn_on_first_join` | true | Auto-teleport new players to spawn |
| `force_spawn_on_respawn` | false | Teleport to spawn on death (no bed/anchor) |

### Slot Bypass
- **Permission node**: `arcadia_spawn.slots.bypass`
- Assign via LuckPerms to allow VIP players to join when server is full
- `fake_max_slots_enabled`: Display configured max in server list
- `hide_join_leave_messages`: Hide vanilla join/leave chat messages

## LuckPerms Meta Overrides

Override warmup and cooldown values per group using LuckPerms meta:

```bash
# Remove warmup for VIP group
/lp group vip meta set arcadia_spawn.spawn_tp.warmup 0
/lp group vip meta set arcadia_spawn.lobby_tp.warmup 0
/lp group vip meta set arcadia_spawn.rtp.warmup 20

# Reduce cooldowns for VIP group
/lp group vip meta set arcadia_spawn.spawn_tp.cooldown 5000
/lp group vip meta set arcadia_spawn.rtp.cooldown 15000

# Remove all restrictions for staff
/lp group admin meta set arcadia_spawn.spawn_tp.warmup 0
/lp group admin meta set arcadia_spawn.spawn_tp.cooldown 0
/lp group admin meta set arcadia_spawn.rtp.warmup 0
/lp group admin meta set arcadia_spawn.rtp.cooldown 0
```

Values: **ticks** for warmup, **milliseconds** for cooldown. Falls back to config values if meta not set.

## Architecture

```
com.arcadia.spawn
  +-- ArcadiaSpawnMod.java              Entry point, config, hub card registration
  +-- commands/
  |   +-- SpawnCommands.java            Admin + player commands
  |   +-- RTPCommand.java              Random teleport logic
  |   +-- DebugCommands.java           13 diagnostic subcommands
  |   +-- TeleportHelper.java          Bilingual TP (events, warmup, cooldown, LuckPerms)
  +-- config/
  |   +-- SpawnConfig.java             Dimension + gameplay config (COMMON)
  |   +-- SlotBypassConfig.java        Slot bypass config (SERVER)
  +-- data/
  |   +-- RTPData.java                 Player RTP attachment (Codec serialized)
  +-- events/
  |   +-- ModEvents.java               Commands, first join, respawn, tick, disconnect
  |   +-- MobSpawnHandler.java         Mob category filtering in spawn dimension
  |   +-- SlotBypassHandler.java       Permission-based slot bypass
  +-- lobby/
  |   +-- LobbyLocation.java           Warp point record
  |   +-- LobbyManager.java            JSON persistence (CopyOnWriteArrayList)
  |   +-- LobbyMenu.java               Chest GUI menu (9x3)
  |   +-- LobbyTabHandler.java         DashboardTabHandler for prestige
  |   +-- LocalizationManager.java     EN/FR translation loader
  +-- mixin/
  |   +-- MixinMinecraftServer.java    Fake max slots in server list
  |   +-- MixinPlayerList.java         Hide join/leave messages
  +-- network/
  |   +-- C2SOpenLobby.java            Client->Server packet for hub card
  |   +-- SpawnNetworking.java         Packet registration
  +-- registry/
  |   +-- AttachmentRegistry.java      RTP data attachment (copyOnDeath)
  +-- world/
      +-- DimensionRegistry.java       Spawn dimension + flat world generation
      +-- SpawnData.java               Spawn point persistence (with dimension ID)
```

## Building from Source

```bash
git clone https://github.com/Team-Arcadia/Arcadia-Spawn-Dimension.git
cd Arcadia-Spawn-Dimension
./gradlew build
```

The compiled JAR will be in `build/libs/`.

## Contributing

We welcome contributions! Please read our [Contributing Guide](.github/CONTRIBUTING.md) before submitting a pull request.

## Links

- [Arcadia: Echoes of Power](https://arcadia-echoes-of-power.fr/)
- [Discord](https://discord.gg/xjF8Rtzyd4)
- [Donate](https://buy.stripe.com/3cI3co6X97Vy4IK50QfIs00)

## License

All Rights Reserved. See [LICENSE](LICENSE) for details.

## Credits

**Author:** vyrriox
**Organization:** [Team Arcadia](https://github.com/Team-Arcadia)

---

<h1 align="center">Arcadia Spawn (Version Francaise)</h1>

<p align="center">
  <b>Gestion complete de dimension spawn, lobby dynamique et systeme RTP pour Minecraft</b><br/>
  <i>Propulse par <a href="https://github.com/Team-Arcadia">Arcadia Lib</a> | Construit pour NeoForge 1.21.1</i>
</p>

## Apercu

Arcadia Spawn est un gestionnaire de dimension spawn complet concu pour l'ecosysteme Arcadia. Il fournit une dimension personnalisee entierement configurable, un menu lobby dynamique integre au Hub Arcadia, une teleportation aleatoire avec warmup/cooldown, un bypass de slots, et des outils de debug complets. Tous les messages sont bilingues (EN/FR) et compatibles avec FTB Essentials `/back`.

## Caracteristiques

| Fonctionnalite | Description |
|---|---|
| **Integration Hub** | Premiere carte dans le Hub Arcadia (row 0). Ouvre le menu lobby via packet C2S |
| **Menu Lobby Dynamique** | Interface avec points de warp configurables, icones, descriptions, bordures en verre |
| **Dimension Spawn** | Dimension personnalisee entierement configurable (biome, couches, temps, meteo, mobs) |
| **Teleportation Aleatoire (RTP)** | `/arcadiartp` avec rayon, limites d'utilisation, recherche de position sure |
| **Warmup Bilingue** | Tous les messages de teleportation en EN/FR avec annulation sur mouvement |
| **Meta LuckPerms** | Override warmup/cooldown par groupe via les meta LuckPerms |
| **Bypass de Slots** | Limitation de slots basee sur les permissions avec faux maximum affiche |
| **FTB /back** | Toutes les teleportations declenchent `EntityTeleportEvent` pour compatibilite FTB |
| **Suite Debug** | 13 commandes de diagnostic pour inspection complete du serveur |
| **Bilingue** | Detection automatique de la langue (Anglais/Francais) selon le client |
| **Spawn Multi-Dimension** | Le point de spawn stocke l'ID de dimension — fonctionne dans toute dimension |
| **Controle Respawn** | Respawn configurable au spawn quand le joueur n'a pas de lit/ancre |
| **Optimise** | Zero tick handlers, ThreadLocalRandom, CopyOnWriteArrayList, evenementiel uniquement |

## Commandes

### Commandes Joueur
| Commande | Permission | Description |
|---|---|---|
| `/lobby` | Tous | Ouvre le menu de teleportation lobby |
| `/spawn` | Tous | Teleportation vers le spawn configure |
| `/arcadiartp` | Tous | Teleportation aleatoire dans l'Overworld |

### Commandes Admin (`/arcadia_spawn`)
| Commande | Permission | Description |
|---|---|---|
| `reload` | Op Niveau 2 | Recharger la configuration lobby |
| `setlobbytp <nom> [item] [desc]` | Op Niveau 2 | Creer un point de warp |
| `dellobbytp <nom>` | Op Niveau 2 | Supprimer un point de warp |
| `edit <nom> description\|item\|location` | Op Niveau 2 | Modifier un warp existant |
| `tp <nom>` | Op Niveau 2 | Se teleporter a un warp |
| `setspawn` | Op Niveau 2 | Definir le spawn (stocke la dimension) |

### Commandes Debug (`/arcadia_spawn debug`)
| Commande | Description |
|---|---|
| `status` | Vue d'ensemble du mod |
| `spawn` | Details du point de spawn |
| `dimension` | Dump config dimension |
| `lobbies` | Lister tous les lobbies |
| `rtp` | Donnees RTP du joueur |
| `rtp reset` | Reinitialiser le compteur RTP |
| `config` | Dump de toute la config |
| `slots` | Statut bypass de slots |
| `player` | Info joueur (UUID, langue, position) |
| `reload_all` | Recharger lobbies + langues |
| `reset_visited` | Supprimer le tag premier join |
| `tps` | TPS serveur et timing des ticks |
| `lang [cle]` | Info langue / tester une cle |

## Installation

### Prerequis
- Minecraft **1.21.1**
- NeoForge **21.1+**
- [Arcadia Lib](https://github.com/Team-Arcadia) **>= 1.2.0**

### Etapes
1. Telecharger la derniere release depuis [Releases](https://github.com/Team-Arcadia/Arcadia-Spawn-Dimension/releases)
2. Placer `arcadia-lib-1.2.0.jar` dans le dossier `mods/`
3. Placer `arcadia_spawn-1.5.0.jar` dans le dossier `mods/`
4. Demarrer le serveur

## Meta LuckPerms

Override les valeurs de warmup et cooldown par groupe :

```bash
# Supprimer le warmup pour les VIP
/lp group vip meta set arcadia_spawn.spawn_tp.warmup 0
/lp group vip meta set arcadia_spawn.lobby_tp.warmup 0

# Reduire les cooldowns pour les VIP
/lp group vip meta set arcadia_spawn.rtp.cooldown 15000

# Supprimer toutes les restrictions pour le staff
/lp group admin meta set arcadia_spawn.spawn_tp.warmup 0
/lp group admin meta set arcadia_spawn.spawn_tp.cooldown 0
```

Valeurs : **ticks** pour warmup, **millisecondes** pour cooldown.

## Compiler depuis les Sources

```bash
git clone https://github.com/Team-Arcadia/Arcadia-Spawn-Dimension.git
cd Arcadia-Spawn-Dimension
./gradlew build
```

Le JAR compile sera dans `build/libs/`.

## Contribuer

Les contributions sont les bienvenues ! Lisez notre [Guide de Contribution](.github/CONTRIBUTING.md) avant de soumettre une pull request.

## Liens

- [Arcadia: Echoes of Power](https://arcadia-echoes-of-power.fr/)
- [Discord](https://discord.gg/xjF8Rtzyd4)
- [Donation](https://buy.stripe.com/3cI3co6X97Vy4IK50QfIs00)

## Credits

**Auteur :** vyrriox
**Organisation :** [Team Arcadia](https://github.com/Team-Arcadia)
