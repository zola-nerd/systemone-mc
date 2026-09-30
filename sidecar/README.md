# Jev sidecar

Small HTTP planner that the Jev client calls while AI is on. This copy is a dummy: `POST /v1/systemone` always returns `{"action":"STOP"}`.

The Java mod matches keyword goals first (protect, follow, build, idle, survive). `STOP` means the sidecar is not overriding that plan. Replace `plan()` in `jev_sidecar/app.py` to plug in Laya, jevos, or another LLM later.

## Run

From this `sidecar` directory:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m jev_sidecar
```

On Linux or macOS, activate with `source .venv/bin/activate` instead.

The server listens on `http://127.0.0.1:8765`.

- `GET /health` — connection check used by the settings screen
- `POST /v1/systemone` — observation JSON in, action JSON out

The mod's default address is that URL. In game, press `J` (or `.jev settings`) and use **Test connection**.
