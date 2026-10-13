#!/usr/bin/env python3
"""Headless playtesting: the real game client on an invisible X display, driven by xdotool.

Needs Xvfb, xdotool, ImageMagick `import` and Mesa (software OpenGL). Start the display first:
    Xvfb :99 -screen 0 1600x900x24 -nolisten tcp &

Subcommands (run from the project root):
    world SEED NAME       generate a Meridian world (dev server), cheats on, into run/saves/NAME
    client NAME           launch the dev client on :99 straight into run/saves/NAME (background)
    serve NAME            run run/saves/NAME on a dedicated dev server with RCON (background); Dev is op
    join                  launch the dev client on :99 connected to that server (background)
    rcon "command"        run a server console command (no slash), print the reply
    cmd "/some command"   type a chat command and press Enter
    key KEY [SECONDS]     press (or hold) a key: w, space, shift, F5, Escape, r, 1..9 ...
    look DX DY            turn the view by a mouse move of DX, DY pixels
    click [left|right] [SECONDS]
    shot OUT.png          screenshot the display
    stop                  quit the client

Writes run/ (gitignored). Nothing here touches a real installation or the user's display.
"""
from __future__ import annotations

import gzip
import io
import os
import secrets
import shutil
import struct
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import site_sweep as sw  # noqa: E402  (NBT reader, RCON client, paths)

ROOT = sw.ROOT
RUN = sw.RUN
# SIGNAL_DISPLAY=:0 runs on the real desktop (hardware GL, watchable); the default is the invisible :99.
DISPLAY = os.environ.get("SIGNAL_DISPLAY", ":99")
ENV = dict(os.environ, DISPLAY=DISPLAY)
if DISPLAY == ":99":
    ENV.update(__GLX_VENDOR_LIBRARY_NAME="mesa", LIBGL_ALWAYS_SOFTWARE="1")

# ------------------------------------------------------------------ NBT writing (for level.dat)


def _w_payload(out: io.BytesIO, tag: int, v) -> None:
    if tag == 1:
        out.write(struct.pack(">b", v))
    elif tag == 2:
        out.write(struct.pack(">h", v))
    elif tag == 3:
        out.write(struct.pack(">i", v))
    elif tag == 4:
        out.write(struct.pack(">q", v))
    elif tag == 5:
        out.write(struct.pack(">f", v))
    elif tag == 6:
        out.write(struct.pack(">d", v))
    elif tag == 7:
        out.write(struct.pack(">i", len(v)) + bytes(v))
    elif tag == 8:
        b = v.encode()
        out.write(struct.pack(">H", len(b)) + b)
    elif tag == 9:
        inner, items = v
        out.write(struct.pack(">bi", inner, len(items)))
        for it in items:
            _w_payload(out, inner, it)
    elif tag == 10:
        for name, (t, val) in v.items():
            out.write(struct.pack(">b", t))
            b = name.encode()
            out.write(struct.pack(">H", len(b)) + b)
            _w_payload(out, t, val)
        out.write(b"\0")
    elif tag == 11:
        out.write(struct.pack(">i", len(v)) + struct.pack(f">{len(v)}i", *v))
    elif tag == 12:
        out.write(struct.pack(">i", len(v)) + struct.pack(f">{len(v)}q", *v))


def _r_typed(buf: io.BytesIO, tag: int):
    """Like site_sweep's reader, but keeps tag types so the file can be written back unchanged."""
    r = buf.read
    if tag == 9:
        inner = r(1)[0]
        n = struct.unpack(">i", r(4))[0]
        return (inner, [_r_typed(buf, inner) for _ in range(n)])
    if tag == 10:
        out = {}
        while True:
            t = r(1)[0]
            if t == 0:
                return out
            n = struct.unpack(">H", r(2))[0]
            name = r(n).decode()
            out[name] = (t, _r_typed(buf, t))
    if tag == 7:
        n = struct.unpack(">i", r(4))[0]
        return r(n)
    if tag == 11:
        n = struct.unpack(">i", r(4))[0]
        return list(struct.unpack(f">{n}i", r(4 * n)))
    if tag == 12:
        n = struct.unpack(">i", r(4))[0]
        return list(struct.unpack(f">{n}q", r(8 * n)))
    return sw._nbt_payload(buf, tag)


def patch_level(path: Path, **data: tuple[int, object]) -> None:
    raw = gzip.decompress(path.read_bytes())
    buf = io.BytesIO(raw)
    tag = buf.read(1)[0]
    n = struct.unpack(">H", buf.read(2))[0]
    name = buf.read(n).decode()
    root = _r_typed(buf, tag)
    root["Data"][1].update(data)
    out = io.BytesIO()
    out.write(struct.pack(">bH", tag, len(name.encode())) + name.encode())
    _w_payload(out, tag, root)
    path.write_bytes(gzip.compress(out.getvalue()))


# ------------------------------------------------------------------ world and client


def make_world(seed: int, name: str) -> None:
    RUN.mkdir(exist_ok=True)
    shutil.rmtree(RUN / "world", ignore_errors=True)
    (RUN / "eula.txt").write_text("eula=true\n")
    pw = secrets.token_hex(16)
    props = {"server-ip": "127.0.0.1", "level-seed": str(seed), "level-type": "signal\\:meridian",
             "online-mode": "false", "level-name": "world", "enable-rcon": "true", "rcon.port": str(sw.RCON_PORT),
             "rcon.password": pw, "view-distance": "8"}
    (RUN / "server.properties").write_text("".join(f"{k}={v}\n" for k, v in props.items()))
    with open(RUN / "playtest-server.log", "w") as log:
        proc = subprocess.Popen(["./gradlew", "--no-daemon", "runServer", "--console=plain"], cwd=ROOT,
                                stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            rcon, t0 = None, time.time()
            while rcon is None:
                if time.time() - t0 > 600:
                    raise SystemExit("server did not start")
                try:
                    rcon = sw.Rcon(sw.RCON_PORT, pw)
                except OSError:
                    time.sleep(2)
            rcon.command("save-all flush")
            try:
                rcon.command("stop")
            except Exception:
                pass
            proc.wait(timeout=300)
        finally:
            if proc.poll() is None:
                os.killpg(proc.pid, 9)
    dest = RUN / "saves" / name
    shutil.rmtree(dest, ignore_errors=True)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.move(str(RUN / "world"), dest)
    patch_level(dest / "level.dat", allowCommands=(1, 1), GameType=(3, 0), LevelName=(8, name))
    print("world ready:", dest)


def client(name: str) -> None:
    opts = RUN / "options.txt"
    if not opts.exists():  # sane defaults; software rendering on :99 needs the low distances
        opts.write_text("renderDistance:8\nsimulationDistance:6\nmaxFps:60\ngraphicsMode:1\nguiScale:2\npauseOnLostFocus:false\n"
                        "onboardAccessibility:false\nskipMultiplayerWarning:true\ntutorialStep:none\nsoundCategory_master:0.0\n")
    log = open(RUN / "playtest-client.log", "w")
    subprocess.Popen(["./gradlew", "--no-daemon", "runClient", f"-PquickPlayWorld={name}", "--console=plain"], cwd=ROOT,
                     env=ENV, stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    print("client starting; log in run/playtest-client.log")


# ------------------------------------------------------------------ input and capture


def window() -> str:
    out = subprocess.run(["xdotool", "search", "--onlyvisible", "--name", "Minecraft"], env=ENV, capture_output=True, text=True).stdout.split()
    if not out:
        raise SystemExit("no Minecraft window on " + DISPLAY)
    return out[-1]


def xdo(*args: str) -> None:
    subprocess.run(["xdotool", *args], env=ENV, check=True)


def focus() -> str:
    """Raise and focus the game window; synthetic events sent to an unfocused GLFW window are ignored."""
    w = window()
    # Xvfb has no window manager, so there is nothing to "activate": give it input focus directly.
    verb = "windowfocus" if DISPLAY == ":99" else "windowactivate"
    subprocess.run(["xdotool", verb, "--sync", w], env=ENV, capture_output=True)
    return w


KEYS = {"shift": "Shift_L", "space": "space", "esc": "Escape", "enter": "Return", "ctrl": "Control_L"}


def key(k: str, seconds: float = 0.0) -> None:
    focus()
    k = KEYS.get(k.lower(), k)
    if seconds <= 0:
        xdo("key", k)
    else:
        xdo("keydown", k)
        time.sleep(seconds)
        xdo("keyup", k)


def cmd(text: str) -> None:
    focus()
    xdo("key", "Escape")  # close anything half-open (a stray chat line) before starting
    time.sleep(0.6)
    xdo("key", "t")
    time.sleep(1.0)
    xdo("type", "--delay", "45", text.lstrip("/") if text.startswith("/") and False else text)
    time.sleep(0.6)
    xdo("key", "Return")
    time.sleep(1.0)


def look(dx: int, dy: int) -> None:
    focus()
    xdo("mousemove_relative", "--", str(dx), str(dy))


def click(button: str = "left", seconds: float = 0.0) -> None:
    focus()
    b = "1" if button == "left" else "3"
    if seconds <= 0:
        xdo("click", b)
    else:
        xdo("mousedown", b)
        time.sleep(seconds)
        xdo("mouseup", b)


def shot(out: str) -> None:
    """Capture the game window to {@code out} (X capture of the window; works on Xvfb and on Xwayland)."""
    subprocess.run(["import", "-window", window(), out], env=ENV, check=True)
    print(out)


def f2shot(out: str) -> None:
    """The game's own screenshot (F2), copied to {@code out}."""
    shots = RUN / "screenshots"
    before = set(shots.glob("*.png")) if shots.exists() else set()
    key("F2")
    for _ in range(40):
        time.sleep(0.25)
        new = set(shots.glob("*.png")) - before if shots.exists() else set()
        if new:
            time.sleep(0.5)
            shutil.copy(max(new, key=lambda p: p.stat().st_mtime), out)
            print(out)
            return
    raise SystemExit("no screenshot appeared")


def viewer(fps: float = 2.0) -> None:
    """A watch-only window on the real desktop mirroring the invisible display; input never reaches the game."""
    import tkinter as tk
    from PIL import Image, ImageTk

    root = tk.Tk()
    root.title("SIGNAL playtest (watch only)")
    label = tk.Label(root, bg="black")
    label.pack(fill="both", expand=True)
    root.geometry("1280x740")

    def refresh() -> None:
        try:
            png = subprocess.run(["import", "-display", DISPLAY, "-window", "root", "png:-"], capture_output=True, timeout=5).stdout
            im = Image.open(io.BytesIO(png))
            w, h = max(1, label.winfo_width()), max(1, label.winfo_height())
            im.thumbnail((w, h))
            label.image = ImageTk.PhotoImage(im)
            label.configure(image=label.image)
        except Exception:
            pass
        root.after(int(1000 / fps), refresh)

    refresh()
    root.mainloop()


RCON_FILE = RUN / "playtest-rcon.txt"


def serve(name: str) -> None:
    src = RUN / "saves" / name
    if not src.exists():
        raise SystemExit(f"no world {src}")
    shutil.rmtree(RUN / "world", ignore_errors=True)
    shutil.copytree(src, RUN / "world")
    (RUN / "eula.txt").write_text("eula=true\n")
    pw = secrets.token_hex(16)
    RCON_FILE.write_text(pw)
    props = {"server-ip": "127.0.0.1", "level-type": "signal\\:meridian", "online-mode": "false", "level-name": "world",
             "enable-rcon": "true", "rcon.port": str(sw.RCON_PORT), "rcon.password": pw, "spawn-protection": "0",
             "view-distance": "8", "simulation-distance": "6", "allow-flight": "true", "max-tick-time": "-1"}
    (RUN / "server.properties").write_text("".join(f"{k}={v}\n" for k, v in props.items()))
    log = open(RUN / "playtest-server.log", "w")
    subprocess.Popen(["./gradlew", "--no-daemon", "runServer", "--console=plain"], cwd=ROOT, stdin=subprocess.DEVNULL,
                     stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    for _ in range(300):
        try:
            sw.Rcon(sw.RCON_PORT, pw).close()
            print("server up; rcon ready")
            return
        except OSError:
            time.sleep(2)
    raise SystemExit("server did not come up; see run/playtest-server.log")


def rcon(command: str) -> str:
    r = sw.Rcon(sw.RCON_PORT, RCON_FILE.read_text().strip())
    try:
        return r.command(command)
    finally:
        r.close()


def join() -> None:
    log = open(RUN / "playtest-client.log", "w")
    subprocess.Popen(["./gradlew", "--no-daemon", "runClient", "-PquickPlayServer=127.0.0.1:25565", "--console=plain"],
                     cwd=ROOT, env=ENV, stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    print("client connecting; log in run/playtest-client.log")


def main() -> None:
    a = sys.argv[1:]
    if not a:
        print(__doc__)
        return
    c = a[0]
    if c == "world":
        make_world(int(a[1]), a[2])
    elif c == "client":
        client(a[1])
    elif c == "cmd":
        cmd(a[1])
    elif c == "key":
        key(a[1], float(a[2]) if len(a) > 2 else 0.0)
    elif c == "look":
        look(int(a[1]), int(a[2]))
    elif c == "click":
        click(a[1] if len(a) > 1 else "left", float(a[2]) if len(a) > 2 else 0.0)
    elif c == "serve":
        serve(a[1])
    elif c == "join":
        join()
    elif c == "rcon":
        print(rcon(a[1]))
    elif c == "viewer":
        viewer(float(a[1]) if len(a) > 1 else 2.0)
    elif c == "shot":
        shot(a[1])
    elif c == "stop":
        cmd("/stop") if False else key("Escape")
    else:
        raise SystemExit(f"unknown subcommand {c}")


if __name__ == "__main__":
    main()
