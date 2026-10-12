# Epitech All-In-One for CLion

CLion port of the *Epitech All-In-One* VS Code extension, with the Banana coding style checker built into the IDE.

## Features

- **Header generation** (`Ctrl+Shift+H`, also in *Code > Generate*): inserts or replaces the Epitech header for C/C++, Makefile, Python, shell, JS/TS, Java... Optional `Author:` line.
- **Banana in the editor**: `.c`/`.h` files are checked by epiclang while you type (unsaved content included). Issues are underlined, shown in the gutter and in the scrollbar, colored by severity (Major / Minor / Info).
- **Banana report** (*Epitech* tool window):
  - `Ctrl+Shift+L`: check the current file
  - `Ctrl+Alt+Shift+L`: check every `.c`/`.h` of the project
  - `Ctrl+Alt+Shift+B`: rebuild with `make -B CC=epiclang` and collect Banana issues
  - double-click / Enter on an issue to jump to it, *Output* tab for the raw epiclang output
- **Coding style guide** (`Ctrl+Shift+S`): every rule, with examples, in the *Coding Style* tab.
- **Settings** in *Settings > Tools > Epitech*: author, email, epiclang path, live check, include directories.

All actions are also in *Tools > Epitech*. Shortcuts can be changed in *Settings > Keymap* (search "Epitech").

## Requirements

- CLion 2026.2 or newer
- [epiclang](https://github.com/Epitech/epiclang) available in `PATH` (or configured in the settings)

## Build

The build compiles against a local CLion installation (default: `/snap/clion/current`, see `gradle.properties`).

```sh
./gradlew buildPlugin   # -> build/distributions/epitech-all-in-one-<version>.zip
./gradlew test
```

Install the zip with *Settings > Plugins > ⚙ > Install Plugin from Disk...*.
