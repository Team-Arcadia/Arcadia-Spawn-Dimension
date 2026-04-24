# Changelog

All notable changes to Arcadia Spawn are documented here.

---

## [1.5.1] - 2026-04-23 (latest)

### Fixed

- **Invisible mobs in the Arcadia Spawn dimension** — `MobSpawnHandler` subscribed to `EntityJoinLevelEvent` on both sides without an `isClientSide()` guard. Combined with `SpawnConfig` being registered as `ModConfig.Type.COMMON` (local to each side, no server→client sync), a dedicated server whose config allowed a mob category would spawn the entity, but the remote client — reading its own default (false) config — would re-evaluate the event and cancel the entity add client-side. Result: the server kept the mob (AI, sounds, hitbox) while the client never added it to its `ClientLevel`, so players could hear mobs but not see them; flipping `spawn_*` to false on the server only hid them harder. Handler now short-circuits on `event.getLevel().isClientSide()` — the server's cancel already stops the add-entity packet from being sent, so the client has nothing to filter. Kept off `Dist` so the integrated server in singleplayer still runs the filter.

### Correctifs

- **Mobs invisibles dans la dimension Arcadia Spawn** — `MobSpawnHandler` s'abonnait à `EntityJoinLevelEvent` des deux côtés sans garde `isClientSide()`. Combiné à `SpawnConfig` enregistrée en `ModConfig.Type.COMMON` (locale à chaque côté, pas de sync serveur→client), un serveur dédié dont la config autorisait une catégorie de mob faisait spawn l'entité, mais le client distant — qui lit sa propre config locale par défaut (false) — réévaluait l'event et annulait l'ajout côté client. Résultat : le serveur gardait le mob (IA, sons, hitbox) alors que le client ne l'ajoutait jamais à son `ClientLevel` — les joueurs entendaient les mobs mais ne les voyaient pas ; passer `spawn_*` à false côté serveur ne faisait que les cacher plus fort. Le handler court-circuite désormais sur `event.getLevel().isClientSide()` — le cancel serveur empêche déjà l'envoi du packet d'ajout d'entité, le client n'a rien à filtrer. Pas de filtre `Dist` pour que le serveur intégré en solo lance le filtrage.

---

## [1.5.0] - 2026-04-14

### Added

- **arcadia-lib Integration** — Full integration with arcadia-lib ecosystem. Hub card registered at first position. Uses ArcadiaMessages for themed messaging, TeleportManager for warmup/cooldown teleports.
- **Debug Command Suite** — 13 diagnostic subcommands under `/arcadia_spawn debug`: status, spawn, dimension, lobbies, rtp, config, slots, player, reload_all, reset_visited, tps, lang.
- **Teleport Warmup & Cooldown** — All teleports (/spawn, /lobby, /arcadiartp) now support configurable warmup delays and cooldowns via arcadia-lib TeleportManager with movement cancellation.
- **Enhanced Configuration** — New config options: `force_spawn_on_first_join`, `force_spawn_on_respawn`, `rtp_cooldown_seconds`, `rtp_warmup_ticks`, `rtp_max_attempts`, `spawn_tp_warmup_ticks`, `spawn_tp_cooldown_seconds`, `lobby_tp_warmup_ticks`, `lobby_tp_cooldown_seconds`.
- **Lobby Menu UX** — Glass pane borders, bilingual lore text ("Click to teleport | Cliquez pour vous tp").

### Changed

- **Package** — Moved from `com.vyrriox.arcadiaspawn` to `com.arcadia.spawn` for ecosystem consistency.
- **Build System** — Migrated from `net.neoforged.gradle.userdev` to `net.neoforged.moddev` 2.0.140. Added arcadia-lib 1.2.0 as dependency.
- **Side** — Changed from server-only to BOTH (client + server) for Hub integration.
- **Command Structure** — Admin commands consolidated under `/arcadia_spawn` (was `/arcadialobby`). Player commands `/lobby`, `/spawn`, `/arcadiartp` unchanged.
- **Config Paths** — Moved from `config/arcadia/arcadialobbyspawn/` to `config/arcadia/spawn/`. Lobby data now in `config/arcadia/spawn/lobbies/`.
- **Mixin Config** — Renamed from `arcadiaspawn.mixins.json` to `arcadia_spawn.mixins.json`.
- **NeoForge Version** — Updated to 21.1.219.

### Fixed

- **Spawn Dimension Bug** — `/setlobbyspawn` and `/arcadia_spawn setspawn` now store the dimension alongside coordinates. The spawn teleport correctly targets the dimension where spawn was set, not just `arcadia:spawn`. This prevents wrong-dimension teleports when spawn is set outside the spawn dimension.
- **Respawn Bug** — Players dying without a bed/anchor are now correctly teleported to the configured spawn point instead of world spawn. Uses `server.execute()` to schedule after respawn completion.
- **FTB Essentials /back Compatibility** — All teleportations now fire `EntityTeleportEvent.TeleportCommand` before executing, allowing FTB Essentials and other mods to record the pre-teleport position for `/back`.
- **RTP Thread Safety** — Replaced `java.util.Random` with `ThreadLocalRandom` to eliminate synchronization contention.
- **Kick Message** — Slot bypass kick message now bilingual by default (EN | FR).

### Performance

- **ThreadLocalRandom** — RTP safe position search uses lock-free random for better throughput.
- **Configurable RTP Attempts** — `rtp_max_attempts` limits worst-case chunk loading (default 50).
- **Zero Tick Handlers** — Mod registers no tick listeners. All logic is event-driven.

---

### Ajouts

- **Integration arcadia-lib** — Integration complete avec l'ecosysteme arcadia-lib. Carte Hub en premiere position. Utilise ArcadiaMessages pour les messages themes, TeleportManager pour les teleportations avec warmup/cooldown.
- **Suite de Commandes Debug** — 13 sous-commandes de diagnostic sous `/arcadia_spawn debug` : status, spawn, dimension, lobbies, rtp, config, slots, player, reload_all, reset_visited, tps, lang.
- **Warmup & Cooldown de Teleportation** — Toutes les teleportations (/spawn, /lobby, /arcadiartp) supportent maintenant des delais de warmup et cooldowns configurables via le TeleportManager d'arcadia-lib avec annulation sur mouvement.
- **Configuration Etendue** — Nouvelles options : `force_spawn_on_first_join`, `force_spawn_on_respawn`, `rtp_cooldown_seconds`, `rtp_warmup_ticks`, `rtp_max_attempts`, etc.
- **UX Menu Lobby** — Bordures en vitres, texte de lore bilingue.

### Modifications

- **Package** — Deplacement de `com.vyrriox.arcadiaspawn` vers `com.arcadia.spawn`.
- **Systeme de Build** — Migration vers `net.neoforged.moddev` 2.0.140 avec arcadia-lib 1.2.0.
- **Side** — Passe de serveur uniquement a BOTH (client + serveur) pour l'integration Hub.
- **Structure des Commandes** — Commandes admin sous `/arcadia_spawn` (etait `/arcadialobby`). `/lobby`, `/spawn`, `/arcadiartp` inchanges.
- **Chemins de Config** — Deplaces de `config/arcadia/arcadialobbyspawn/` vers `config/arcadia/spawn/`.

### Correctifs

- **Bug Dimension Spawn** — `/setlobbyspawn` et `/arcadia_spawn setspawn` stockent maintenant la dimension avec les coordonnees. La teleportation cible correctement la dimension ou le spawn a ete defini.
- **Bug Respawn** — Les joueurs mourant sans lit/ancre sont maintenant correctement teleportes au spawn configure au lieu du world spawn. Utilise `server.execute()` pour planifier apres la fin du respawn.
- **Compatibilite FTB Essentials /back** — Toutes les teleportations declenchent maintenant `EntityTeleportEvent.TeleportCommand` avant execution, permettant a FTB Essentials et autres mods d'enregistrer la position pre-teleportation pour `/back`.
- **Thread Safety RTP** — Remplacement de `java.util.Random` par `ThreadLocalRandom`.
- **Message de Kick** — Message de kick bilingue par defaut (EN | FR).

### Performance

- **ThreadLocalRandom** — Recherche de position RTP sans verrou pour un meilleur debit.
- **Tentatives RTP Configurables** — `rtp_max_attempts` limite le chargement de chunks worst-case.
- **Zero Tick Handlers** — Le mod n'enregistre aucun listener de tick. Tout est evenementiel.

---

## [1.4.3] - 2026-03-11

### Changed
- **Version bump** — Recompile for release.

### Modifications
- **Bump de version** — Recompilation pour la release.

---

## [1.4.2] - 2026-03-11

### Added
- **Slot Bypass System** — Permission-based slot limiting with `arcadia_spawn.slots.bypass` node.
- **Mixins** — `fake_max_slots_enabled` and `hide_join_leave_messages` tweaks.

### Fixed
- **`/setlobbyspawn`** — Command was not registered in dispatcher.

### Ajouts
- **Systeme de Bypass de Slots** — Limitation de slots basee sur les permissions.
- **Mixins** — Ajustements `fake_max_slots_enabled` et `hide_join_leave_messages`.

### Correctifs
- **`/setlobbyspawn`** — La commande n'etait pas enregistree dans le dispatcher.

---

## [1.4.1] - 2026-02-03

### Added
- **Command consolidation** — Commands grouped under `/arcadialobby`.
- **Edit command** — `/arcadialobby edit <name> [description|item|location]`.
- **Direct TP** — `/arcadialobby tp <name>`.

### Ajouts
- **Consolidation des commandes** — Regroupement sous `/arcadialobby`.
- **Commande Edit** — `/arcadialobby edit <nom> [description|item|location]`.
- **TP Direct** — `/arcadialobby tp <nom>`.

---

## [1.4.0] - 2026-01-23

### Added
- **RTP Command** — `/arcadiartp` with configurable radius and usage limit.
- **Persistence** — RTP data via NeoForge Data Attachments.

### Ajouts
- **Commande RTP** — `/arcadiartp` avec rayon et limite configurables.
- **Persistance** — Donnees RTP via NeoForge Data Attachments.

---

## [1.3.0] - 2026-01-16

### Added
- **Full Configuration** — `config.toml` for all dimension properties.
- **Mob Spawning Control** — Per-category mob spawn toggles.

### Ajouts
- **Configuration Complete** — `config.toml` pour toutes les proprietes de dimension.
- **Controle des Mobs** — Toggles de spawn par categorie.

---

## [1.2.0] - 2026-01-14

### Added
- **Localization** — English + French auto-detection.
- **Custom Icons** — Item icons for lobby warps.

### Ajouts
- **Localisation** — Anglais + Francais avec detection automatique.
- **Icones Personnalisees** — Items pour les warps lobby.

---

## [1.1.0] - 2026-01-12

### Added
- **Lobby Menu** — Dynamic GUI via `/lobby`.
- **Commands** — `/setlobbytp`, `/dellobbytp`, reload.

### Ajouts
- **Menu Lobby** — Interface dynamique via `/lobby`.
- **Commandes** — `/setlobbytp`, `/dellobbytp`, reload.

---

## [1.0.0] - Initial Release

### Added
- **Spawn System** — `/spawn` command with fall damage prevention.

### Ajouts
- **Systeme de Spawn** — Commande `/spawn` avec prevention des degats de chute.
