<p align="center"><a href="https://kitsunetechnologies.org/work"><img src="https://raw.githubusercontent.com/KitsuneTech1/.github/main/assets/kitsune-banner.svg" alt="Built by Kitsune Technologies" width="760"></a></p>

# SimpleAutomation

Placeable machines that mine, farm, and move items for you, with fuel and an upgrade tree.

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21-brightgreen.svg)
![Spigot](https://img.shields.io/badge/Spigot%2FPaper-API-orange.svg)
![Java](https://img.shields.io/badge/Java-21-blue.svg)

## What it does

SimpleAutomation adds five placeable machines (Quarry, Conveyor Belt, Tree Farmer, Crop Farmer, Block Miner) plus a Wrench for picking them back up. Machines run on fuel, tick on a configurable interval, and can be upgraded with vanilla materials for more speed, range, fortune, silk touch, and other effects. Machine state is saved between server restarts.

## Features

- **Quarry** - mines a configurable area and depth, upgradeable speed, area, fortune, silk touch, efficiency, and a void filter that discards junk blocks like cobblestone and dirt
- **Block Miner** - mines the block directly in front of it, with speed, fortune, silk touch, and efficiency upgrades
- **Tree Farmer** - chops and replants trees in range, with speed, range, multi-tree, and leaf decay upgrades
- **Crop Farmer** - harvests and replants mature crops in a radius, with speed and range upgrades
- **Conveyor Belt** - moves items along a line of belts
- **Wrench** - picks machines up without losing their upgrade levels
- Machine placement and state persist across restarts
- Per-machine GUI for viewing status and applying upgrades

## Commands

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/automation give <machine\|wrench>` | `/sa`, `/auto` | Gives a machine item or wrench | `simpleautomation.admin` |
| `/automation list` | | Lists all placed machines and their status | `simpleautomation.admin` |
| `/automation reload` | | Reloads config.yml | `simpleautomation.admin` |
| `/automation save` | | Forces an immediate save of all machines | `simpleautomation.admin` |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `simpleautomation.admin` | op | Admin access to `/automation` subcommands |
| `simpleautomation.use` | true | Use placed automation machines |
| `simpleautomation.craft` | true | Craft automation machines |

## Config

Key options in `config.yml`:

- `general.tick-interval` - how often machines are ticked, in ticks (default 10)
- `general.max-machines-per-chunk` - cap on machines per chunk (default 16)
- `quarry.base-speed`, `quarry.default-area`, `quarry.max-depth` - quarry mining rate and bounds
- `quarry.power-mode` - currently fuel only
- `quarry.upgrades.*` and `quarry.void-filter-blocks` - upgrade costs and the block list the void filter discards
- `conveyor.speed`, `conveyor.max-items-per-belt`, `conveyor.pickup-delay`
- `tree-farmer.*` and `crop-farmer.*` - check intervals, harvest speed, range, and their upgrade costs
- `block-miner.*` - mining speed and upgrade costs
- `wrench.preserve-upgrades` - keep upgrade levels when a machine is picked up
- `particles.enabled`, `particles.density`
- `messages.*` - all player-facing message strings, with color codes

## Install

### What you need

- A Spigot or Paper server on Minecraft 1.21. Get Paper at https://papermc.io/downloads . This plugin will not work on vanilla Minecraft or on Realms.
- Java 21. Get it at https://adoptium.net . Check what you have installed by typing `java -version` in a terminal.

### Step by step

**Option A: download the jar (easiest)**

1. Go to the [v1.0.0 release page](https://github.com/KitsuneTech1/SimpleAutomation/releases/tag/v1.0.0) and download `SimpleAutomation-1.0.0.jar`, or grab it directly from [this download link](https://github.com/KitsuneTech1/SimpleAutomation/releases/download/v1.0.0/SimpleAutomation-1.0.0.jar).

**Option B: build from source**

1. Install a Java 21 JDK from https://adoptium.net if you don't already have one.
2. Clone the repo: `git clone https://github.com/KitsuneTech1/SimpleAutomation.git`. If you don't use git, click "Code" then "Download ZIP" on the GitHub repo page and unzip it instead.
3. Open a terminal in the project folder: `cd SimpleAutomation`.
4. Run `mvn clean package`.
5. The jar comes out at `target/SimpleAutomation-1.0.0.jar`.

**Both options finish the same way:**

1. Drop the jar into your server's `plugins/` folder.
2. Restart the server. Don't use `/reload`, do a full stop and start. `config.yml` is generated on first run.
3. Check the console for errors as it starts up, then run `/automation` in game or from the console (aliases `/sa` and `/auto` also work). If it responds with plugin output instead of "Unknown command," it's working.

It worked if the server console shows no errors on startup and `/automation` gives you a real response instead of "Unknown command."

**Troubleshooting**

- `UnsupportedClassVersionError` in the console: your server is running an older Java than 21. Install Java 21 from https://adoptium.net and make sure your server startup script points at it.
- A warning about an unsupported or unknown API version, or the plugin not loading at all: your Spigot/Paper build is older than Minecraft 1.21. Update your server jar from https://papermc.io/downloads .
- `/automation` says "Unknown command": the jar isn't in `plugins/`, or the server wasn't fully restarted after you dropped it in. Double check the file landed in the right `plugins/` folder and do a full restart, not a reload.

## License

MIT. See [LICENSE](LICENSE).
