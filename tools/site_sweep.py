#!/usr/bin/env python3
"""Build the Bellwether site on several Meridian seeds and check what came out, without a game client.

For each seed: a fresh dev server (`./gradlew runServer`, game directory `run/`, gitignored) generates a
Meridian Corridor world, the console runs `/signal scenario create bellwether ~ ~ ~` at world spawn (what a
player standing at spawn would get), the world is saved and the server stopped. The region files are then
read directly and the site is judged:

  * created or refused (and why),
  * water hanging over air inside the bounds (must be 0),
  * floating logs (must be 0),
  * bare dirt faces: dirt with open air beside it, i.e. raw cut faces (lower is better),
  * steep steps: neighbouring columns 3+ blocks apart inside the site (natural terrain counts too),

and a shaded top-down map is written per seed so the layout can be looked at.

Usage:
    tools/site_sweep.py --out /tmp/sweep 91024 4242 7 ...
    tools/site_sweep.py --out /tmp/sweep --no-server 91024        # re-analyse saved worlds only

The JAR under test is whatever `./gradlew runServer` builds from the working tree. Writes `run/eula.txt`
(a local dev server cannot start without it) and moves each world to <out>/world-<seed>.
"""
from __future__ import annotations

import argparse
import io
import json
import math
import os
import re
import secrets
import shutil
import signal
import socket
import struct
import subprocess
import sys
import time
import zipfile
import zlib
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RUN = ROOT / "run"
CLIENT_JAR = Path.home() / ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"

# ------------------------------------------------------------------ NBT


def _nbt_payload(buf: io.BytesIO, tag: int):
    r = buf.read
    if tag == 1:
        return struct.unpack(">b", r(1))[0]
    if tag == 2:
        return struct.unpack(">h", r(2))[0]
    if tag == 3:
        return struct.unpack(">i", r(4))[0]
    if tag == 4:
        return struct.unpack(">q", r(8))[0]
    if tag == 5:
        return struct.unpack(">f", r(4))[0]
    if tag == 6:
        return struct.unpack(">d", r(8))[0]
    if tag == 7:
        n = struct.unpack(">i", r(4))[0]
        return r(n)
    if tag == 8:
        n = struct.unpack(">H", r(2))[0]
        return r(n).decode("utf-8", "replace")
    if tag == 9:
        inner = r(1)[0]
        n = struct.unpack(">i", r(4))[0]
        return [_nbt_payload(buf, inner) for _ in range(n)]
    if tag == 10:
        out = {}
        while True:
            t = r(1)[0]
            if t == 0:
                return out
            n = struct.unpack(">H", r(2))[0]
            name = r(n).decode("utf-8", "replace")
            out[name] = _nbt_payload(buf, t)
    if tag == 11:
        n = struct.unpack(">i", r(4))[0]
        return np.frombuffer(r(4 * n), dtype=">i4")
    if tag == 12:
        n = struct.unpack(">i", r(4))[0]
        return np.frombuffer(r(8 * n), dtype=">i8")
    raise ValueError(f"bad NBT tag {tag}")


def read_nbt(data: bytes) -> dict:
    buf = io.BytesIO(data)
    tag = buf.read(1)[0]
    n = struct.unpack(">H", buf.read(2))[0]
    buf.read(n)
    return _nbt_payload(buf, tag)


# ------------------------------------------------------------------ world access


class World:
    """Block names in a box of a saved world, from its region files (Anvil, 1.18+ section format)."""

    def __init__(self, world: Path):
        self.region = world / "region"
        self.chunks: dict[tuple[int, int], dict[int, np.ndarray]] = {}
        self.palettes: dict[tuple[int, int], dict[int, list[str]]] = {}

    def _load_chunk(self, cx: int, cz: int):
        key = (cx, cz)
        if key in self.chunks:
            return
        sections: dict[int, np.ndarray] = {}
        palettes: dict[int, list[str]] = {}
        path = self.region / f"r.{cx >> 5}.{cz >> 5}.mca"
        if path.exists():
            with path.open("rb") as f:
                head = f.read(4096)
                i = 4 * ((cx & 31) + (cz & 31) * 32)
                off = int.from_bytes(head[i:i + 3], "big")
                if off:
                    f.seek(off * 4096)
                    length = int.from_bytes(f.read(4), "big")
                    comp = f.read(1)[0]
                    raw = f.read(length - 1)
                    nbt = read_nbt(zlib.decompress(raw) if comp == 2 else raw)
                    for sec in nbt.get("sections", []):
                        bs = sec.get("block_states")
                        if not bs:
                            continue
                        pal = [p["Name"].removeprefix("minecraft:") for p in bs["palette"]]
                        y = sec["Y"]
                        if len(pal) == 1:
                            sections[y] = np.zeros(4096, dtype=np.int32)
                        else:
                            bits = max(4, (len(pal) - 1).bit_length())
                            per = 64 // bits
                            longs = bs["data"].astype(np.uint64)
                            idx = np.arange(4096)
                            word = longs[idx // per]
                            shift = ((idx % per) * bits).astype(np.uint64)
                            sections[y] = ((word >> shift) & np.uint64((1 << bits) - 1)).astype(np.int32)
                        palettes[y] = pal
        self.chunks[key] = sections
        self.palettes[key] = palettes

    def block(self, x: int, y: int, z: int) -> str:
        cx, cz = x >> 4, z >> 4
        self._load_chunk(cx, cz)
        sy = y >> 4
        sec = self.chunks[(cx, cz)].get(sy)
        if sec is None:
            return "air"
        return self.palettes[(cx, cz)][sy][sec[((y & 15) * 16 + (z & 15)) * 16 + (x & 15)]]

    def column(self, x: int, z: int, y0: int, y1: int) -> list[str]:
        return [self.block(x, y, z) for y in range(y0, y1 + 1)]


# ------------------------------------------------------------------ colours

TINT_GRASS = (0x6f, 0x7a, 0x4a)  # Meridian olive (approximate)
TINT_LEAVES = (0x4f, 0x5e, 0x3a)
TINTED = {"grass_block": TINT_GRASS, "short_grass": TINT_GRASS, "tall_grass": TINT_GRASS, "fern": TINT_GRASS,
          "large_fern": TINT_GRASS, "vine": TINT_LEAVES, "spruce_leaves": TINT_LEAVES, "oak_leaves": TINT_LEAVES,
          "dark_oak_leaves": TINT_LEAVES, "birch_leaves": TINT_LEAVES, "water": (0x3a, 0x40, 0x3c)}
SPECIAL = {"air": None, "cave_air": None, "void_air": None, "lava": (0xcf, 0x5a, 0x10)}
TRANSPARENT = {"air", "cave_air", "void_air", "barrier", "light", "glass_pane", "iron_bars", "chain", "rail",
               "short_grass", "tall_grass", "fern", "large_fern", "vine", "snow", "dead_bush", "sweet_berry_bush",
               "lantern", "torch", "wall_torch", "spruce_sign", "spruce_wall_sign", "cobweb", "ladder",
               "lightning_rod", "candle", "brown_mushroom", "red_mushroom", "moss_carpet", "lily_pad"}


NATURAL_GROUND = {"grass_block", "dirt", "coarse_dirt", "podzol", "mud", "packed_mud", "rooted_dirt", "gravel",
                  "stone", "andesite", "diorite", "granite", "tuff", "deepslate", "sand", "clay", "moss_block",
                  "mycelium", "dirt_path", "water", "calcite", "cobblestone", "mossy_cobblestone", "snow_block"}


class Palette:
    def __init__(self):
        self.cache: dict[str, tuple[int, int, int]] = {}
        self.jar = zipfile.ZipFile(CLIENT_JAR) if CLIENT_JAR.exists() else None

    def _texture(self, name: str):
        if self.jar is None:
            return None
        base = re.sub(r"_(slab|stairs|wall|fence|fence_gate|trapdoor|door|button|pressure_plate)$", "", name)
        base = {"waxed_exposed_cut_copper": "exposed_cut_copper", "waxed_weathered_cut_copper": "weathered_cut_copper",
                "waxed_oxidized_cut_copper": "oxidized_cut_copper", "waxed_exposed_copper": "exposed_copper",
                "waxed_weathered_copper": "weathered_copper", "waxed_oxidized_copper": "oxidized_copper",
                "stone_brick": "stone_bricks", "mud_brick": "mud_bricks", "tuff_brick": "tuff_bricks",
                "polished_blackstone_brick": "polished_blackstone_bricks", "spruce": "spruce_planks",
                "dark_oak": "dark_oak_planks", "oak": "oak_planks", "mossy_cobblestone": "mossy_cobblestone",
                "smooth_stone": "smooth_stone", "deepslate_tile": "deepslate_tiles", "brick": "bricks",
                "cobbled_deepslate": "cobbled_deepslate"}.get(base, base)
        for cand in (f"{base}_top", base, f"{base}_side", f"{name}_top", name):
            try:
                img = Image.open(io.BytesIO(self.jar.read(f"assets/minecraft/textures/block/{cand}.png"))).convert("RGBA")
            except KeyError:
                continue
            a = np.asarray(img.crop((0, 0, 16, 16)), dtype=np.float64)
            w = a[..., 3:4] / 255.0
            if w.sum() == 0:
                continue
            return tuple(int(v) for v in (a[..., :3] * w).sum(axis=(0, 1)) / w.sum())
        return None

    def color(self, name: str):
        if name in SPECIAL:
            return SPECIAL[name]
        if name not in self.cache:
            c = self._texture(name)
            if c is None:
                h = zlib.crc32(name.encode())
                c = (90 + h % 90, 90 + (h >> 8) % 90, 90 + (h >> 16) % 90)  # unknown: stable grey-ish
            if name in TINTED:
                t = TINTED[name]
                if name == "grass_block":
                    c = t
                else:
                    c = tuple(int(c[i] * t[i] / 255 * 1.6) for i in range(3))
            self.cache[name] = c
        return self.cache[name]


# ------------------------------------------------------------------ analysis


def top_down(world: World, box: dict, pal: Palette, scale: int = 4):
    x0, y0, z0, x1, y1, z1 = box["minX"], box["minY"], box["minZ"], box["maxX"], box["maxY"], box["maxZ"]
    w, d = x1 - x0 + 1, z1 - z0 + 1
    height = np.zeros((d, w), dtype=np.int32)
    img = np.zeros((d, w, 3), dtype=np.float64)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            col = world.column(x, z, y0, y1)
            for i in range(len(col) - 1, -1, -1):
                n = col[i]
                if n in TRANSPARENT and n not in ("glass_pane",):
                    continue
                c = pal.color(n)
                if c is None:
                    continue
                height[z - z0, x - x0] = y0 + i
                img[z - z0, x - x0] = c
                break
    # Hillshade from the north-west.
    shade = np.ones((d, w))
    dh = np.zeros((d, w))
    dh[1:, 1:] = height[1:, 1:] - height[:-1, :-1]
    shade += np.clip(dh, -4, 4) * 0.07
    out = np.clip(img * shade[..., None], 0, 255).astype(np.uint8)
    return Image.fromarray(out).resize((w * scale, d * scale), Image.NEAREST), height


def judge(world: World, box: dict, height: np.ndarray) -> dict:
    x0, y0, z0, x1, y1, z1 = box["minX"], box["minY"], box["minZ"], box["maxX"], box["maxY"], box["maxZ"]
    dirt_faces = 0
    hanging: list[tuple[int, int, int, str]] = []
    floating: list[tuple[int, int, int]] = []
    ground = np.zeros_like(height)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            col = world.column(x, z, y0, y1)
            for i, n in enumerate(col):
                y = y0 + i
                if n == "water":
                    for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
                        nx, ny, nz = x + dx, y + dy, z + dz
                        if x0 <= nx <= x1 and z0 <= nz <= z1 and y0 <= ny and world.block(nx, ny, nz) == "air":
                            hanging.append((x, y, z, "down" if dy else "side"))
                            break
                elif n.endswith("_log") and i > 0 and col[i - 1] == "air" and not any(
                        world.block(x + dx, y, z + dz) not in TRANSPARENT for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    floating.append((x, y, z))  # nothing below and nothing beside it (a cross-arm is held by its pole)
                elif n in ("dirt", "coarse_dirt", "rooted_dirt") and i + 1 < len(col) and col[i + 1] not in ("air",):
                    # A buried dirt block (not the surface) with open air beside it: a raw cut face.
                    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, nz = x + dx, z + dz
                        if x0 <= nx <= x1 and z0 <= nz <= z1 and world.block(nx, y, nz) == "air":
                            dirt_faces += 1
                            break
            for i in range(len(col) - 1, -1, -1):
                if col[i] in NATURAL_GROUND:
                    ground[z - z0, x - x0] = y0 + i
                    break
    # Steep steps in the ground itself (trees and buildings ignored).
    steep = int((np.abs(np.diff(ground, axis=0)) >= 3).sum() + (np.abs(np.diff(ground, axis=1)) >= 3).sum())
    return {"hanging_water": len(hanging), "hanging_at": hanging[:12], "floating_logs": len(floating), "floating_at": floating[:12],
            "bare_dirt_faces": dirt_faces, "steep_steps": steep}


# ------------------------------------------------------------------ server harness


RCON_PORT = 25599


class Rcon:
    """Minimal RCON client (the dev run does not forward stdin to the server console)."""

    def __init__(self, port: int, password: str):
        self.sock = socket.create_connection(("127.0.0.1", port), timeout=600)
        if self._send(3, password)[0] == -1:
            self.sock.close()
            raise PermissionError("rcon login refused")

    def _send(self, kind: int, body: str) -> tuple[int, str]:
        data = body.encode() + b"\0\0"
        self.sock.sendall(struct.pack("<iii", len(data) + 8, 1, kind) + data)
        size = struct.unpack("<i", self._read(4))[0]
        payload = self._read(size)
        return struct.unpack("<i", payload[:4])[0], payload[8:-2].decode("utf-8", "replace")

    def _read(self, n: int) -> bytes:
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("rcon closed")
            buf += chunk
        return buf

    def command(self, cmd: str) -> str:
        return self._send(2, cmd)[1]

    def close(self):
        self.sock.close()


def run_server(seed: int, log: Path, anchor: str = "~ ~ ~") -> str:
    RUN.mkdir(exist_ok=True)
    shutil.rmtree(RUN / "world", ignore_errors=True)
    (RUN / "eula.txt").write_text("eula=true\n")
    password = secrets.token_hex(16)  # fresh per run; the console is bound to localhost only
    props = {"server-ip": "127.0.0.1", "level-seed": str(seed), "level-type": "signal\\:meridian", "online-mode": "false",
             "spawn-protection": "0", "max-tick-time": "-1", "view-distance": "6", "simulation-distance": "4",
             "level-name": "world", "motd": "signal sweep", "enable-rcon": "true", "rcon.port": str(RCON_PORT),
             "rcon.password": password, "broadcast-rcon-to-ops": "false"}
    (RUN / "server.properties").write_text("".join(f"{k}={v}\n" for k, v in props.items()))
    with log.open("w") as lf:
        # Own process group, so a kill also takes the server JVM Gradle forks, not just the Gradle client.
        proc = subprocess.Popen(["./gradlew", "--no-daemon", "runServer", "--console=plain"], cwd=ROOT, stdin=subprocess.DEVNULL,
                                stdout=lf, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            deadline = time.time() + 600
            rcon = None
            while rcon is None:
                if proc.poll() is not None:
                    return f"server exited ({proc.returncode})"
                if time.time() > deadline:
                    return "timeout waiting for server"
                try:
                    rcon = Rcon(RCON_PORT, password)
                except OSError:
                    time.sleep(2)
            result = rcon.command(f"signal scenario create bellwether {anchor}").strip() or "(no reply)"
            rcon.command("save-all flush")
            try:
                rcon.command("stop")
            except (OSError, ConnectionError):
                pass
            rcon.close()
            try:
                proc.wait(timeout=300)
            except subprocess.TimeoutExpired:
                result += " (server did not stop; killed)"
            return result
        finally:
            if proc.poll() is None:
                os.killpg(proc.pid, signal.SIGKILL)
                proc.wait()


def site_box(world: Path) -> dict | None:
    data = world / "data" / "signal_campaign.dat"
    if not data.exists():
        return None
    nbt = read_nbt(zlib.decompress(data.read_bytes(), 15 + 32))
    tag = nbt.get("data", nbt)
    if not tag.get("Initialized"):
        return None
    b = [int(v) for v in tag["Bounds"]]
    return dict(zip(("minX", "minY", "minZ", "maxX", "maxY", "maxZ"), b))


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("seeds", nargs="+", type=int)
    ap.add_argument("--out", required=True, type=Path)
    ap.add_argument("--no-server", action="store_true", help="analyse worlds already in --out")
    ap.add_argument("--anchor", default="~ ~ ~", help='site anchor, e.g. "600 70 600" (default: world spawn)')
    args = ap.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    pal = Palette()
    report = {}
    for seed in args.seeds:
        wdir = args.out / f"world-{seed}"
        if not args.no_server:
            print(f"[{seed}] server ...", flush=True)
            res = run_server(seed, args.out / f"server-{seed}.log", args.anchor)
            print(f"[{seed}] {res}", flush=True)
            shutil.rmtree(wdir, ignore_errors=True)
            if (RUN / "world").exists():
                shutil.move(str(RUN / "world"), wdir)
        box = site_box(wdir)
        if not box:
            print(f"[{seed}] no site ({box})", flush=True)
            report[seed] = {"created": False, "box": box}
            continue
        world = World(wdir)
        img, height = top_down(world, box, pal)
        img.save(args.out / f"site-{seed}.png")
        j = judge(world, box, height)
        j.update(created=True, box=box, relief=int(height.max() - height.min()))
        report[seed] = j
        print(f"[{seed}] {j}", flush=True)
    (args.out / "report.json").write_text(json.dumps(report, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
