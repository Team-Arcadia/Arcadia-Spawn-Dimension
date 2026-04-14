# Contributing to Arcadia Spawn / Contribuer

Thank you for your interest in contributing! | Merci de votre interet !

## Getting Started / Pour commencer

### Prerequisites / Prerequis
- Java 21 (Temurin recommended)
- Gradle 8.7+
- NeoForge MDK knowledge
- [Arcadia Lib](https://github.com/Team-Arcadia) source (for API reference)

### Setup / Installation
```bash
git clone https://github.com/Team-Arcadia/Arcadia-Spawn-Dimension.git
cd Arcadia-Spawn-Dimension
./gradlew build
```

Place `arcadia-lib-1.2.0.jar` in the `libs/` folder before building.

## Code Conventions / Conventions de code

### Language
- **Code, variables, logs**: English only
- **Comments**: English, minimal
- **UI text**: Must use `LocalizationManager` with both EN and FR translations

### Style
- **Naming**: `PascalCase` for classes, `camelCase` for methods/fields
- **Indentation**: 4 spaces
- **Max line length**: 120 characters (soft limit)

### Architecture Rules
- Use `ArcadiaMessages` from Arcadia Lib for all chat messages
- Use `TeleportHelper` for all teleportation (fires NeoForge events for FTB /back)
- Use `CooldownManager` from Arcadia Lib for cooldown tracking
- All config options in `SpawnConfig.java` or `SlotBypassConfig.java`
- Never use `player.teleportTo()` directly — always go through TeleportHelper
- LuckPerms meta access must be isolated in separate classes (prevent NoClassDefFoundError)

### Commit Messages
Follow [Conventional Commits](https://www.conventionalcommits.org/):
```
feat: add new feature
fix: resolve bug
refactor: restructure code
docs: update documentation
perf: improve performance
```

## Branch Strategy / Strategie de branches

```
main          Production-ready releases (protected)
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

### Workflow
1. Create `feat/my-feature` from `develop`
2. Work and commit on your feature branch
3. PR into `develop` (code review)
4. When ready for testing: merge `develop` into `staging`
5. After QA passes: merge `staging` into `main` + tag release

## Pull Request Process / Processus de PR

1. Fork the repository / Forkez le repo
2. Create a feature branch from `develop` / Creez une branche depuis `develop`: `git checkout -b feat/my-feature develop`
3. Make your changes / Faites vos modifications
4. Ensure `./gradlew build` passes / Verifiez que le build passe
5. Submit a PR against `develop` / Soumettez une PR vers `develop`

## Reporting Issues / Signaler des problemes

Use the [issue templates](https://github.com/Team-Arcadia/Arcadia-Spawn-Dimension/issues/new/choose) for bug reports and feature requests.

## Community / Communaute

- [Discord](https://discord.gg/xjF8Rtzyd4) - Best place for questions and discussion
- [Website](https://arcadia-echoes-of-power.fr/)

---

# Contribuer (Francais)

Merci de votre interet pour la contribution ! Suivez les memes regles que ci-dessus. L'essentiel :

- Le code doit etre en **anglais**
- Les textes UI doivent supporter **EN + FR** via `LocalizationManager`
- Toutes les teleportations doivent passer par `TeleportHelper`
- Le build doit passer (`./gradlew build`)
- Utilisez les templates de PR et d'issues
