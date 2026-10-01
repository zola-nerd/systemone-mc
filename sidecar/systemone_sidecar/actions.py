"""Action helpers and validation for System One MC sidecar replies."""

from __future__ import annotations

from typing import Any

ALLOWED_ACTIONS = frozenset({
    "STOP",
    "MOVE_TO",
    "LOOK_AT",
    "ATTACK",
    "USE_ITEM",
    "PLACE_BLOCK",
    "BREAK_BLOCK",
    "JUMP",
    "SNEAK",
    "HOTBAR_SELECT",
    "SAY",
})


def stop() -> dict:
    return {"action": "STOP"}


def move_to(x: float, y: float, z: float, sprint: bool = False) -> dict:
    return {"action": "MOVE_TO", "x": float(x), "y": float(y), "z": float(z), "sprint": bool(sprint)}


def look_at(x: float, y: float, z: float) -> dict:
    return {"action": "LOOK_AT", "x": float(x), "y": float(y), "z": float(z)}


def attack(target: str) -> dict:
    return {"action": "ATTACK", "target": str(target)}


def use_item(slot: int) -> dict:
    return {"action": "USE_ITEM", "slot": int(slot)}


def validate_action(raw: Any) -> dict | None:
    """Return a cleaned action dict, or None if invalid."""
    if not isinstance(raw, dict):
        return None
    action = str(raw.get("action", "")).strip().upper()
    if action not in ALLOWED_ACTIONS:
        return None

    out: dict[str, Any] = {"action": action}

    if action in {"MOVE_TO", "LOOK_AT", "PLACE_BLOCK", "BREAK_BLOCK"}:
        try:
            out["x"] = float(raw["x"])
            out["y"] = float(raw["y"])
            out["z"] = float(raw["z"])
        except (KeyError, TypeError, ValueError):
            return None
        if action == "MOVE_TO":
            out["sprint"] = bool(raw.get("sprint", False))
        return out

    if action == "ATTACK":
        target = raw.get("target")
        if target is None or str(target).strip() == "":
            return None
        out["target"] = str(target).strip()
        return out

    if action in {"USE_ITEM", "HOTBAR_SELECT"}:
        try:
            slot = int(raw["slot"])
        except (KeyError, TypeError, ValueError):
            return None
        if slot < 0 or slot > 8:
            return None
        out["slot"] = slot
        return out

    if action == "SNEAK":
        out["enabled"] = bool(raw.get("enabled", True))
        return out

    if action == "SAY":
        message = raw.get("message")
        if message is None:
            return None
        out["message"] = str(message)
        return out

    # STOP, JUMP
    return out
