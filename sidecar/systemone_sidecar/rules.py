"""Thin rule-based fallback when the local LLM is down or returns invalid JSON."""

from __future__ import annotations

import math
from typing import Any

from .actions import attack, look_at, move_to, stop, use_item

HOSTILE_TOKENS = (
    "zombie", "skeleton", "creeper", "spider", "enderman", "witch", "drowned",
    "husk", "stray", "phantom", "blaze", "piglin", "hoglin", "vex", "pillager",
    "vindicator", "ravager", "slime", "magma_cube", "silverfish", "endermite",
    "guardian", "shulker", "wither", "warden", "ghast", "zoglin", "evoker",
    "breeze", "bogged",
)

FOOD_TOKENS = (
    "bread", "apple", "golden apple", "carrot", "potato", "baked potato",
    "beetroot", "berry", "berries", "melon", "cookie", "pie", "stew", "soup",
    "honey", "chicken", "pork", "beef", "mutton", "rabbit", "cod", "salmon",
    "fish", "steak", "flesh", "chorus", "kelp", "glow berries",
)


def _player(obs: dict) -> dict:
    p = obs.get("player") or {}
    return p if isinstance(p, dict) else {}


def _goal(obs: dict) -> tuple[str, str]:
    g = obs.get("goal") or {}
    if not isinstance(g, dict):
        return "idle", ""
    return str(g.get("type") or "idle").lower(), str(g.get("target") or "").strip()


def _num(d: dict, key: str, default: float = 0.0) -> float:
    try:
        return float(d.get(key, default))
    except (TypeError, ValueError):
        return default


def _entities(obs: dict) -> list[dict]:
    raw = obs.get("entities") or []
    return [e for e in raw if isinstance(e, dict)]


def _players(obs: dict) -> list[dict]:
    raw = obs.get("players") or []
    return [p for p in raw if isinstance(p, dict)]


def _hotbar(obs: dict) -> list[str]:
    inv = obs.get("inventory") or {}
    if not isinstance(inv, dict):
        return []
    hb = inv.get("hotbar") or []
    return [str(x) for x in hb] if isinstance(hb, list) else []


def _is_hostile(entity: dict) -> bool:
    t = str(entity.get("type") or "").lower()
    return any(tok in t for tok in HOSTILE_TOKENS)


def _nearest_hostile(obs: dict, max_dist: float) -> dict | None:
    best = None
    best_d = max_dist
    for e in _entities(obs):
        if not _is_hostile(e):
            continue
        d = _num(e, "dist", 999.0)
        if d <= best_d:
            best = e
            best_d = d
    return best


def _dist_between(a: dict, b: dict) -> float:
    return math.dist(
        (_num(a, "x"), _num(a, "y"), _num(a, "z")),
        (_num(b, "x"), _num(b, "y"), _num(b, "z")),
    )


def _nearest_hostile_around(obs: dict, focus: dict | None, max_dist: float) -> dict | None:
    """Hostile closest to focus when it has coordinates, else closest to self."""
    if focus is None or not _has_coords(focus):
        return _nearest_hostile(obs, max_dist)
    best = None
    best_d = max_dist
    for entity in _entities(obs):
        if not _is_hostile(entity) or not _has_coords(entity):
            continue
        distance = _dist_between(focus, entity)
        if distance <= best_d:
            best = entity
            best_d = distance
    return best


def _engage(threat: dict) -> dict:
    distance = _num(threat, "dist", 999.0)
    entity_id = str(threat.get("id") or "").strip()
    if distance > 3.2 and _has_coords(threat):
        return move_to(_num(threat, "x"), _num(threat, "y"), _num(threat, "z"))
    if entity_id:
        return attack(entity_id)
    if _has_coords(threat):
        return look_at(_num(threat, "x"), _num(threat, "y") + 1.0, _num(threat, "z"))
    return stop()


def _find_player(obs: dict, name: str) -> dict | None:
    if not name:
        # nearest other player
        best = None
        best_d = 1e9
        for p in _players(obs):
            d = _num(p, "dist", 999.0)
            if d < best_d:
                best = p
                best_d = d
        return best
    name_l = name.lower()
    for p in _players(obs):
        if str(p.get("name") or "").lower() == name_l:
            return p
    return None


def _has_coords(obj: dict) -> bool:
    return all(k in obj for k in ("x", "y", "z"))


def _find_food_slot(hotbar: list[str], prefer_gapple: bool = False) -> int | None:
    if prefer_gapple:
        for i, label in enumerate(hotbar):
            low = label.lower()
            if "empty" in low:
                continue
            if "golden apple" in low or "enchanted golden apple" in low:
                return i
    for i, label in enumerate(hotbar):
        low = label.lower()
        if not low or low == "empty":
            continue
        if any(tok in low for tok in FOOD_TOKENS):
            return i
    return None


def _flee(player: dict, threat: dict) -> dict:
    px, py, pz = _num(player, "x"), _num(player, "y"), _num(player, "z")
    if _has_coords(threat):
        tx, ty, tz = _num(threat, "x"), _num(threat, "y"), _num(threat, "z")
        dx, dz = px - tx, pz - tz
    else:
        # no coords: step backward along yaw
        yaw = math.radians(_num(player, "yaw"))
        dx, dz = math.sin(yaw), -math.cos(yaw)
        ty = py
    length = math.hypot(dx, dz) or 1.0
    dist = 8.0
    return move_to(px + dx / length * dist, ty if _has_coords(threat) else py, pz + dz / length * dist, sprint=True)


def plan_rules(observation: dict[str, Any]) -> dict:
    goal_type, target_name = _goal(observation)
    player = _player(observation)
    hotbar = _hotbar(observation)

    if goal_type in {"build", "dig"}:
        # Java owns block placement and the 1x2 tunnel.
        return stop()

    if goal_type in {"idle", "none", "stop", ""}:
        # slight look ahead
        yaw = math.radians(_num(player, "yaw"))
        px, py, pz = _num(player, "x"), _num(player, "y"), _num(player, "z")
        return look_at(px - math.sin(yaw) * 4.0, py + 1.5, pz + math.cos(yaw) * 4.0)

    if goal_type == "survive":
        health = _num(player, "health", 20.0)
        food = _num(player, "food", 20.0)
        close = _nearest_hostile(observation, 4.0)
        if health <= 10.0:
            slot = _find_food_slot(hotbar, prefer_gapple=True)
            if slot is not None and ("golden" in hotbar[slot].lower()):
                return use_item(slot)
            # any food if no gapple
            slot = _find_food_slot(hotbar)
            if slot is not None and health <= 6.0:
                return use_item(slot)
        if food <= 14.0 and close is None:
            slot = _find_food_slot(hotbar)
            if slot is not None:
                return use_item(slot)
        threat = _nearest_hostile(observation, 12.0)
        if threat is not None:
            return _flee(player, threat)
        return stop()

    if goal_type == "protect":
        threat = _nearest_hostile(observation, 16.0)
        if threat is not None:
            action = _engage(threat)
            if action.get("action") != "STOP":
                return action
        ally = _find_player(observation, target_name)
        if ally is None:
            return stop()
        d = _num(ally, "dist", 999.0)
        if d > 4.5 and _has_coords(ally):
            return move_to(_num(ally, "x"), _num(ally, "y"), _num(ally, "z"))
        if _has_coords(ally):
            return look_at(_num(ally, "x"), _num(ally, "y") + 1.6, _num(ally, "z"))
        return stop()

    if goal_type == "fight":
        # Hostiles near the ally first, then near self. Otherwise stay with the ally.
        ally = _find_player(observation, target_name)
        threat = _nearest_hostile_around(observation, ally, 8.0) if ally is not None else None
        if threat is None:
            threat = _nearest_hostile(observation, 8.0)
        if threat is not None:
            action = _engage(threat)
            if action.get("action") != "STOP":
                return action
        if ally is None or not _has_coords(ally):
            return stop()
        d = _num(ally, "dist", 999.0)
        if d > 4.5:
            return move_to(_num(ally, "x"), _num(ally, "y"), _num(ally, "z"))
        return look_at(_num(ally, "x"), _num(ally, "y") + 1.6, _num(ally, "z"))

    if goal_type == "follow":
        ally = _find_player(observation, target_name)
        if ally is None or not _has_coords(ally):
            return stop()
        d = _num(ally, "dist", 999.0)
        if d <= 3.0:
            return stop()
        sprint = d > 8.0
        return move_to(_num(ally, "x"), _num(ally, "y"), _num(ally, "z"), sprint=sprint)

    return stop()
