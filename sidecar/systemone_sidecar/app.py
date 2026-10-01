"""Laya (System One MC) sidecar.

The Minecraft client POSTs a compact observation JSON to ``/v1/systemone``.
Primary planner: local OpenAI-compatible LLM (Ollama by default). Thin
rule-based fallback when the model is off, unreachable, times out, or returns
invalid JSON.

Env:
  SYSTEMONE_LLM=ollama|openai|off   (default ollama)
  SYSTEMONE_BASE_URL                (default http://127.0.0.1:11434/v1)
  SYSTEMONE_MODEL                   (default llama3.2:3b)
  SYSTEMONE_API_KEY                 (optional; default "ollama")
  SYSTEMONE_LLM_TIMEOUT             (seconds; default 2.5)

STOP = no override (Java keyword plan keeps running). Other actions override.
"""

from __future__ import annotations

from fastapi import FastAPI

from .llm import llm_config, plan_llm, probe_llm
from .rules import plan_rules

app = FastAPI(title="Laya (System One MC) sidecar", version="1.1.0")


@app.get("/health")
def health() -> dict:
    cfg = llm_config()
    probe = probe_llm()
    if cfg["mode"] == "off":
        planner = "rules"
    elif probe.get("reachable"):
        planner = "llm+rules"
    else:
        planner = "rules"  # LLM configured but down — fallback active
    return {
        "ok": True,
        "planner": planner,
        "llm": {
            "mode": cfg["mode"],
            "base_url": cfg["base_url"],
            "model": cfg["model"],
            "reachable": bool(probe.get("reachable")),
        },
    }


@app.post("/v1/systemone")
def systemone(observation: dict) -> dict:
    return plan(observation)


def plan(observation: dict) -> dict:
    """LLM first; rules if LLM off/fails/invalid."""
    if not isinstance(observation, dict):
        observation = {}
    llm_action = plan_llm(observation)
    if llm_action is not None:
        return llm_action
    return plan_rules(observation)
