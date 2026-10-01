# Local AI for Laya

Laya (System One MC) is singleplayer-first. The Minecraft client talks to the Python sidecar, and the sidecar talks to a local model. The default model is **`llama3.2:3b`** through Ollama.

That model is small, and it is fast enough to sit next to the ~10 Hz goal tick. Keyword rules cover the slow first load, so a cold model does not have to answer before Laya can protect, follow, fight, or dig.

In-game settings must point at the sidecar (`http://127.0.0.1:8765`), not at Ollama.

## Windows PowerShell

### 1. Install Ollama

```powershell
winget install Ollama.Ollama
```

Or download the installer from [https://ollama.com](https://ollama.com).

Ollama usually runs as a service after install. If `ollama` is not on PATH yet, open a new PowerShell window.

### 2. Pull the default model

```powershell
ollama pull llama3.2:3b
```

### 3. Point the sidecar at Ollama

In the shell you will use to start the sidecar:

```powershell
$env:SYSTEMONE_LLM = "ollama"
$env:SYSTEMONE_BASE_URL = "http://127.0.0.1:11434/v1"
$env:SYSTEMONE_MODEL = "llama3.2:3b"
```

These match the sidecar defaults. Set them in that shell so the model choice is explicit.

### 4. Start the sidecar

From the `sidecar` directory:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m systemone_sidecar
```

The process listens on `http://127.0.0.1:8765`. `GET /health` should report `"planner":"llm+rules"` once Ollama is up.

### 5. Point Minecraft at the sidecar

In game, press `J` to open Laya settings. Set the address to:

```text
http://127.0.0.1:8765
```

Use **Test connection**. Do not enter Ollama’s `http://127.0.0.1:11434` — the mod calls `/health` and `/v1/systemone` on the sidecar.

Turn AI on with `K`. Goals from the radial, or from `.laya `, `.jev `, and `.s1 `, run on the client. The sidecar may override a tick when the model returns an action other than `STOP`.

## Advanced

The default stays **`llama3.2:3b`**.

- Other Ollama models: `ollama pull <name>`, then `$env:SYSTEMONE_MODEL = "<name>"` before starting the sidecar.
- Any OpenAI-compatible server on localhost: set `SYSTEMONE_LLM` to `openai`, `SYSTEMONE_BASE_URL` to that server’s `/v1` URL, and `SYSTEMONE_MODEL` to its model id. Keep the in-game address on the sidecar.
- Rules only, no model: `$env:SYSTEMONE_LLM = "off"`.
- Hosted Laya or jevos endpoints can come later. This setup stays on localhost.

Linux and macOS use `source .venv/bin/activate` and `export SYSTEMONE_MODEL=llama3.2:3b` with the same base URL.
