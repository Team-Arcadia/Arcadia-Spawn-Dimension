# Arcadia Spawn

![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-red) ![Version](https://img.shields.io/badge/version-1.4.3-blue)

**[EN]** Advanced spawn management and dynamic lobby menu for the Arcadia server.
**[FR]** Gestion avancée du spawn et menu lobby dynamique pour le serveur Arcadia.

---

## 🇺🇸 English (US)

### Configuration (v1.3.0+)
The dimension is fully configurable via `config/arcadia/arcadialobbyspawn/config.toml`.
- **World**: Biome, Layers, Time Locking, Weather.
- **Rules**: Mob Spawning, Player Safety (Piglins, Beds, Raids).

> [!CAUTION]
> **Important - Mob Spawning (v1.3.0+)**
> If you set `spawn_monsters = false`, **ALL** monsters will be blocked (including Spawners, Eggs, Commands).
> To allow Spawners but block natural spawns:
> 1. Set `spawn_monsters = true` in config.
> 2. Ensure `biome` is set to `"minecraft:the_void"` (default) which naturally prevents mob spawning.

### Features
- **Dynamic Lobby Menu**: Access a GUI via `/lobby` to teleport to defined servers/dimensions.
- **Custom Icons**: Use any Minecraft item as an icon for your warp points.
- **Bilingual**: Fully translated in English and French.
- **Spawn Management**: Secure spawn system preventing void falls.

### Commands
- `/lobby`: Opens the lobby menu.
- `/spawn`: Teleports to the defined global spawn.
- `/arcadialobby setlobbytp <name> [item] [description]` *(Admin)*: Sets a warp point at your location.
    - `item`: (Optional) The item ID to show (e.g., `minecraft:diamond`). Defaults to paper.
    - `description`: (Optional) Text shown in the tooltip.
- `/arcadialobby dellobbytp <name>` *(Admin)*: Removes a warp point.
- `/arcadialobby edit <name> [description|item|location]` *(Admin)*: Modify an existing warp.
- `/arcadialobby tp <name>` *(Admin)*: Teleport directly to a warp.
- `/setlobbyspawn` *(Admin)*: Sets the global spawn point.
- `/arcadialobby reload` *(Admin)*: Reloads configuration from disk.
- `/arcadiartp`: Random teleport in Overworld (Configurable limit & radius).

### Slot Bypass System
Allows specific permission holders to join even when the server is full.
- **Config file**: `config/arcadia/arcadialobbyspawn/slot_bypass.toml`
  - `enabled`: Enable/disable the system.
  - `max_slots`: Maximum player count before bypass is required.
  - `kick_message`: Custom kick message (supports `&` color codes).
- **Permission node**: `arcadia_spawn.slots.bypass`
  - Assign this permission in **LuckPerms** to any group or player.
  - Uses NeoForge PermissionAPI (native LuckPerms compatibility).

### Tweaks (Fake Slots & Chat Messages)
The mod provides tweaks configurable via `config/arcadia/arcadialobbyspawn/slot_bypass.toml`.
- `fake_max_slots_enabled`: When enabled, the server list will display the `max_slots` value as the maximum capacity rather than the real server capacity, helping to prevent connection spam when players think there's room.
- `hide_join_leave_messages`: When enabled, hides the default vanilla "Player joined the game" and "Player left the game" chat messages.

---

## 🇫🇷 Français (FR)

### Configuration (v1.3.0+)
La dimension est entièrement configurable via `config/arcadia/arcadialobbyspawn/config.toml`.
- **Monde** : Biome, Couches, Blocage du temps, Météo.
- **Règles** : Apparition des mobs, Sécurité du joueur (Piglins, Lits, Raids).

> [!CAUTION]
> **Important - Mob Spawning (v1.3.0+)**
> Si vous réglez `spawn_monsters = false`, **TOUS** les montres seront bloqués (y compris via Spawner, Oeuf, Commande).
> Pour autoriser les Spawners tout en bloquant le spawn naturel :
> 1. Mettez `spawn_monsters = true` dans la config.
> 2. Assurez-vous que `biome` est réglé sur `"minecraft:the_void"` (par défaut) qui empêche naturellement le spawn des mobs.

### Fonctionnalités
- **Menu Lobby Dynamique** : Accédez à un menu via `/lobby` pour vous téléporter vers différents points.
- **Icônes Personnalisées** : Utilisez n'importe quel item Minecraft comme icône pour vos points de warp.
- **Bilingue** : Entièrement traduit en Français et Anglais.
- **Gestion du Spawn** : Système de spawn sécurisé évitant les chutes dans le vide.

### Commandes
- `/lobby` : Ouvre le menu lobby.
- `/spawn` : Téléporte au spawn global défini.
- `/arcadialobby setlobbytp <nom> [item] [description]` *(Admin)* : Crée un point de warp à votre position.
    - `item` : (Optionnel) L'ID de l'item à afficher (ex: `minecraft:diamond`). Par défaut papier.
    - `description` : (Optionnel) Texte affiché dans l'infobulle.
- `/arcadialobby dellobbytp <nom>` *(Admin)* : Supprime un point de warp.
- `/arcadialobby edit <nom> [description|item|location]` *(Admin)* : Modifie un warp existant.
- `/arcadialobby tp <nom>` *(Admin)* : Se téléporter directement à un warp.
- `/setlobbyspawn` *(Admin)* : Définit le point de spawn global.
- `/arcadialobby reload` *(Admin)* : Recharge la configuration.
- `/arcadiartp` : Téléportation aléatoire dans l'Overworld (Limite & rayon configurables).

### Système de Bypass de Slots
Permet à certains joueurs de se connecter même quand le serveur est plein.
- **Fichier de config** : `config/arcadia/arcadialobbyspawn/slot_bypass.toml`
  - `enabled` : Activer/désactiver le système.
  - `max_slots` : Nombre max de joueurs avant activation du bypass.
  - `kick_message` : Message de kick personnalisé (supporte les codes couleur `&`).
- **Permission** : `arcadia_spawn.slots.bypass`
  - Assignez cette permission dans **LuckPerms** à n'importe quel groupe ou joueur.
  - Utilise le PermissionAPI NeoForge (compatibilité native avec LuckPerms).

### Tweaks (Faux Slots & Messages Chat)
Le mod fournit des ajustements configurables via `config/arcadia/arcadialobbyspawn/slot_bypass.toml`.
- `fake_max_slots_enabled` : Si activé, la liste des serveurs affichera la valeur de `max_slots` comme capacité maximale plutôt que la vraie capacité du serveur, évitant que les joueurs spamment la connexion pensant qu'il reste de la place.
- `hide_join_leave_messages` : Si activé, masque les messages Vanilla par défaut "Joueur a rejoint la partie" et "Joueur a quitté la partie" dans le tchat.

---

### Author / Auteur
**@author vyrriox**

### Links / Liens
- **Website**: [Arcadia: Echoes Of Power](https://arcadia-echoes-of-power.fr/)
- **Support**: [Discord](https://discord.gg/xjF8Rtzyd4)
- **Donation**: [Stripe](https://buy.stripe.com/3cI3co6X97Vy4IK50QfIs00)
