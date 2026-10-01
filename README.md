# System One MC

Client-only Fabric mod for Minecraft **1.21.1**. System One MC follows keyword goals on your own client: protect, follow, build, idle, and survive. It moves by holding the normal movement keys and looking at targets. It does not teleport and it does not run on a dedicated server.

An optional Python sidecar overrides a tick with a planner action. The sidecar’s **primary brain is a local LLM** (Ollama / OpenAI-compatible on localhost) with a thin rule fallback when the model is down. `STOP` (or a failed connection) leaves the Java keyword goals running. The HUD shows **System One MC: sidecar offline (dummy)** when the HTTP service cannot be reached.

## Requirements

- Java 21 (the Gradle build uses Fabric Loom 1.17.21, which runs on Java 21)
- Minecraft 1.21.1 with the Fabric Loader
- Fabric API in your mods folder (the project builds against Fabric API `0.116.17+1.21.1`)

## Build on Windows (PowerShell)

Open PowerShell in this directory and run:

```powershell
.\gradlew.bat build
```

The mod jar is written to:

```text
build\libs\systemone-mc-1.0.0.jar
```

Install that jar. Leave `systemone-mc-1.0.0-sources.jar` out of the mods folder.

Copy the jar into your mods folder:

```powershell
Copy-Item build\libs\systemone-mc-1.0.0.jar "$env:APPDATA\.minecraft\mods\"
```

Launch a Fabric 1.21.1 profile that also has Fabric API installed. The log line `System One MC mod loaded` is written at INFO when the client starts.

Linux and macOS use `./gradlew build`. The jar path is `build/libs/systemone-mc-1.0.0.jar`, and the mods folder is the `mods` directory of the instance you launch.

## Controls

| Key | Action |
| --- | --- |
| `G` | Open the goal radial |
| `K` | Toggle AI on or off |
| `J` | Open settings |

The key category in Controls is **System One MC**.

The radial lists **protect**, **follow**, **build**, **idle**, and **survive**.

- **Protect** and **follow** open a list of players in the same world within 48 blocks. Clicking a name starts that goal immediately.
- **Build**, **idle**, and **survive** start immediately.
- **Build** queues a 5×5×3 dirt hut in front of you (floor, walls with a door, roof outline). Dirt has to be on the hotbar. If the hotbar has no dirt, the goal waits and says so.
- The center **Settings** button opens the sidecar address screen.

AI has to be on (`K`) before the goal tick runs. The tick is every 2 client ticks, about 10 Hz.

## Chat

Lines that start with `.jev ` (any case) are handled on the client and are not sent to the server. `.s1 ` is the same parser.

```text
.jev protect Steve
.jev follow
.jev follow Steve
.jev build
.jev idle
.jev survive
.jev stop
.jev ai on
.jev ai off
.jev settings
.jev help
```

The same commands work with `.s1 ` in place of `.jev ` (for example `.s1 protect Steve`).

`follow` and `protect` with no name use the nearest other player in range.

## Sidecar

Default address: `http://127.0.0.1:8765`. Change it in the settings screen and use **Test connection**. An empty address or a failed test uses the built-in dummy, which is `STOP`.

From `sidecar/`:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m systemone_sidecar
```

`POST /v1/systemone` accepts observation JSON and returns one action. The sidecar asks local Ollama (default `http://127.0.0.1:11434/v1`) first; rules run if the LLM fails. Point the in-game settings address at the **sidecar** (`http://127.0.0.1:8765`), not at Ollama directly. See `sidecar/README.md` for env vars and action shapes.

While the service is down, keyword goals still run locally and the HUD shows `System One MC: sidecar offline (dummy)`.

## Layout

- `src/client/java/com/zolanerd/systemone_mc/client` — client initializer, keybinds, radial menu, goals, observation JSON, action executor, HUD
- `src/main/java/com/zolanerd/systemone_mc` — mod id constants (`SystemOneMc`) and `fabric.mod.json` (`environment` is `client`, id `systemone_mc`)
- `src/main/resources/assets/systemone_mc` — keybind translations (`category.systemone_mc`, `key.systemone_mc.*`)
- `sidecar/systemone_sidecar` — FastAPI app (`python -m systemone_sidecar`)
