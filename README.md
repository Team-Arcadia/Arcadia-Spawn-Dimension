# Arcadia Spawn

![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-red) ![Version](https://img.shields.io/badge/version-1.5.0-blue) ![Side](https://img.shields.io/badge/side-BOTH-green) ![MC](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)

Advanced spawn dimension management, dynamic lobby menu, and RTP system for the Arcadia ecosystem.

## Features

- **Arcadia Hub Integration** — Registered as first card in the Arcadia Hub (via arcadia-lib).
- **Dynamic Lobby Menu** — GUI accessible via `/lobby` with configurable teleport points, custom icons, and descriptions.
- **Spawn Dimension** — Fully configurable custom dimension (biome, layers, time, weather, mob spawning).
- **Random Teleport (RTP)** — `/arcadiartp` with configurable radius, usage limits, warmup, and cooldown.
- **Teleport Warmup & Cooldown** — All teleports use arcadia-lib's TeleportManager with movement cancellation.
- **Slot Bypass** — Permission-based server slot limiting with fake max display and join/leave message hiding.
- **Debug Suite** — 13 debug subcommands for full server diagnostics.
- **Bilingual** — Full English and French support (auto-detected from client).

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.219+
- [arcadia-lib](https://github.com/vyrriox) 1.2.0+

## Commands

### Player Commands
| Command | Description |
|---------|-------------|
| `/lobby` | Opens the lobby teleport menu |
| `/spawn` | Teleport to the spawn dimension |
| `/arcadiartp` | Random teleport in Overworld |

### Admin Commands (`/arcadia_spawn`, requires op 2)
| Command | Description |
|---------|-------------|
| `reload` | Reload lobby configuration |
| `setlobbytp <name> [item] [desc]` | Create warp point |
| `dellobbytp <name>` | Delete warp point |
| `edit <name> description/item/location` | Modify warp point |
| `tp <name>` | Teleport to warp point |
| `setspawn` | Set spawn at current position |
| `/setlobbyspawn` | Alias for setspawn |

### Debug Commands (`/arcadia_spawn debug`)
| Command | Description |
|---------|-------------|
| `status` | Full mod status overview |
| `spawn` | Spawn dimension details |
| `dimension` | Dimension config dump |
| `lobbies` | List all lobby locations |
| `rtp` | Player RTP data |
| `rtp reset` | Reset RTP usage |
| `config` | Dump all active config |
| `slots` | Slot bypass status |
| `player` | Player info (UUID, lang, pos) |
| `reload_all` | Force reload lobbies + languages |
| `reset_visited` | Remove first-join tag |
| `tps` | Server TPS and tick timing |
| `lang [key]` | Language info / test key |

## Configuration

- **Spawn Config**: `config/arcadia/spawn/config.toml`
- **Slot Bypass**: `config/arcadia/spawn/slot_bypass.toml`
- **Lobby Data**: `config/arcadia/spawn/lobbies/*.json`

### Permission Node
- `arcadia_spawn.slots.bypass` — Bypass slot limit (LuckPerms compatible)

## Credits

Author: vyrriox

---

# Arcadia Spawn (Version Francaise)

Gestion avancee de la dimension spawn, menu lobby dynamique et systeme RTP pour l'ecosysteme Arcadia.

## Caracteristiques

- **Integration Hub Arcadia** — Enregistre comme premiere carte dans le Hub Arcadia (via arcadia-lib).
- **Menu Lobby Dynamique** — Interface accessible via `/lobby` avec points de teleportation configurables.
- **Dimension Spawn** — Dimension personnalisee entierement configurable (biome, couches, temps, meteo, mobs).
- **Teleportation Aleatoire (RTP)** — `/arcadiartp` avec rayon, limites, warmup et cooldown configurables.
- **Warmup & Cooldown** — Toutes les teleportations utilisent le TeleportManager d'arcadia-lib avec annulation sur mouvement.
- **Bypass de Slots** — Limitation de slots basee sur les permissions avec affichage de faux maximum.
- **Suite Debug** — 13 sous-commandes de debug pour le diagnostic serveur complet.
- **Bilingue** — Support complet anglais et francais (detection automatique).

## Pre-requis

- Minecraft 1.21.1
- NeoForge 21.1.219+
- [arcadia-lib](https://github.com/vyrriox) 1.2.0+

## Commandes

### Commandes Joueur
| Commande | Description |
|----------|-------------|
| `/lobby` | Ouvre le menu de teleportation lobby |
| `/spawn` | Teleportation vers la dimension spawn |
| `/arcadiartp` | Teleportation aleatoire dans l'Overworld |

### Commandes Admin (`/arcadia_spawn`, necessite op 2)
| Commande | Description |
|----------|-------------|
| `reload` | Recharger la configuration lobby |
| `setlobbytp <nom> [item] [desc]` | Creer un point de warp |
| `dellobbytp <nom>` | Supprimer un point de warp |
| `edit <nom> description/item/location` | Modifier un point de warp |
| `tp <nom>` | Se teleporter a un point de warp |
| `setspawn` | Definir le spawn a la position actuelle |

### Commandes Debug (`/arcadia_spawn debug`)
| Commande | Description |
|----------|-------------|
| `status` | Vue d'ensemble du mod |
| `spawn` | Details de la dimension spawn |
| `dimension` | Dump de la config dimension |
| `lobbies` | Lister tous les lobbies |
| `rtp` | Donnees RTP du joueur |
| `rtp reset` | Reinitialiser le compteur RTP |
| `config` | Dump de toute la config active |
| `slots` | Statut du bypass de slots |
| `player` | Info joueur (UUID, langue, pos) |
| `reload_all` | Recharger lobbies + langues |
| `reset_visited` | Supprimer le tag premier join |
| `tps` | TPS serveur et timing des ticks |
| `lang [cle]` | Info langue / tester une cle |

## Configuration

- **Config Spawn** : `config/arcadia/spawn/config.toml`
- **Bypass Slots** : `config/arcadia/spawn/slot_bypass.toml`
- **Donnees Lobby** : `config/arcadia/spawn/lobbies/*.json`

## Credits

Author: vyrriox

---

### Links / Liens
- **Website**: [Arcadia: Echoes Of Power](https://arcadia-echoes-of-power.fr/)
- **Support**: [Discord](https://discord.gg/xjF8Rtzyd4)
