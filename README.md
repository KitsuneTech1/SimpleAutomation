<p align="center"><a href="https://kitsunetechnologies.org/work"><img src="https://raw.githubusercontent.com/KitsuneTech1/.github/main/assets/kitsune-banner.svg" alt="Built by Kitsune Technologies" width="760"></a></p>

# Blockworks

A Paper plugin that adds placeable, upgradeable automation machines including quarries, farms, and utility blocks.

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-brightgreen.svg)
![Spigot](https://img.shields.io/badge/Spigot%2FPaper-API-orange.svg)
![Java](https://img.shields.io/badge/Java-25-blue.svg)

## What it does

Blockworks adds five placeable machines: Quarry, Block Miner, Tree Farmer, Crop Farmer, and Conveyor Belt. Each runs on fuel and can be upgraded with vanilla materials for speed, range, fortune, silk touch, and other effects. Machines pull fuel and send output to adjacent inventories. Conveyor Belts move items along a line and can extract from nearby storage.

All machine placement and state persists across server restarts.

## Machines

- **Quarry** - mines a configurable area and depth with speed, area, fortune, silk touch, and efficiency upgrades. Includes a void filter to discard junk blocks.
- **Block Miner** - mines the block in front of it. Upgradeable for speed, fortune, silk touch, and efficiency.
- **Tree Farmer** - chops and replants trees in range. Upgradeable for speed, range, multi-tree, and leaf decay.
- **Crop Farmer** - harvests and replants mature crops in a radius. Upgradeable for speed and range.
- **Conveyor Belt** - moves items along a line. Can extract from adjacent inventories.
- **Wrench** - picks machines up without losing upgrades.

Each machine has a GUI to view status and apply upgrades.

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

## Quick start

You need:
- A Paper server running Minecraft 26.1.2 (https://papermc.io/downloads). Does not work on vanilla Minecraft or Realms.
- Java 25 JDK (https://adoptium.net). Check with `java -version`.

Then choose:

**Option A: Download a prebuilt jar**
1. Go to the [releases page](https://github.com/KitsuneTech1/SimpleAutomation/releases) and download the jar.
2. Drop it into your server's `plugins/` folder.
3. Restart the server (full restart, not `/reload`).
4. Run `/automation` in-game or console. If it responds, you're done.

**Option B: Build from source**
1. Clone the repo: `git clone https://github.com/KitsuneTech1/SimpleAutomation.git` (or download and unzip the ZIP from GitHub).
2. `cd SimpleAutomation`
3. `mvn -q package`
4. Copy `target/SimpleAutomation-1.0.0.jar` to your server's `plugins/` folder.
5. Restart the server and run `/automation` to confirm.

The server will generate `config.yml` on first startup.

**Troubleshooting**

- `UnsupportedClassVersionError`: Your server is running an older Java. Install Java 25 from https://adoptium.net and update your startup script.
- Plugin doesn't load or API version warning: Your Paper build is too old. Update from https://papermc.io/downloads.
- `/automation` says "Unknown command": The jar isn't in `plugins/` or the server wasn't fully restarted. Check the file is in the right folder and do a full restart.

## License

MIT. Copyright (c) 2026 Kitsune Technologies LLC. Do what you like with it, commercially or otherwise, just keep the copyright notice. Full text in [LICENSE](LICENSE).

---

<p align="center"><sub>Part of <b>Buildtober 2026</b>: 31 open source projects in 31 days. Built by <a href="https://kitsunetechnologies.org/work">Kitsune Technologies</a>. More at <a href="https://github.com/KitsuneTech1">github.com/KitsuneTech1</a>.</sub></p>
