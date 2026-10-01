# Laya (System One MC)

Client-only Fabric mod for Minecraft **1.21.1**. Singleplayer first: Laya follows keyword goals on your own client. It moves by holding the normal movement keys and looking at targets. It does not teleport and it does not run on a dedicated server.

Goals: **protect**, **follow**, **build**, **idle**, **survive**, **fight**, and **dig**.

An optional Python sidecar overrides a tick with a planner action. The sidecar’s **primary brain is a local LLM** (default model **`llama3.2:3b`** via Ollama) with a thin rule fallback when the model is down. `STOP` (or a failed connection) leaves the Java keyword goals running. The HUD shows **Laya: sidecar offline (dummy)** when the HTTP service cannot be reached.

Point in-game settings at the sidecar (`http://127.0.0.1:8765`), not at Ollama. Full setup: [sidecar/LOCAL_AI.md](sidecar/LOCAL_AI.md).

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

Launch a Fabric 1.21.1 profile that also has Fabric API installed. The log line `Laya mod loaded` is written at INFO when the client starts.

Linux and macOS use `./gradlew build`. The jar path is `build/libs/systemone-mc-1.0.0.jar`, and the mods folder is the `mods` directory of the instance you launch.

## Controls

| Key | Action |
| --- | --- |
| `G` | Open the goal radial |
| `K` | Toggle AI on or off |
| `J` | Open settings |

The key category in Controls is **Laya**.

The radial (**Laya goals**) lists **protect**, **follow**, **build**, **idle**, **survive**, **fight**, and **dig**.

- **Protect**, **follow**, and **fight** open a list of players in the same world within 48 blocks. Clicking a name starts that goal immediately. Fight's heading is "Fight alongside"; picking a player says "Fighting alongside NAME."
- **Build**, **idle**, **survive**, and **dig** start immediately.
- **Build** queues a 5×5×3 dirt hut in front of you (floor, walls with a door, roof outline). Dirt has to be on the hotbar. If the hotbar has no dirt, the goal waits and says so.
- **Dig** breaks a forward 1×2 tunnel (feet and head) for 8 blocks along your look direction. A pickaxe or shovel must be on the hotbar.
- **Fight** attacks hostiles near your ally and near you, and stays with that ally. With no other players it still fights nearby hostiles. If there is nobody to fight with and no hostiles, it says so once.
- The center **Settings** button opens the sidecar address screen.

AI has to be on (`K`) before the goal tick runs. The tick is every 2 client ticks, about 10 Hz.

## Chat

Lines that start with `.jev ` (any case) are handled on the client and are not sent to the server. `.s1 ` and `.laya ` are the same parser.

```text
.jev protect Steve
.jev follow
.jev follow Steve
.jev fight
.jev fight Steve
.jev build
.jev dig
.jev idle
.jev survive
.jev stop
.jev ai on
.jev ai off
.jev settings
.jev help
```

The same commands work with `.s1 ` or `.laya ` in place of `.jev ` (for example `.laya fight Steve` or `.s1 dig`).

`follow`, `protect`, and `fight` with no name use the nearest other player in range. Fight still attacks nearby hostiles when no other player is loaded.

## Local AI

Default model: **`llama3.2:3b`** (Ollama). It is small and fast enough for the ~10 Hz goal tick. Rules cover the slow first model load.

Full Windows PowerShell setup: [sidecar/LOCAL_AI.md](sidecar/LOCAL_AI.md).

In settings (`J`), set the sidecar address to `http://127.0.0.1:8765` and use **Test connection**. Do not point the mod at Ollama’s port (`11434`). The mod expects the sidecar’s `/health` and `/v1/systemone`.

## Sidecar

Default address: `http://127.0.0.1:8765`. Change it in the settings screen and use **Test connection**. An empty address or a failed test uses the built-in dummy, which is `STOP`.

From `sidecar/`:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m systemone_sidecar
```

`POST /v1/systemone` accepts observation JSON and returns one action. The sidecar asks local Ollama (default model `llama3.2:3b` at `http://127.0.0.1:11434/v1`) first; rules run if the LLM fails. Point the in-game settings address at the **sidecar** (`http://127.0.0.1:8765`), not at Ollama directly. See [sidecar/README.md](sidecar/README.md) for env vars and [sidecar/LOCAL_AI.md](sidecar/LOCAL_AI.md) for the Windows model setup.

While the service is down, keyword goals still run locally and the HUD shows `Laya: sidecar offline (dummy)`.

## Layout

- `src/client/java/com/zolanerd/systemone_mc/client` — client initializer, keybinds, radial menu, goals, observation JSON, action executor, HUD
- `src/main/java/com/zolanerd/systemone_mc` — mod id constants (`SystemOneMc`) and `fabric.mod.json` (`environment` is `client`, id `systemone_mc`)
- `src/main/resources/assets/systemone_mc` — keybind translations (`category.systemone_mc`, `key.systemone_mc.*`)
- `sidecar/systemone_sidecar` — FastAPI app (`python -m systemone_sidecar`)
