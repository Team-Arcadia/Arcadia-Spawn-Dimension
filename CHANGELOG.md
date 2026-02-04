# CHANGELOG / JOURNAL DES MODIFICATIONS

## [1.4.2] - 2026-02-04

### 🇺🇸 English
#### Added
- **GitHub Workflow**: Automatically builds and uploads the mod JAR as a GitHub Action artifact.

#### Fixed
- **Repository Cleanup**: Removed unnecessary files (`src_backup`, `.vscode`, build artifacts) and updated `.gitignore`.

---

### 🇫🇷 Français
#### Ajouté
- **Workflow GitHub** : Compilation et mise à disposition automatique du JAR du mod en tant qu'artefact GitHub Action.

#### Fixé
- **Nettoyage du Dépôt** : Suppression des fichiers inutiles (`src_backup`, `.vscode`, artefacts de build) et mise à jour du `.gitignore`.

______________________________________________________________________

## [1.4.1] - 2026-02-03

### 🇺🇸 English
#### Added
- **Command Structure**: Consolidated lobby commands under `/arcadialobby` for better organization.
- **New Command**: `/arcadialobby edit <name> [description|item|location]` to modify existing lobby warps.
- **New Command**: `/arcadialobby tp <name>` to teleport directly to a lobby warp without the menu.
- **Optimization**: Switched `LobbyManager` to `CopyOnWriteArrayList` for better thread safety and performance.

#### Changed
- **Moved**: `/setlobbytp` is now `/arcadialobby setlobbytp`.
- **Moved**: `/dellobbytp` is now `/arcadialobby dellobbytp`.

#### Fixed
- **Stability**: improved error handling to prevent data corruption.

---

### 🇫🇷 Français
#### Ajouté
- **Structure des Commandes** : Regroupement des commandes de lobby sous `/arcadialobby` pour une meilleure organisation.
- **Nouvelle Commande** : `/arcadialobby edit <nom> [description|item|location]` pour modifier les warps existants.
- **Nouvelle Commande** : `/arcadialobby tp <nom>` pour se téléporter directement à un warp sans passer par le menu.
- **Optimisation** : Passage de `LobbyManager` à `CopyOnWriteArrayList` pour une meilleure sécurité des threads et performances.

#### Changé
- **Déplacé** : `/setlobbytp` devient `/arcadialobby setlobbytp`.
- **Déplacé** : `/dellobbytp` devient `/arcadialobby dellobbytp`.

#### Corrigé
- **Stabilité** : Amélioration de la gestion des erreurs pour empêcher la corruption des données.

______________________________________________________________________

## [1.4.0] - 2026-01-23

### 🇺🇸 English
#### Added
- **RTP Command**: New `/arcadiartp` command allowing random teleportation TO the Overworld (from anywhere).
- **RTP Config**:
    - `rtpRadius`: Configurable max radius (default 50,000 blocks).
    - `rtpMaxUsage`: Configurable use limit per player (default 1).
- **RTP Fallback**: If limit is reached, command teleports player to their last valid RTP destination.
- **Persistence**: Player RTP usage and locations are saved securely using NeoForge Data Attachments.

---

### 🇫🇷 Français
#### Ajouté
- **Commande RTP** : Nouvelle commande `/arcadiartp` permettant la téléportation aléatoire VERS l'Overworld (depuis n'importe où).
- **Config RTP** :
    - `rtpRadius` : Rayon max configurable (défaut 50 000 blocs).
    - `rtpMaxUsage` : Limite d'utilisation par joueur configurable (défaut 1).
- **RTP Fallback** : Si la limite est atteinte, la commande téléporte le joueur à sa dernière destination RTP valide.
- **Persistance** : L'utilisation et les lieux RTP des joueurs sont sauvegardés via NeoForge Data Attachments.

______________________________________________________________________

## [1.3.0] - 2026-01-16

### 🇺🇸 English
#### Added
- **Full Configuration System**: New `config.toml` to control all dimension aspects.
- **World Generation Config**:
    - **Biome**: Customizable default biome (e.g., `minecraft:the_void`).
    - **Layers**: Customizable flat world layers.
- **Environment Config**:
    - **Time**: Lock time or allow cycle.
    - **Weather**: Toggle rain/thunder.
- **Mob Spawning**: Fine-grained control per category (Monsters, Creatures, etc.).
- **Safety Options**: Piglin zombification, bed explosions, respawn anchors, and raids toggles.

#### Changed
- **Dimension Generation**: Now fully dynamic based on configuration. Static JSON files have been removed.
- **Mob Spawning**: Switched to **strict blocking** for safety. If disabled, blocks EVERYTHING (Natural/Spawner/Egg).

#### Fixed
- **Terrain Generation**: Fixed "void world" bug by using clean Reflection for instantiation.
- **Compilation**: Stabilized for NeoForge 1.21.1.

---

### 🇫🇷 Français
#### Ajouté
- **Système de Configuration Complet** : Nouveau `config.toml` pour contrôler tous les aspects de la dimension.
- **Config Génération Monde** :
    - **Biome** : Biome par défaut personnalisable (ex : `minecraft:the_void`).
    - **Couches** : Couches du monde plat personnalisables.
- **Config Environnement** :
    - **Temps** : Bloquer le temps ou laisser le cycle.
    - **Météo** : Activer/Désactiver pluie/orage.
- **Apparition des Mobs** : Contrôle fin par catégorie (Monstres, Créatures, etc.).
- **Options de Sécurité** : Zombification des Piglins, explosions de lits, ancres de réapparition et raids désactivables.

#### Changé
- **Génération de Dimension** : Désormais entièrement dynamique basée sur la configuration. Les fichiers JSON statiques ont été supprimés.
- **Spawn des Mobs** : Passage à un **blocage strict** pour garantir la sécurité. Si désactivé, bloque tout (Naturel/Spawner/Oeuf).

#### Corrigé
- **Génération Terrain** : Correction du bug de "monde vide" (Void) en utilisant une instanciation propre via Réflexion.
- **Compilation** : Stabilisation pour NeoForge 1.21.1.

______________________________________________________________________

## [1.2.0] - 2026-01-14

### 🇺🇸 English
#### Added
- **Localization Support**: Native support for English (en_us) and French (fr_fr). The mod now automatically adapts to the client's language.
- **Custom Icons**: You can now define a specific item for each lobby warp point using `/setlobbytp <name> [item] [description]`.
    - Example: `/setlobbytp pvp minecraft:diamond_sword` will display a Diamond Sword in the menu.

#### Changed
- **Lobby Menu**: Now utilizes translatable components, ensuring correct text display for all users.
- **Config**: The `LobbyLocation` JSON structure now includes an optional `item` field.

#### Fixed
- **Typo**: Corrected "ArcadiA" to "Lobby Arcadia" in the menu title.

---

### 🇫🇷 Français
#### Ajouté
- **Système de Langue** : Support natif du Français (fr_fr) et de l'Anglais (en_us). Le mod s'adapte automatiquement à la langue du client.
- **Icônes Personnalisées** : Possibilité de définir un item spécifique pour chaque point de warp via `/setlobbytp <name> [item] [description]`.
    - Exemple : `/setlobbytp pvp minecraft:diamond_sword` affichera une Épée en Diamant dans le menu.

#### Changé
- **Menu Lobby** : Utilise maintenant des composants traduisibles pour un affichage correct du texte.
- **Config** : La structure JSON de `LobbyLocation` inclut désormais un champ optionnel `item`.

#### Corrigé
- **Coquille** : Correction de "ArcadiA" en "Lobby Arcadia" dans le titre du menu.

______________________________________________________________________

## [1.1.0] - 2026-01-12

### 🇺🇸 English
#### Added
- **Dynamic Lobby Menu**: New GUI accessible via `/lobby` displaying configurable teleport points.
- **Commands**:
    - `/setlobbytp <name> [description]`: Define a new warp point at your location.
    - `/dellobbytp <name>`: Remove an existing warp point.
    - `/arcadialobby reload`: Reload configuration without restarting the server.
- **Configuration**: Teleport points are now stored in JSON files located at `config/arcadia/arcadialobbyspawn/`.

### 🇫🇷 Français
#### Ajouté
- **Menu Lobby Dynamique** : Nouvelle interface accessible via `/lobby` affichant des points de téléportation configurables.
- **Commandes** :
    - `/setlobbytp <nom> [description]` : Définit un nouveau point de warp à votre position.
    - `/dellobbytp <nom>` : Supprime un point de warp existant.
    - `/arcadialobby reload` : Recharge la configuration sans redémarrer le serveur.
- **Configuration** : Les points de téléportation sont maintenant stockés dans des fichiers JSON situés dans `config/arcadia/arcadialobbyspawn/`.

______________________________________________________________________

## [1.0.0] - Initial Release

### 🇺🇸 English
- **Spawn System**: Basic `/spawn` command to teleport players to a predefined global spawn point.
- **Safety**: Prevents fall damage upon teleportation.

### 🇫🇷 Français
- **Système de Spawn** : Commande de base `/spawn` pour téléporter les joueurs vers un point de spawn global prédéfini.
- **Sécurité** : Empêche les dégâts de chute lors de la téléportation.
