# System One MC sidecar

FastAPI planner the System One MC client calls while AI is on. **Primary brain: a local LLM** (Ollama / any OpenAI-compatible server on localhost). A thin **rule fallback** runs when the model is off, unreachable, times out, or returns invalid JSON.

Architecture: Minecraft → `POST /v1/systemone` (this sidecar) → Ollama `chat/completions` → validated action JSON. `STOP` means no override; the Java keyword planner keeps running. Any other action overrides that tick.

## Run

From this `sidecar` directory:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
# optional env — defaults target local Ollama
$env:SYSTEMONE_LLM = "ollama"
$env:SYSTEMONE_BASE_URL = "http://127.0.0.1:11434/v1"
$env:SYSTEMONE_MODEL = "llama3.2:3b"
python -m systemone_sidecar
```

On Linux/macOS: `source .venv/bin/activate` and `export SYSTEMONE_…=…`.

Listens on `http://127.0.0.1:8765`.

- `GET /health` — `planner` is `llm+rules` when Ollama is up, else `rules` (or `rules` if `SYSTEMONE_LLM=off`)
- `POST /v1/systemone` — observation JSON in, one action JSON out

In game, press `J` (or `.jev settings`) and set the **sidecar address** to `http://127.0.0.1:8765` (this FastAPI wrapper). Do **not** point the mod straight at Ollama’s `/v1` — the mod expects `/health` and `/v1/systemone`. Use **Test connection**.

## Environment

| Variable | Default | Meaning |
| --- | --- | --- |
| `SYSTEMONE_LLM` | `ollama` | `ollama`, `openai` (same HTTP shape), or `off` (rules only) |
| `SYSTEMONE_BASE_URL` | `http://127.0.0.1:11434/v1` | OpenAI-compatible base (Ollama’s `/v1`) |
| `SYSTEMONE_MODEL` | `llama3.2:3b` | Model name for chat completions |
| `SYSTEMONE_API_KEY` | `ollama` | Bearer token if the server wants one |
| `SYSTEMONE_LLM_TIMEOUT` | `2.5` | Seconds before falling back to rules |

## Local Ollama

```powershell
winget install Ollama.Ollama
ollama pull llama3.2:3b
ollama serve   # usually already running as a service after install
```

Then start this sidecar. Health should show `"planner":"llm+rules"` and `"llm":{"reachable":true,...}`.

## Actions

`STOP`, `MOVE_TO`, `LOOK_AT`, `ATTACK`, `USE_ITEM`, `PLACE_BLOCK`, `BREAK_BLOCK`, `JUMP`, `SNEAK`, `HOTBAR_SELECT`, `SAY` — same JSON shapes the Java executor already understands.

Rule fallback behaviour by `goal.type`:

- **protect** — engage nearby hostiles; else move toward `goal.target` player
- **follow** — `MOVE_TO` target (sprint if far); soft `STOP` when within ~3 blocks
- **survive** — eat / flee hostiles
- **build** — always `STOP` (Java `BuildPlan`)
- **idle** — slight `LOOK_AT` ahead
