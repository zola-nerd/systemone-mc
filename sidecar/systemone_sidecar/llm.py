"""Local OpenAI-compatible LLM planner (Ollama / LM Studio / etc.)."""

from __future__ import annotations

import json
import os
import re
import urllib.error
import urllib.request
from typing import Any

from .actions import validate_action

SYSTEM_PROMPT = """You are System One MC, a local Minecraft client AI. Given one observation JSON, reply with ONE action JSON object only — no markdown, no explanation.

Allowed actions and shapes:
{"action":"STOP"}
{"action":"MOVE_TO","x":0,"y":64,"z":0,"sprint":false}
{"action":"LOOK_AT","x":0,"y":64,"z":0}
{"action":"ATTACK","target":"<uuid-or-name>"}
{"action":"USE_ITEM","slot":0}
{"action":"PLACE_BLOCK","x":0,"y":64,"z":0}
{"action":"BREAK_BLOCK","x":0,"y":64,"z":0}
{"action":"JUMP"}
{"action":"SNEAK","enabled":true}
{"action":"HOTBAR_SELECT","slot":3}
{"action":"SAY","message":"hello"}

Rules:
- goal.type "build": always {"action":"STOP"} so the Java BuildPlan runs.
- goal.type "follow": MOVE_TO the goal.target player using players[].x/y/z; sprint if dist>8; STOP if dist<=3.
- goal.type "protect": ATTACK or MOVE_TO nearby hostiles (entities with hostile type); else stay near goal.target player.
- goal.type "survive": USE_ITEM food/gapple from hotbar if health/food low; else flee hostiles with MOVE_TO away; else STOP.
- goal.type "idle"/unknown: STOP or slight LOOK_AT.
- Prefer real coordinates from observation. ATTACK target must be entities[].id UUID when present.
- STOP means no override (Java keyword plan continues).
"""


def llm_config() -> dict[str, Any]:
    mode = (os.environ.get("SYSTEMONE_LLM") or "ollama").strip().lower()
    if mode in {"0", "false", "no", "off", "rules", "none"}:
        mode = "off"
    if mode in {"openai", "openai-compat", "compatible"}:
        mode = "openai"
    if mode not in {"ollama", "openai", "off"}:
        mode = "ollama"

    default_base = "http://127.0.0.1:11434/v1"
    base = (os.environ.get("SYSTEMONE_BASE_URL") or default_base).rstrip("/")
    model = os.environ.get("SYSTEMONE_MODEL") or "llama3.2:3b"
    api_key = os.environ.get("SYSTEMONE_API_KEY") or "ollama"
    try:
        timeout = float(os.environ.get("SYSTEMONE_LLM_TIMEOUT") or "2.5")
    except ValueError:
        timeout = 2.5
    return {
        "mode": mode,
        "base_url": base,
        "model": model,
        "api_key": api_key,
        "timeout": timeout,
    }


def _extract_json_object(text: str) -> Any | None:
    text = (text or "").strip()
    if not text:
        return None
    # strip markdown fences
    fence = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", text, re.DOTALL | re.IGNORECASE)
    if fence:
        text = fence.group(1)
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass
    start = text.find("{")
    end = text.rfind("}")
    if start >= 0 and end > start:
        try:
            return json.loads(text[start : end + 1])
        except json.JSONDecodeError:
            return None
    return None


def plan_llm(observation: dict) -> dict | None:
    """Ask the local LLM for an action. Returns validated action or None on failure."""
    cfg = llm_config()
    if cfg["mode"] == "off":
        return None

    payload = {
        "model": cfg["model"],
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {
                "role": "user",
                "content": "Observation:\n" + json.dumps(observation, separators=(",", ":"), ensure_ascii=False),
            },
        ],
        "temperature": 0.1,
        "max_tokens": 160,
    }
    # Prefer JSON mode when supported (Ollama openai compat often accepts it)
    payload["response_format"] = {"type": "json_object"}

    url = cfg["base_url"] + "/chat/completions"
    body = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {cfg['api_key']}",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=cfg["timeout"]) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
        data = json.loads(raw)
        content = data["choices"][0]["message"]["content"]
    except Exception:
        # Retry once without response_format for older servers
        payload.pop("response_format", None)
        body = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            url,
            data=body,
            method="POST",
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {cfg['api_key']}",
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=cfg["timeout"]) as resp:
                raw = resp.read().decode("utf-8", errors="replace")
            data = json.loads(raw)
            content = data["choices"][0]["message"]["content"]
        except Exception:
            return None

    parsed = _extract_json_object(content if isinstance(content, str) else json.dumps(content))
    return validate_action(parsed)


def probe_llm() -> dict[str, Any]:
    """Lightweight reachability check for /health."""
    cfg = llm_config()
    if cfg["mode"] == "off":
        return {"reachable": False, "reason": "off"}
    # Ollama native tags is cheap; openai-compat may only have models
    candidates = []
    base = cfg["base_url"]
    if base.endswith("/v1"):
        candidates.append(base[: -len("/v1")] + "/api/tags")
        candidates.append(base + "/models")
    else:
        candidates.append(base + "/api/tags")
        candidates.append(base.rstrip("/") + "/v1/models")
    for url in candidates:
        try:
            req = urllib.request.Request(url, method="GET", headers={"Authorization": f"Bearer {cfg['api_key']}"})
            with urllib.request.urlopen(req, timeout=1.5) as resp:
                if 200 <= resp.status < 300:
                    return {"reachable": True, "probe": url}
        except Exception:
            continue
    return {"reachable": False, "reason": "unreachable"}
