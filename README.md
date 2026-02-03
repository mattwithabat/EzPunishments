# EzPunishments

EzPunishments is a modern punishment system for **Minecraft 1.21+ (Spigot/Paper)**.

## Features

- **Punishment types**: Ban, Temp Ban, Mute, Temp Mute, IP Ban, Kick
- **Offline support**: All punishments work on offline players (Kick requires the player to be online)
- **Custom messages**:
  - Custom ban / temp-ban screen on join
  - Custom mute message when a muted player tries to chat
- **History GUI**: `/history <player>` opens a configurable menu with all punishments
- **Database support**: SQLite, MySQL (HikariCP), MongoDB

## Requirements

- Java **21**
- Spigot/Paper **1.21+**

## Installation

1. Download/build the jar and place it in your server’s `plugins/` folder.
2. Start/restart the server.
3. Configure:
  - `plugins/EzPunishments/config.yml` (database)
  - `plugins/EzPunishments/messages.yml` (all messages)
  - `plugins/EzPunishments/menu.yml` (history GUI)
4. Reload configs anytime with: `/ezpunishments reload`

## Building

Build with Maven:

```bash
mvn clean package
```

The jar will be in `target/`.

## Configuration

### Database (`config.yml`)

Pick the backend:

- `database.type: sqlite` (default)
- `database.type: mysql`
- `database.type: mongodb`

MySQL settings are under `database.mysql.*`, MongoDB settings under `database.mongodb.*`.

### Messages (`messages.yml`)

Everything is configurable including ban screen lines, mute messages, broadcasts, and usage messages.

Placeholders commonly available:

- `{player}`, `{punisher}`, `{reason}`, `{duration}`, `{remaining}`, `{date}`, `{type}`, `{id}`, `{ip}`

### History Menu (`menu.yml`)

Configure:

- Title, rows
- Filler item
- Next/previous page buttons (slot/material/name)
- Per-punishment item style (material/name/lore)

## Commands

- **/ban `<player>` `<reason>`**
- **/tempban `<player>` `<duration>` `<reason>`**
- **/mute `<player>` `<reason>`**
- **/tempmute `<player>` `<duration>` `<reason>`**
- **/ipban `<player>` `<reason>`**
- **/kick `<player>` `<reason>`**
- **/unban `<player>`**
- **/unmute `<player>`**
- **/unipban `<ip>`**
- **/history `<player>`**
- **/ezpunishments reload**

Duration examples: `1h`, `1d`, `7d`, `30d`

## Permissions

- `ezpunishments.ban`
- `ezpunishments.tempban`
- `ezpunishments.mute`
- `ezpunishments.tempmute`
- `ezpunishments.ipban`
- `ezpunishments.kick`
- `ezpunishments.unban`
- `ezpunishments.unmute`
- `ezpunishments.unipban`
- `ezpunishments.history`
- `ezpunishments.reload`
- `ezpunishments.admin` (includes all permissions)
