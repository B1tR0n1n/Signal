#!/usr/bin/env python3
"""Build a listening post by hand through the playtest pilot, flying between a few work stations.

Usage: build_post.py OX OZ G   (footprint x OX..OX+6, z OZ..OZ+5, ground level G)

The work is a list of steps in build order. From each station the builder places the longest run of
upcoming steps it can reach, then flies to the station that reaches the most of what is next.
"""
import math
import sys
import time

sys.path.insert(0, str(__import__("pathlib").Path(__file__).resolve().parents[1]))
from pilot import Pilot  # noqa: E402

p = Pilot()
OX, OZ, G = int(sys.argv[1]), int(sys.argv[2]), int(sys.argv[3])
W, D = 7, 6
REACH = 3.7  # to block centre; the game measures to the clicked face, up to ~0.6 further
FIX = "--fix" in sys.argv
EYE = 1.62
DOOR = (OX + 3, OZ + D - 1)
MX, MZ = OX + W + 1, OZ  # mast
CRUISE = G + 8.0


def log(*a):
    print(*a, flush=True)


failures = []


def state():
    """Player state; waits out loading and death screens (no player fields) for up to a minute."""
    for _ in range(600):
        s = p.state()
        if s.get("screen") == "PauseScreen":
            p.send("close")
            continue
        if "pos" in s and not s.get("dead"):
            return s
        if s.get("dead"):
            p.send("respawn")
        time.sleep(0.1)
    raise SystemExit("no player state for a minute; stopping")


def pos():
    return state()["pos"]


def wait(cond, timeout):
    end = time.time() + timeout
    while time.time() < end:
        s = state()
        if cond(s):
            return s
        time.sleep(0.05)
    return state()


def alt(y):
    p.send("fly on")
    p.send(f"alt {y}")
    wait(lambda s: abs(s["pos"][1] - y) < 0.35, 6)


def fly_to(x, y, z):
    px, py, pz = pos()
    if math.dist((px, pz), (x, z)) > 0.6:
        # Long hops go over the top of the work; short ones stay at working height.
        alt(max(py, y, CRUISE) if math.dist((px, pz), (x, z)) > 4 else max(py, y))
        p.send(f"walk {x} {z} - 0.35")
        wait(lambda s: not s["walking"], 20)
    alt(y)


# ------------------------------------------------------------------ the plan

steps = []  # (action, item, x, y, z, face)


def put(item, x, y, z, face=None):
    steps.append(("place", item, x, y, z, face))


def build_plan():
    # Clear anything solid in the hut's volume and the mast's column.
    for y in range(G + 7, G, -1) if not FIX else ():
        for z in range(OZ - 1, OZ + D + 1):
            for x in range(OX - 1, OX + W + 2):
                b = p.send(f"block {x} {y} {z}")["block"]
                if not any(k in b for k in ("air", "short_grass", "fern", "dead_bush")):
                    steps.append(("clear", None, x, y, z, None))
    log("to clear:", sum(1 for s in steps if s[0] == "clear"))
    for z in range(OZ, OZ + D):
        for x in range(OX, OX + W):
            put("minecraft:stone_bricks", x, G + 1, z)
    windows = {(OX + 2, OZ), (OX + 4, OZ), (OX, OZ + 2), (OX, OZ + 3), (OX + W - 1, OZ + 2), (OX + W - 1, OZ + 3)}
    for y in (G + 2, G + 3, G + 4):
        for z in range(OZ, OZ + D):
            for x in range(OX, OX + W):
                if not (x in (OX, OX + W - 1) or z in (OZ, OZ + D - 1)) or (x, z) == DOOR and y < G + 4:
                    continue
                if x in (OX, OX + W - 1) and z in (OZ, OZ + D - 1):
                    put("minecraft:stripped_spruce_log", x, y, z)
                elif y == G + 3 and (x, z) in windows:
                    put("minecraft:glass_pane", x, y, z)
                else:
                    put("minecraft:spruce_planks", x, y, z)
    put("minecraft:spruce_door", DOOR[0], G + 2, DOOR[1])
    # Furnish before the roof closes it in.
    put("minecraft:cartography_table", OX + 1, G + 2, OZ + 1)
    put("minecraft:barrel", OX + W - 2, G + 2, OZ + 1)
    put("minecraft:chest", OX + 1, G + 2, OZ + D - 2)
    put("minecraft:red_carpet", OX + 3, G + 2, OZ + 2)
    put("minecraft:red_carpet", OX + 3, G + 2, OZ + 3)
    roof = G + 5
    cells = [(x, z) for z in range(OZ - 1, OZ + D + 1) for x in range(OX - 1, OX + W + 1)]

    def ring(c):  # 0 = over the walls, negative = inside, positive = overhang
        x, z = c
        inside = OX <= x < OX + W and OZ <= z < OZ + D
        if not inside:
            return 1 if (x in (OX - 1, OX + W) and z in (OZ - 1, OZ + D)) is False else 2
        return -min(x - OX, OX + W - 1 - x, z - OZ, OZ + D - 1 - z)

    rim = [c for c in cells if ring(c) == 0]
    inner = sorted((c for c in cells if ring(c) < 0), key=lambda c: -ring(c))
    edge = [c for c in cells if ring(c) == 1]
    corners = [c for c in cells if ring(c) == 2]
    for x, z in rim + inner + edge + corners:
        put("minecraft:spruce_slab", x, roof, z)
    for x in (DOOR[0] - 1, DOOR[0] + 1):
        put("minecraft:lantern", x, roof - 1, OZ + D, "up")
    # The mast.
    put("minecraft:stone_brick_wall", MX, G + 1, MZ)
    put("minecraft:stone_brick_wall", MX, G + 2, MZ)
    for y in range(G + 3, G + 13):
        put("minecraft:iron_bars", MX, y, MZ)
        if y == G + 11:
            put("minecraft:iron_bars", MX - 1, y, MZ)
            put("minecraft:iron_bars", MX + 1, y, MZ)
    put("minecraft:lightning_rod", MX, G + 13, MZ)


# ------------------------------------------------------------------ stations

solid = set()  # cells we have placed or know are built


def body_cells(sx, sy, sz):
    return {(sx, y, sz) for y in range(math.floor(sy), math.floor(sy + 1.8) + 1)}


def stations(roofed):
    out = []
    for sx in range(OX - 3, OX + W + 4):
        for sz in range(OZ - 3, OZ + D + 3):
            inside = OX <= sx < OX + W and OZ <= sz < OZ + D
            for sy in (G + 2.2, G + 4.6, G + 7.0, G + 9.5, G + 12.0):
                if inside and roofed and sy < G + 6:
                    continue
                if body_cells(sx, sy, sz) & solid:
                    continue
                out.append((sx + 0.5, sy, sz + 0.5))
    return out


def reach(st, s):
    x, y, z = s[2], s[3], s[4]
    return math.dist((st[0], st[1] + EYE, st[2]), (x + 0.5, y + 0.5, z + 0.5)) <= REACH


def run_from(st, i):
    n = 0
    while i + n < len(steps) and reach(st, steps[i + n]) and (steps[i + n][2], steps[i + n][3], steps[i + n][4]) not in body_cells(
            math.floor(st[0]), st[1], math.floor(st[2])):
        n += 1
    return n


holding = None


def do(s):
    global holding
    act, item, x, y, z, face = s
    if act == "clear":
        b = p.send(f"block {x} {y} {z}")["block"]
        if any(k in b for k in ("air", "short_grass", "fern", "dead_bush")):
            return
        p.send(f"break {x} {y} {z}")
        wait(lambda st: not st.get("result", "").startswith("break"), 3)
        return
    if item != holding:
        p.send(f"hold {item}")
        holding = item
    r = p.send(f"place {x} {y} {z}" + (f" {face}" if face else ""))
    if not r.get("ok"):
        log("  skip", item, x, y, z, r.get("error"))
        failures.append((item, x, y, z, r.get("error")))
        return
    st = wait(lambda st: not st.get("result", "").startswith("plac"), 3)
    if st.get("result") != "placed":
        log("  ", item, x, y, z, st.get("result"))
        failures.append((item, x, y, z, st.get("result")))
    solid.add((x, y, z))


def missing():
    """Planned placements whose block is not in the world yet."""
    out = []
    for s in steps:
        if s[0] != "place":
            continue
        b = p.send(f"block {s[2]} {s[3]} {s[4]}")["block"]
        want = s[1].split(":")[1].replace("_carpet", "_carpet")
        if want not in b:
            out.append(s)
        else:
            solid.add((s[2], s[3], s[4]))
    return out


def main():
    global steps
    p.send("cmd gamemode creative")
    p.send("cmd time set 1000")
    p.send("cmd weather clear")
    p.send("cmd gamerule doDaylightCycle false")
    build_plan()
    if FIX:
        steps = missing()
        log("missing:", len(steps))
    log("steps", len(steps))
    i = 0
    roofed = False
    while i < len(steps):
        here = tuple(pos())
        n = run_from(here, i)
        if n == 0:
            best = max(stations(roofed), key=lambda st: (run_from(st, i), -math.dist(st, here)))
            if run_from(best, i) == 0:
                log("unreachable", steps[i])
                failures.append(steps[i][1:5] + ("unreachable",))
                i += 1
                continue
            fly_to(*best)
            n = run_from(tuple(pos()), i) or 1
        here = tuple(pos())
        chunk = steps[i:i + n]
        if not any(s[1] == "minecraft:spruce_slab" for s in chunk):
            # No sideways dependencies here: sweep one way round, low courses first, so the view turns
            # steadily instead of snapping between targets.
            start = None

            def sweep(s):
                nonlocal start
                a = math.degrees(math.atan2(-(s[2] + 0.5 - here[0]), s[4] + 0.5 - here[2])) % 360
                return (0 if s[0] == "clear" else s[3], a)
            chunk = sorted(chunk, key=sweep)
        for s in chunk:
            do(s)
            if s[1] == "minecraft:spruce_slab":
                roofed = True
        i += n
        log(f"{i}/{len(steps)} at {tuple(round(v, 1) for v in pos())}")
    # Step back for the view.
    fly_to(OX + 3.5, G + 4.0, OZ + D + 7.5)
    p.send(f"lookat {OX + 3.5} {G + 4} {OZ + 2.5}")
    log(f"done: {len(steps)} steps, {len(failures)} failed")
    for f in failures:
        log("  failed:", f)


if __name__ == "__main__":
    main()
