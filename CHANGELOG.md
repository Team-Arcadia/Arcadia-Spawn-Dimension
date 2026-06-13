# Changelog

All notable changes to Arcadia Spawn are documented here.

---

## [1.5.6] - 2026-06-13 (latest)

### Fixed

- **Staff stayed invisible after leaving spectator mode** — The spectator tab-list hider sent a `ClientboundPlayerInfoRemovePacket` to hide a spectator from everyone else's tab. That packet deletes the player's `PlayerInfo` on every other client — and the vanilla client then refuses to spawn that player's entity (`ClientPacketListener.createEntityFromPacket` null-checks the `PlayerInfo` and silently drops the spawn). So when a staff member returned to creative/survival, the re-add packet restored the tab row but their **entity** never re-appeared for anyone who re-tracked them, until a full re-track (relog, dimension change, leaving and re-entering view distance) happened. The hide/reveal mechanism now toggles the per-player `UPDATE_LISTED` flag instead (`listed=false` to hide the tab row, `listed=true` to restore it). The client keeps the `PlayerInfo` and the entity at all times, so the player stays rendered and the invisibility is gone.

### Changed

- **Spectator visibility reconciled on login** — `PlayerLoggedInEvent` now runs `SpectatorVisibility.reconcile()` so a player relogging while in spectator is hidden immediately, and a player relogging out of spectator is revealed immediately, instead of waiting for the next refresh tick.
- **Access transformer exposes `ClientboundPlayerInfoUpdatePacket.entries`** — The vanilla public packet constructors hard-code `listed=true`, so a one-line AT makes the `entries` field writable, letting the mod emit a genuine `listed=false` update for a single player.

### Correctifs

- **Le staff restait invisible après être sorti du mode spectateur** — Le masquage des spectateurs dans le TAB envoyait un `ClientboundPlayerInfoRemovePacket` pour cacher un spectateur du TAB des autres. Ce paquet supprime la `PlayerInfo` du joueur sur tous les autres clients — et le client vanilla refuse alors de faire apparaître l'entité de ce joueur (`ClientPacketListener.createEntityFromPacket` vérifie la nullité de la `PlayerInfo` et abandonne silencieusement l'apparition). Ainsi, quand un membre du staff revenait en créatif/survie, le paquet de réajout restaurait la ligne du TAB mais son **entité** ne réapparaissait jamais pour quiconque le re-suivait, jusqu'à un re-suivi complet (reconnexion, changement de dimension, sortie puis retour dans la distance de vue). Le mécanisme cache/révèle bascule désormais le drapeau `UPDATE_LISTED` par joueur (`listed=false` pour masquer la ligne du TAB, `listed=true` pour la restaurer). Le client conserve la `PlayerInfo` et l'entité en permanence, donc le joueur reste affiché et l'invisibilité disparaît.

### Modifications

- **Visibilité spectateur réconciliée à la connexion** — `PlayerLoggedInEvent` exécute désormais `SpectatorVisibility.reconcile()` pour qu'un joueur qui se reconnecte en spectateur soit masqué immédiatement, et qu'un joueur qui se reconnecte hors spectateur soit révélé immédiatement, au lieu d'attendre le prochain tick de rafraîchissement.
- **L'access transformer expose `ClientboundPlayerInfoUpdatePacket.entries`** — Les constructeurs publics vanilla du paquet forcent `listed=true` ; un AT d'une ligne rend donc le champ `entries` modifiable, permettant au mod d'émettre une véritable mise à jour `listed=false` pour un seul joueur.

---

## [1.5.5] - 2026-06-09

### Changed

- **Migrated to arcadia-lib 1.2.14** — Bumped the bundled `arcadia-lib` from `1.2.0` to `1.2.14` and raised the `mods.toml` dependency to `[1.2.14,)`. The library API the mod consumes (`ArcadiaModRegistry`, `ArcadiaModCard`, `DatabaseManager`, `ArcadiaMessages`, `CooldownManager`, `DashboardTabHandler`, `ServerContext`, `TableDefinition`, `ArcadiaTheme`) is source-compatible, but 1.2.14 changed runtime behaviour — most importantly the permission backend is now **fail-closed (DENY)** by default on dedicated servers. The audit fixes below align the mod's own permission paths with that new contract.

### Security

- **Slot bypass now fails CLOSED on a permission-check error** — `SlotBypassHandler` caught any exception from `PermissionAPI.getPermission()` and silently let the player in ("fail-open"). With arcadia-lib 1.2.14's fail-closed backend, a thrown check means "could not verify", not "allowed" — so a LuckPerms crash / DB outage during a full server would let unlimited players past the slot limit. An exception now denies and disconnects the player with a bilingual message.
- **`/arcadia_spawn dimension delete` now validates its id (path traversal)** — `deleteDimension()` passed the raw command id straight to `CustomDimensionManager.exists()` / `delete()`, which resolve it into a `<id>.json` filesystem path — while `createDimension()` validated. An admin could pass `../../…` to escape the dimensions directory. `delete` now runs the same `InputValidation.isValidDimensionId()` gate as `create`.
- **`PermissionRegistry.require()` fails closed when LuckPerms is present** — A thrown permission check fell back to vanilla op-level, so an op-2 admin kept access to a node the operator had deliberately gated behind LuckPerms whenever the backend errored. It now denies on exception **when LuckPerms is on the classpath**, and only falls back to op-level when there is genuinely no permission backend (singleplayer / no perms plugin).

### Fixed

- **`LocalizationManager.TRANSLATIONS` is thread-safe** — The translation table was a plain `HashMap` written on `init()` / `/spawn reload` and read from command and teleport message paths; a reload `put()` mid-resize could be observed as a torn read. Switched to `ConcurrentHashMap`, with a null-map guard for empty/malformed language files (which `ConcurrentHashMap` would otherwise reject).
- **Malformed lobby entries are skipped, not NPE-recovered** — `LobbyManager.loadFile()` read required JSON fields with `.get(field).getAsX()`, relying on a caught `NullPointerException` for a missing field. Required fields are now validated with `.has()` and a single bad entry is skipped with a warning instead of aborting the whole file into backup recovery.

### Performance

- **Tab-list team sync is O(1) instead of O(teams)** — `syncTeamFor()` scanned every scoreboard team each sync (per player, per refresh tick) to find a stale `as_*` team to remove the player from. It now uses the O(1) reverse lookup `scoreboard.getPlayersTeam()`.
- **Placeholder expansion skips work for absent placeholders** — `PlaceholderFormatter.expand()` ran 13 sequential `String.replace()` calls plus unconditional `formatTps/Mspt/Uptime` and a per-line `GradeResolver.resolve()` (a LuckPerms cache traversal) on every line, for every player, every refresh. Each replacement and its value are now guarded on a cheap `contains()` check, so a line that uses no `%lp_*%` placeholder never touches LuckPerms and a line with no `%tps%` never formats it.
- **Server display name resolved once per refresh** — `apply()` recomputed `resolveServerDisplayName()` for every player in the refresh loop though it is identical across players; it is now resolved once and passed in.
- **`%cross_total%` no longer recomputes the local server id per peer** — `localServerId()` is hoisted out of the peer-summing loop.

### Fixed (config)

- **`peer_order` config now actually orders peers** — The `peer_order` tab-list option was defined and documented but never read; the `%peers%` footer always rendered in arbitrary DB order. `expandPeers()` now sorts peers by the configured order, appending unknown peers at the end.

---

### Modifications

- **Migration vers arcadia-lib 1.2.14** — `arcadia-lib` embarquée passée de `1.2.0` à `1.2.14` et dépendance `mods.toml` relevée à `[1.2.14,)`. L'API de la lib utilisée par le mod (`ArcadiaModRegistry`, `ArcadiaModCard`, `DatabaseManager`, `ArcadiaMessages`, `CooldownManager`, `DashboardTabHandler`, `ServerContext`, `TableDefinition`, `ArcadiaTheme`) est source-compatible, mais 1.2.14 change le comportement à l'exécution — surtout le backend de permissions désormais **fail-closed (DENY)** par défaut sur serveur dédié. Les correctifs d'audit ci-dessous alignent les chemins de permission du mod sur ce nouveau contrat.

### Sécurité

- **Le slot bypass échoue désormais en mode FERMÉ sur erreur de vérification** — `SlotBypassHandler` attrapait toute exception de `PermissionAPI.getPermission()` et laissait silencieusement entrer le joueur (« fail-open »). Avec le backend fail-closed de 1.2.14, une vérification qui lève signifie « impossible de vérifier », pas « autorisé » — donc un crash LuckPerms / panne DB sur un serveur plein laissait passer un nombre illimité de joueurs au-delà de la limite de slots. Une exception refuse et déconnecte désormais le joueur avec un message bilingue.
- **`/arcadia_spawn dimension delete` valide désormais son id (traversée de chemin)** — `deleteDimension()` passait l'id brut directement à `CustomDimensionManager.exists()` / `delete()`, qui le résolvent en un chemin fichier `<id>.json` — alors que `createDimension()` validait. Un admin pouvait passer `../../…` pour sortir du dossier des dimensions. `delete` applique maintenant la même garde `InputValidation.isValidDimensionId()` que `create`.
- **`PermissionRegistry.require()` échoue en mode fermé quand LuckPerms est présent** — Une vérification de permission qui lève retombait sur le niveau op vanilla, donc un admin op-2 gardait l'accès à un node que l'opérateur avait délibérément réservé à LuckPerms dès que le backend était en erreur. Il refuse désormais sur exception **quand LuckPerms est dans le classpath**, et ne retombe sur le niveau op que s'il n'y a réellement aucun backend de permissions (solo / pas de plugin de perms).

### Correctifs

- **`LocalizationManager.TRANSLATIONS` est thread-safe** — La table de traductions était une `HashMap` simple écrite au `init()` / `/spawn reload` et lue depuis les chemins de messages de commandes et de téléportation ; un `put()` de reload en plein redimensionnement pouvait être observé comme une lecture déchirée. Passée en `ConcurrentHashMap`, avec une garde contre les maps nulles (fichiers de langue vides/malformés que `ConcurrentHashMap` rejetterait).
- **Les entrées de lobby malformées sont ignorées, pas récupérées par NPE** — `LobbyManager.loadFile()` lisait les champs JSON requis via `.get(champ).getAsX()`, comptant sur une `NullPointerException` attrapée pour un champ manquant. Les champs requis sont maintenant validés avec `.has()` et une seule entrée fautive est ignorée avec un warning au lieu d'abandonner tout le fichier vers la récupération de backup.

### Performance

- **La sync des teams de tab list est en O(1) au lieu de O(teams)** — `syncTeamFor()` scannait chaque team du scoreboard à chaque sync (par joueur, par tick de refresh) pour retirer le joueur d'une ancienne team `as_*`. Il utilise désormais la recherche inverse O(1) `scoreboard.getPlayersTeam()`.
- **L'expansion de placeholders saute le travail pour les placeholders absents** — `PlaceholderFormatter.expand()` exécutait 13 `String.replace()` séquentiels plus `formatTps/Mspt/Uptime` inconditionnels et un `GradeResolver.resolve()` par ligne (un parcours du cache LuckPerms) sur chaque ligne, pour chaque joueur, à chaque refresh. Chaque remplacement et sa valeur sont désormais gardés par un `contains()` peu coûteux, donc une ligne sans placeholder `%lp_*%` ne touche jamais LuckPerms et une ligne sans `%tps%` ne le formate jamais.
- **Nom d'affichage du serveur résolu une fois par refresh** — `apply()` recalculait `resolveServerDisplayName()` pour chaque joueur dans la boucle de refresh alors qu'il est identique pour tous ; il est désormais résolu une fois et transmis.
- **`%cross_total%` ne recalcule plus l'id du serveur local par peer** — `localServerId()` est sorti de la boucle de somme des peers.

### Correctifs (config)

- **`peer_order` ordonne désormais réellement les peers** — L'option de tab list `peer_order` était définie et documentée mais jamais lue ; le footer `%peers%` s'affichait toujours dans l'ordre arbitraire de la DB. `expandPeers()` trie maintenant les peers selon l'ordre configuré, en ajoutant les peers inconnus à la fin.

---

## [1.5.4] - 2026-06-03

### Fixed

- **Player & entity collisions restored** — The grade-sorting tab list placed every player into a scoreboard team created with `CollisionRule.NEVER`. Because a team's collision rule overrides vanilla pushing, `EntitySelector.pushableBy` returned "push nothing", disabling all player↔player **and** player↔entity collisions. The rule now defaults to `ALWAYS` (vanilla) and is re-applied on every team sync, so teams previously persisted to `scoreboard.dat` with the broken rule are repaired automatically. A new `collision_rule` option (`ALWAYS` / `NEVER` / `PUSH_OWN_TEAM` / `PUSH_OTHER_TEAMS`) makes the behaviour configurable.
- **RTP no longer dumps players at (0, 0, 0)** — `findRandomSafePos()` returned a hardcoded world-origin column when every attempt failed, skipping the fluid/ground safety checks and teleporting players into the void or lava at spawn with a misleading "success" message. It now signals failure, surfacing the already-localized "Could not find a safe position" message instead.
- **Dimension load crash on extreme `min_y`** — `min_y` was passed to `DimensionType` unclamped (unlike `height` / `logical_height`), so an out-of-range or non-multiple-of-16 value crashed dimension registration at load. `min_y` is now snapped to a multiple of 16 and clamped to Minecraft's hard limits in both the `arcadia:spawn` and custom-dimension paths.
- **Infinite recursion in dimension backup recovery** — `CustomDimensionManager.loadOne()` retried a corrupt definition from its newest backup by calling itself; if that backup was also corrupt it recursed forever (`StackOverflowError`). Recovery is now capped at a single retry.
- **Phantom lobby teleports** — In the `/lobby` menu, clicks were bound to `locations.size()` instead of the 7 rendered icon slots, so with 8+ lobbies configured a click on a decorative glass pane teleported to a never-shown location. Clicks are now bound to the actual icon slots (10–16).
- **Lobby file namespace collision** — Lobby files were named and matched by dimension *path* only, so two dimensions sharing a path across namespaces (e.g. `minecraft:lobby` / `arcadia:lobby`) overwrote each other's data. Files are now namespaced (`<namespace>.<path>.json`) and matched on the full `ResourceKey`; legacy files are migrated automatically on load with no data loss.
- **Silent diagnostics gaps** — Invalid LuckPerms integer meta, failed translation `String.format` calls, and failed backup deletions were swallowed without a trace. Each now logs a warning while keeping the safe fallback behaviour.

### Changed

- **Mod version is now sourced from Gradle** — `neoforge.mods.toml` used a hardcoded `version` that had drifted to `1.5.0`. It now expands `${mod_version}` from the build during `processResources`, so the displayed version can never go stale. `license` is corrected to `LGPL-3.0-or-later` and the issue-tracker URL is populated.

### Performance

- **Bounded RTP chunk generation** — `/arcadiartp` force-generated a chunk on the server thread for every attempt (up to `rtp_max_attempts`), able to freeze the server for seconds in unexplored terrain. It now evaluates already-loaded chunks for free (`getChunkNow`) and caps forced generation at 8 chunks per call, bounding the worst-case stall while still reaching fresh terrain.

---

### Correctifs

- **Collisions joueurs & entités restaurées** — La tab list triée par grade plaçait chaque joueur dans une scoreboard team créée avec `CollisionRule.NEVER`. Comme la règle de collision d'une team override le push vanilla, `EntitySelector.pushableBy` renvoyait « ne pousse rien », désactivant toutes les collisions joueur↔joueur **et** joueur↔entité. La règle est désormais `ALWAYS` (vanilla) par défaut et ré-appliquée à chaque sync de team, donc les teams déjà persistées dans `scoreboard.dat` avec la mauvaise règle sont réparées automatiquement. Une nouvelle option `collision_rule` (`ALWAYS` / `NEVER` / `PUSH_OWN_TEAM` / `PUSH_OTHER_TEAMS`) rend le comportement configurable.
- **RTP ne téléporte plus en (0, 0, 0)** — `findRandomSafePos()` renvoyait une colonne codée en dur à l'origine du monde quand toutes les tentatives échouaient, en sautant les vérifications fluide/sol et en téléportant le joueur dans le vide ou la lave au spawn avec un message « succès » trompeur. La méthode signale désormais l'échec, affichant à la place le message déjà localisé « Impossible de trouver une position sûre ».
- **Crash de chargement de dimension sur `min_y` extrême** — `min_y` était passé à `DimensionType` sans clamp (contrairement à `height` / `logical_height`), donc une valeur hors limites ou non multiple de 16 crashait l'enregistrement de la dimension au chargement. `min_y` est maintenant aligné sur un multiple de 16 et borné aux limites dures de Minecraft, dans le chemin `arcadia:spawn` comme dans celui des dimensions personnalisées.
- **Récursion infinie dans la récupération de backup de dimension** — `CustomDimensionManager.loadOne()` retentait une définition corrompue depuis son backup le plus récent en s'appelant lui-même ; si ce backup était lui aussi corrompu, la récursion ne s'arrêtait jamais (`StackOverflowError`). La récupération est désormais limitée à une seule tentative.
- **Téléportations de lobby fantômes** — Dans le menu `/lobby`, les clics étaient bornés à `locations.size()` au lieu des 7 slots d'icônes affichés, donc avec 8+ lobbies configurés un clic sur une vitre décorative téléportait vers un emplacement jamais affiché. Les clics sont maintenant bornés aux vrais slots d'icônes (10–16).
- **Collision de namespace des fichiers de lobby** — Les fichiers de lobby étaient nommés et filtrés par le *path* de la dimension uniquement, donc deux dimensions partageant un path entre namespaces (ex. `minecraft:lobby` / `arcadia:lobby`) écrasaient leurs données mutuellement. Les fichiers sont désormais namespacés (`<namespace>.<path>.json`) et filtrés sur le `ResourceKey` complet ; les anciens fichiers sont migrés automatiquement au chargement sans perte de données.
- **Trous de diagnostic silencieux** — Une meta entière LuckPerms invalide, un `String.format` de traduction en échec, et une suppression de backup en échec étaient avalés sans trace. Chacun log désormais un warning tout en conservant le fallback sûr.

### Modifications

- **La version du mod provient désormais de Gradle** — `neoforge.mods.toml` utilisait une `version` codée en dur qui avait dérivé à `1.5.0`. Elle expand maintenant `${mod_version}` depuis le build pendant `processResources`, donc la version affichée ne peut plus être obsolète. `license` est corrigée en `LGPL-3.0-or-later` et l'URL du tracker d'issues est renseignée.

### Performance

- **Génération de chunks RTP bornée** — `/arcadiartp` forçait la génération d'un chunk sur le thread serveur à chaque tentative (jusqu'à `rtp_max_attempts`), pouvant geler le serveur plusieurs secondes en terrain inexploré. Il évalue désormais gratuitement les chunks déjà chargés (`getChunkNow`) et limite la génération forcée à 8 chunks par appel, bornant le gel maximal tout en atteignant le terrain neuf.

---

## [1.5.3] - 2026-05-19

### Added

- **Custom tab list with rank sorting** — When `tablist.enabled = true`, the vanilla tab list is replaced by a configurable header/footer rendered for every connected player. Header and footer accept multi-line templates with `&`-style color codes and placeholders (`%server%`, `%online%`, `%max%`, `%tps%`, `%mspt%`, `%uptime%`, `%player_name%`, `%player_ping%`, `%player_playtime%`, `%lp_group%`, `%lp_prefix%`, `%cross_total%`, `%peers%`). Refresh interval is configurable (default 40 ticks = 2s).
- **LuckPerms-aware grade sorting** — Players are bucketed into Minecraft `PlayerTeam`s named `as_<999-weight>_<group>`, so the highest LuckPerms weight always appears at the top of the tab. Group prefixes from LP meta are rendered in front of the player name, and an optional `color` meta entry recolors the player's name with the nearest matching ChatFormatting. Listens to LuckPerms `UserDataRecalculateEvent` for instant promotion/demotion updates.
- **Fallback ranks without LuckPerms** — Without LuckPerms, players are still sorted by their vanilla op level: Owner (op4), Admin (op3), Mod (op2), Helper (op1), Default. No external dependency required for the basic grade view.
- **Cross-server player count via arcadia-lib DB** — When `cross_server_enabled = true` and arcadia-lib's `DatabaseManager` is active, the mod creates an `arcadia_tablist_servers` table (auto-`CREATE TABLE IF NOT EXISTS` via `TableDefinition`), heartbeats this server's row every `heartbeat_interval_seconds` (default 10s), and reads peers asynchronously to expand the `%peers%` line of the footer into one row per server. Stale peers (no heartbeat for `peer_timeout_seconds`) are displayed as offline. All JDBC goes through `DatabaseManager.executeAsync` / `supplyAsync` — the main server thread never blocks on a query. Fail-soft when the DB is down: the local tab still renders, peer rows just show "no peers reachable".
- **`/arcadia_spawn tablist` admin subcommands** — `reload` forces an immediate refresh for every connected player. `status` reports the live state (enabled flag, LP detection, DB availability, this server's id). `peers` prints all rows currently stored in the shared DB with their ALIVE/STALE flag.
- **Server cleanup on shutdown** — On `ServerStoppingEvent`, the mod removes this server's row from `arcadia_tablist_servers` so other servers' footers don't continue to display it as alive until the timeout expires.

(Plus everything previously documented for 1.5.3 below — custom dimensions, permission nodes, input validation, rate limiting, atomic persistence, performance optimizations.)

- **Custom dimension system** — New `/arcadia_spawn dimension create <id> [preset] [biome]`, `/arcadia_spawn dimension delete <id> [purge]`, and `/arcadia_spawn dimension list` commands. Definitions are serialized to `config/arcadia/spawn/dimensions/<id>.json` and registered at server startup under the dedicated `arcadia_custom:` namespace. Presets ship for `flat`, `void`, and `lobby`. Restart is required for newly-created dimensions to be picked up — this is a NeoForge limitation (level stems can only be registered during `RegisterEvent`). The original `arcadia:spawn` dimension is untouched; everything is opt-in.
- **Mod-removal manifest** — A `_manifest.json` is auto-written next to dimension files listing every custom dimension owned by the mod, plus the exact cleanup path admins should remove from the world save if the mod is uninstalled. Deleting a dimension with `purge=true` additionally writes a `_purge_<id>.marker` so the world data folder can be audited after shutdown.
- **Per-command permission nodes** — Each admin subcommand now has its own NeoForge `PermissionNode` (LuckPerms compatible): `arcadia_spawn.command.reload`, `setlobbytp`, `dellobbytp`, `edit`, `tp`, `setspawn`, `debug`, `dimension.create` (op 4), `dimension.delete` (op 4), `dimension.list`. Falls back to vanilla op-level when no permission backend is installed.
- **Strict input validation** — Lobby names and dimension ids are now matched against a regex and a Windows-reserved-name blocklist before any disk write. Descriptions are length-clamped and stripped of control characters. Prevents path-traversal, filename injection, and accidental NUL/CON/AUX-style filenames on Windows hosts.
- **Anti-spam rate limiter** — Lobby menu open requests (both `/lobby` and the C2SOpenLobby packet from the hub card) are throttled to 5 calls per 10 seconds per player. Protects against a misbehaving client spamming GUIs.
- **Transaction-safe persistence** — All JSON writes go through `SafeFileIO.writeAtomicWithBackup()`: backup of the previous file → write to `.tmp` → atomic rename. Up to 5 rotated backups are kept per file under `<dir>/backups/`. If the primary file is unreadable at startup, the latest backup is restored automatically.

### Changed

- **`LobbyManager` lookups are now O(1)** — Added a `ConcurrentHashMap` index keyed by lower-case lobby name. `getLocation()`, `addLocation()`, and `removeLocation()` no longer linear-scan the list. Tab-completion (3 commands × N lobbies) and `/arcadia_spawn tp` are faster on servers with many lobbies.
- **`DimensionRegistry` reflection cached** — The `FlatLevelGeneratorSettings` private-constructor lookup is now a `static final Constructor` initialized once, instead of being resolved and `setAccessible(true)`'d on every dimension registration. The vanilla `arcadia:spawn` `DimensionType` is also built once and reused for both the `DIMENSION_TYPE` and `LEVEL_STEM` registrations (was being constructed twice).
- **`SpawnCommands` refactor** — Lobby-name suggestions are factored into a single `SuggestionProvider` constant instead of being inlined three times. Custom-dim and preset suggestions follow the same pattern.
- **Brigadier-level permission gating** — `.requires()` predicates now use `PermissionRegistry.require(NODE, opFallback)` instead of raw `hasPermission(2)`, so commands disappear from tab-completion for unauthorized players whether they're op-gated or LuckPerms-gated.

### Fixed

- **`SpawnData` NBT robustness** — A corrupted `dimensionId` field (invalid `ResourceLocation`) used to crash teleport on first dereference. The loader now validates the parse, falls back to `arcadia:spawn`, and logs the original value. A blanket try/catch around NBT load resets to defaults instead of throwing.

### Performance

- **`TeleportHelper.tick()` short-circuits when empty** — The server tick handler iterated a `ConcurrentHashMap` every tick even when no warmups were active. A single `isEmpty()` guard at the top skips the iterator allocation on the common path (0 warmups).
- **`RTPCommand.findRandomSafePos()` zero-allocation** — Uses `BlockPos.MutableBlockPos` for the candidate cursor and ground check instead of allocating a `BlockPos` + `BlockPos.below()` per attempt. With `rtp_max_attempts=50` this saves up to 100 allocations per `/arcadiartp` call. Min/max build heights are also hoisted out of the loop.

### Security

- **Server-authoritative everywhere** — `MobSpawnHandler`'s `isClientSide()` guard (already in 1.5.1) is documented as part of a broader policy: every gameplay decision (mob filter, spawn TP, slot bypass, dimension registration) runs server-side only. Client-friendly = no client-only writes, no client-trusted state.
- **Hardened kick message handling** — The slot-bypass kick message still supports `&`-prefixed color codes but is sanitized when written through `SafeFileIO` along with the rest of the config layer.

### Backward Compatibility

- **`arcadia:spawn` is preserved exactly as-is.** Existing worlds, existing `SpawnData`, existing lobby files, existing `config/arcadia/spawn/config.toml`, existing `slot_bypass.toml` — all continue to work without migration. The custom-dimension system is opt-in: if you never run `/arcadia_spawn dimension create`, nothing changes on disk and nothing new is registered.
- All previously-valid lobby names continue to be accepted; the new validation rules are a strict subset of what was already allowed in practice (the old code would silently break on filename-illegal characters anyway).

---

### Ajouts

- **Tab list personnalisée avec tri par grade** — Quand `tablist.enabled = true`, la tab list vanilla est remplacée par un header/footer configurables rendus pour chaque joueur connecté. Header et footer acceptent des templates multi-lignes avec codes couleur `&` et placeholders (`%server%`, `%online%`, `%max%`, `%tps%`, `%mspt%`, `%uptime%`, `%player_name%`, `%player_ping%`, `%player_playtime%`, `%lp_group%`, `%lp_prefix%`, `%cross_total%`, `%peers%`). Intervalle de refresh configurable (défaut 40 ticks = 2s).
- **Tri par grade LuckPerms** — Les joueurs sont placés dans des `PlayerTeam` Minecraft nommées `as_<999-weight>_<groupe>`, donc le weight LuckPerms le plus haut apparaît toujours en haut de la tab. Les préfixes LP sont rendus devant le pseudo, et une meta `color` optionnelle recolore le pseudo via le ChatFormatting le plus proche. Écoute `UserDataRecalculateEvent` de LuckPerms pour la mise à jour instantanée des promotions/démotions.
- **Fallback sans LuckPerms** — Sans LP, les joueurs sont triés par op level vanilla : Owner (op4), Admin (op3), Mod (op2), Helper (op1), Default. Aucune dépendance externe requise pour la vue basique des grades.
- **Compteur cross-server via la base arcadia-lib** — Quand `cross_server_enabled = true` et que le `DatabaseManager` d'arcadia-lib est actif, le mod crée une table `arcadia_tablist_servers` (auto-`CREATE TABLE IF NOT EXISTS` via `TableDefinition`), heartbeat la ligne de ce serveur toutes les `heartbeat_interval_seconds` (défaut 10s), et lit les peers en asynchrone pour expander la ligne `%peers%` du footer en une ligne par serveur. Les peers obsolètes (pas de heartbeat depuis `peer_timeout_seconds`) sont affichés comme offline. Tout le JDBC passe par `DatabaseManager.executeAsync` / `supplyAsync` — le main thread serveur ne bloque jamais sur une query. Fail-soft quand la DB est down : la tab locale s'affiche quand même, juste sans les lignes peers.
- **Sous-commandes admin `/arcadia_spawn tablist`** — `reload` force un refresh immédiat pour tous les joueurs connectés. `status` affiche l'état (enabled, détection LP, dispo DB, ID de ce serveur). `peers` liste toutes les lignes actuellement présentes dans la DB partagée avec leur flag ALIVE/STALE.
- **Nettoyage au shutdown** — Au `ServerStoppingEvent`, le mod supprime sa ligne dans `arcadia_tablist_servers` pour que les autres serveurs n'affichent pas ce serveur comme alive jusqu'à expiration du timeout.

(Plus tout ce qui est listé pour 1.5.3 ci-dessous — dimensions personnalisées, nodes de permissions, validation d'entrée, rate limiting, persistance atomique, optimisations de performance.)

- **Système de dimensions personnalisées** — Nouvelles commandes `/arcadia_spawn dimension create <id> [preset] [biome]`, `/arcadia_spawn dimension delete <id> [purge]`, et `/arcadia_spawn dimension list`. Les définitions sont sérialisées dans `config/arcadia/spawn/dimensions/<id>.json` et enregistrées au démarrage du serveur sous le namespace dédié `arcadia_custom:`. Presets disponibles : `flat`, `void`, `lobby`. Un redémarrage est requis pour activer une nouvelle dimension — limitation NeoForge (les `LevelStem` ne peuvent être enregistrés qu'au `RegisterEvent`). La dimension `arcadia:spawn` d'origine n'est jamais touchée ; tout est opt-in.
- **Manifest de retrait du mod** — Un `_manifest.json` est écrit automatiquement à côté des fichiers de dimension, listant chaque dimension custom détenue par le mod, plus le chemin exact à supprimer du world save si l'admin désinstalle le mod. La suppression avec `purge=true` écrit en plus un `_purge_<id>.marker` pour auditer le dossier de données du monde après shutdown.
- **Nodes de permission par commande** — Chaque sous-commande admin a maintenant son propre `PermissionNode` NeoForge (compatible LuckPerms) : `arcadia_spawn.command.reload`, `setlobbytp`, `dellobbytp`, `edit`, `tp`, `setspawn`, `debug`, `dimension.create` (op 4), `dimension.delete` (op 4), `dimension.list`. Fallback sur op-level vanilla si pas de backend de permissions.
- **Validation stricte des entrées** — Noms de lobby et identifiants de dimension sont vérifiés via regex + blocklist Windows (CON, NUL, etc.) avant toute écriture disque. Les descriptions sont limitées en taille et débarrassées des caractères de contrôle. Empêche l'injection de filename, le path-traversal et les filenames Windows réservés.
- **Limiteur anti-spam** — Les requêtes d'ouverture du menu lobby (via `/lobby` ou le packet C2SOpenLobby depuis la carte hub) sont limitées à 5 appels par 10 secondes par joueur. Protège contre un client mal intentionné qui spammerait les GUIs.
- **Persistance transactionnelle** — Toutes les écritures JSON passent par `SafeFileIO.writeAtomicWithBackup()` : backup du fichier précédent → écriture dans `.tmp` → rename atomique. Jusqu'à 5 backups roulants sont conservés par fichier sous `<dir>/backups/`. Si le fichier principal est illisible au démarrage, le backup le plus récent est restauré automatiquement.

### Modifications

- **Recherches `LobbyManager` en O(1)** — Ajout d'un `ConcurrentHashMap` indexé par nom en lowercase. `getLocation()`, `addLocation()` et `removeLocation()` ne font plus de scan linéaire. Le tab-complete (3 commandes × N lobbies) et `/arcadia_spawn tp` deviennent plus rapides sur les serveurs avec beaucoup de lobbies.
- **Reflection mise en cache dans `DimensionRegistry`** — Le constructeur privé de `FlatLevelGeneratorSettings` est maintenant un `static final Constructor` initialisé une seule fois, au lieu d'être résolu et `setAccessible(true)`'d à chaque enregistrement de dimension. Le `DimensionType` de `arcadia:spawn` est aussi construit une seule fois et réutilisé pour les enregistrements `DIMENSION_TYPE` et `LEVEL_STEM` (il était construit deux fois).
- **Refactor de `SpawnCommands`** — Les suggestions de noms de lobby sont factorisées dans un `SuggestionProvider` unique au lieu d'être inlinées trois fois. Les suggestions dimension/preset suivent le même pattern.
- **Gating Brigadier basé permissions** — Les prédicats `.requires()` utilisent `PermissionRegistry.require(NODE, opFallback)` au lieu d'un `hasPermission(2)` brut, donc les commandes disparaissent du tab-complete pour les joueurs non autorisés, qu'ils soient gérés par op-level ou par LuckPerms.

### Correctifs

- **Robustesse NBT de `SpawnData`** — Un `dimensionId` corrompu (`ResourceLocation` invalide) faisait crasher au premier déréférencement de teleport. Le loader valide maintenant le parse, fallback sur `arcadia:spawn`, et log la valeur d'origine. Un try/catch global au chargement NBT reset les valeurs par défaut au lieu de throw.

### Performance

- **`TeleportHelper.tick()` court-circuit si vide** — Le handler `ServerTickEvent.Post` itérait un `ConcurrentHashMap` à chaque tick même sans warmup actif. Un simple `isEmpty()` en tête saute l'allocation de l'itérateur sur le chemin commun (0 warmup).
- **`RTPCommand.findRandomSafePos()` zéro-allocation** — Utilise un `BlockPos.MutableBlockPos` pour le curseur et le check sol au lieu d'allouer un `BlockPos` + `BlockPos.below()` à chaque tentative. Avec `rtp_max_attempts=50` ça économise jusqu'à 100 allocations par appel `/arcadiartp`. Les bornes min/max de la dimension sont aussi sorties de la boucle.

### Sécurité

- **Server-authoritative partout** — Le guard `isClientSide()` de `MobSpawnHandler` (déjà en 1.5.1) est documenté comme une politique générale : chaque décision gameplay (filtre mob, TP spawn, slot bypass, enregistrement de dimension) tourne côté serveur uniquement. Client-friendly = pas d'écriture client-only, pas d'état client-trusted.
- **Message de kick assaini** — Le message de kick slot-bypass supporte toujours les codes couleur préfixés `&` mais est nettoyé quand il passe par `SafeFileIO` avec le reste de la couche config.

### Compatibilité ascendante

- **`arcadia:spawn` est préservé tel quel.** Les mondes existants, le `SpawnData` existant, les fichiers de lobby existants, `config/arcadia/spawn/config.toml`, `slot_bypass.toml` — tout continue de fonctionner sans migration. Le système de dimensions custom est opt-in : si vous ne lancez jamais `/arcadia_spawn dimension create`, rien ne change sur le disque et rien de nouveau n'est enregistré.
- Tous les noms de lobby précédemment valides continuent d'être acceptés ; les nouvelles règles de validation sont un sous-ensemble strict de ce que l'ancien code acceptait en pratique (il cassait silencieusement sur les caractères illégaux dans les noms de fichier de toute façon).

---

## [1.5.2] - 2026-04-23

### Fixed

- **`/arcadiartp` callable from any dimension** — The command refused with `rtp.fail_dim` unless the player was already standing in the Overworld. That defeated the whole design point: players who land in `arcadia:spawn` on first join are supposed to hop out into the Overworld via RTP. Removed the overworld-only gate — the destination is still the Overworld (that's where RTP explores), but the command is now invokable from any dimension (arcadia:spawn, Nether, End, anywhere).

### Correctifs

- **`/arcadiartp` utilisable depuis n'importe quelle dimension** — La commande refusait avec `rtp.fail_dim` sauf si le joueur se trouvait déjà dans l'Overworld. Ça cassait tout le design d'origine : les joueurs qui arrivent dans `arcadia:spawn` au premier login sont censés rejoindre l'Overworld via RTP. Suppression du garde overworld-only — la destination reste l'Overworld (là où RTP explore), mais la commande est maintenant invocable depuis n'importe quelle dimension (arcadia:spawn, Nether, End, peu importe).

---

## [1.5.1] - 2026-04-23

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
