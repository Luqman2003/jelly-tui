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

## Install

### Homebrew (macOS)

```bash
brew tap Luqman2003/jelly-tui
brew install jelly-tui
```

### Build from source

Requires Java 21+.

```bash
mvn package
java -jar target/jelly-tui-1.0.0.jar
```

## Requirements

- [mpv](https://mpv.io/installation/) (for media playback)
- A running Jellyfin server

## Usage

```bash
jelly-tui
```

On first run, you'll be prompted for your Jellyfin username, password, and server URL. Credentials are saved to `~/.config/jelly-tui/config.json` for future sessions.

## Keybindings

| Key     | Action                          |
|---------|---------------------------------|
| `Enter` | Select item                     |
| `/`     | Open search                     |
| `Esc`   | Close search / go back          |
| `q`     | Quit                            |
| `...`   | Go back (in nested views)       |
