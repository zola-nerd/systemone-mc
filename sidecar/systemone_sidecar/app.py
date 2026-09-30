"""System One MC sidecar.

The Minecraft client POSTs a compact observation JSON to ``/v1/systemone``.
This build is a dummy planner: it always returns ``{"action": "STOP"}``.

Keyword goals (protect, follow, build, idle, survive, stop) are matched on
the Java client first. This endpoint is the later hook for an LLM planner.

Plug-in point for Laya, jevos, or another planner: replace ``plan`` with a
function that reads the observation and returns one action object. Supported
``action`` values:

    STOP, MOVE_TO, LOOK_AT, ATTACK, USE_ITEM, PLACE_BLOCK,
    BREAK_BLOCK, JUMP, SNEAK, HOTBAR_SELECT, SAY

Examples the Java executor already understands::

    {"action": "STOP"}
    {"action": "MOVE_TO", "x": 0, "y": 64, "z": 0, "sprint": false}
    {"action": "LOOK_AT", "x": 0, "y": 64, "z": 0}
    {"action": "ATTACK", "target": "<uuid-or-name>"}
    {"action": "USE_ITEM", "slot": 0}
    {"action": "PLACE_BLOCK", "x": 0, "y": 64, "z": 0}
    {"action": "BREAK_BLOCK", "x": 0, "y": 64, "z": 0}
    {"action": "JUMP"}
    {"action": "SNEAK", "enabled": true}
    {"action": "HOTBAR_SELECT", "slot": 3}
    {"action": "SAY", "message": "hello"}

A STOP response means "no override". The Java keyword planner keeps running.
"""

from fastapi import FastAPI

app = FastAPI(title="System One MC sidecar", version="1.0.0")


@app.get("/health")
def health() -> dict:
    return {"ok": True, "planner": "dummy"}


@app.post("/v1/systemone")
def systemone(observation: dict) -> dict:
    return plan(observation)


def plan(observation: dict) -> dict:
    # Dummy planner. Keep the observation referenced so a future Laya / jevos
    # hook has an obvious place to read player, goal, entities, and inventory.
    _ = observation
    return {"action": "STOP"}
