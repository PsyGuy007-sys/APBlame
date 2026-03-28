<p align="center">
  <img src="logo.png" alt="APBlame Logo" width="300">
</p>

<h1 align="center">APBlame</h1>

<p align="center">
  <strong>Troll & Chat Fun plugin for Paper 1.21+</strong><br>
  Randomly blames a nearby player when something happens. Zero damage, pure social chaos.
</p>

<p align="center">
  <a href="https://github.com/PsyGuy007-sys/APBlame/releases"><img src="https://img.shields.io/github/v/release/PsyGuy007-sys/APBlame?style=flat-square" alt="Release"></a>
  <img src="https://img.shields.io/badge/API-Paper%201.21+-blue?style=flat-square" alt="Paper 1.21+">
  <img src="https://img.shields.io/badge/Java-21+-orange?style=flat-square" alt="Java 21+">
  <img src="https://img.shields.io/badge/PlaceholderAPI-supported-green?style=flat-square" alt="PlaceholderAPI">
</p>

---

## What is APBlame?

Every time a player **breaks a block**, **places a block**, **kills a mob**, **dies**, or **crafts an item**, there's a small configurable chance (5-20%) that a hilarious message pops up in chat, accusing a random nearby player:

> *"It looks like JeanMichel broke your block behind your back!"*
> *"Breaking news: Sophie filed a complaint for mob cruelty!"*

It creates light paranoia and laughs in groups. Perfect for SMP servers or playing with friends — everyone's looking around like *"are YOU the scapegoat today?"*

## Features

- **5 event triggers** — Block break, block place, mob kill, death, crafting
- **Configurable chances** per event (5-20%)
- **25 pre-built messages** across 5 categories (French by default, fully customizable)
- **Nearby player detection** with configurable radius (default: 30 blocks)
- **Per-player cooldown** to prevent spam (default: 5 seconds)
- **Toggle on/off** per player (`/apblame toggle`)
- **Immunity permission** — `apblame.immune` to never be blamed
- **PlaceholderAPI** soft integration with 5 custom placeholders
- **Externalized messages** — all UI messages in `messages.yml`
- **Adventure API** — modern color code handling, no deprecated Bukkit APIs

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/apblame reload` | Reload configuration and messages | `apblame.reload` |
| `/apblame toggle` | Toggle blame messages for yourself | `apblame.toggle` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `apblame.use` | Use APBlame commands | `true` |
| `apblame.reload` | Reload configuration | `op` |
| `apblame.toggle` | Toggle blame messages | `true` |
| `apblame.immune` | Never be blamed | `false` |

## PlaceholderAPI Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%apblame_last_blamed%` | Name of the last player blamed |
| `%apblame_last_message%` | Last blame message received (plain text) |
| `%apblame_total_blamed%` | Total times blamed as scapegoat |
| `%apblame_total_received%` | Total blame messages received |
| `%apblame_enabled%` | Whether blame messages are enabled |

## Installation

1. Download the latest JAR from [Releases](https://github.com/PsyGuy007-sys/APBlame/releases)
2. Drop it in your server's `plugins/` folder
3. Restart or reload the server
4. (Optional) Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for placeholder support

## Configuration

<details>
<summary><strong>config.yml</strong> — Chances, radius, cooldown & blame messages</summary>

```yaml
# Trigger chances (in %) per event (min 5, max 20)
chances:
  block-break: 10
  block-place: 10
  mob-kill: 10
  death: 15
  craft: 8

# Detection radius for nearby players (in blocks)
radius: 30

# Cooldown between two messages for the same player (in seconds)
cooldown: 5

# Messages per category — {player} = blamed, {victim} = trigger
# Supports PlaceholderAPI placeholders if installed
messages:
  block-break:
    - "..."
  block-place:
    - "..."
  # etc.
```

</details>

<details>
<summary><strong>messages.yml</strong> — UI messages (reload, toggle, errors)</summary>

```yaml
prefix: "&8[&cAPBlame&8] &r"

reload:
  success: "&aConfiguration reloaded successfully!"
  no-permission: "&cYou don't have permission to do this."

toggle:
  enabled: "&aBlame messages enabled!"
  disabled: "&cBlame messages disabled."
```

</details>

## Building from source

```bash
git clone https://github.com/PsyGuy007-sys/APBlame.git
cd APBlame
mvn clean package
```

The JAR will be in `target/APBlame-1.0.0.jar`.

## License

MIT
