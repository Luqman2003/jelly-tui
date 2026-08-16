# jelly-tui

A lightweight terminal UI for browsing and playing media from a [Jellyfin](https://jellyfin.org/) server. Inspired by [ani-cli](https://github.com/pystardust/ani-cli).

## Features

- Browse your Jellyfin libraries, series, and seasons from the terminal
- Play episodes directly via [mpv](https://mpv.io/)
- Fuzzy search with `/` to quickly find media
- Auto-play next episode after playback ends
- Keyboard-driven navigation with a help bar showing available keybindings
- Session persistence — credentials are saved locally so you only log in once
- Password input is masked during login

## Requirements

- Java 21+
- [mpv](https://mpv.io/installation/) (for media playback)
- A running Jellyfin server

## Build

```bash
mvn package
```

This produces a shaded JAR at `target/jelly-tui-1.0.0.jar`.

## Usage

```bash
java -jar target/jelly-tui-1.0.0.jar
```

On first run, you'll be prompted for your Jellyfin username, password, and server URL. Credentials are saved to `~/.config/jelly-cli/config.json` for future sessions.

## Keybindings

| Key     | Action                          |
|---------|---------------------------------|
| `Enter` | Select item                     |
| `/`     | Open search                     |
| `Esc`   | Close search / go back          |
| `q`     | Quit                            |
| `...`   | Go back (in nested views)       |
