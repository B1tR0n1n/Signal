#!/usr/bin/env python3
"""Talk to the in-game playtest pilot (client/PlaytestPilot.java).

Launch the client with the pilot on:
    SIGNAL_PILOT_PORT=25570 SIGNAL_DISPLAY=:0 python3 tools/playtest.py client WORLD

Then each argument is one step, run in order over one connection:
    python3 tools/pilot.py state
    python3 tools/pilot.py "walk 260 240 sprint" "until arrive 40" state
    python3 tools/pilot.py "aim signal:drawn" "slot 2" "use" "sleep 1" "aim off"
    python3 tools/pilot.py screen "press fit fuse" close

Pilot commands: state, walk X Z [sprint] [tol], stop, look YAW PITCH [deg/s], turn DYAW [DPITCH],
lookat X Y Z, aim TYPE|off, key NAME down|up|tap, use [ticks], attack, slot N, cmd /COMMAND, say TEXT,
screen, press LABEL, close, take SLOT, respawn.
Local steps: sleep S; until arrive [timeout]; shot NAME (window capture to $PILOT_SHOTS or /tmp).
"""
from __future__ import annotations

import json
import os
import socket
import subprocess
import sys
import time

PORT = int(os.environ.get("SIGNAL_PILOT_PORT", "25570"))
SHOTS = os.environ.get("PILOT_SHOTS", "/tmp")
DISPLAY = os.environ.get("SIGNAL_DISPLAY", ":0")


class Pilot:
    def __init__(self, port: int = PORT) -> None:
        # The pilot binds the JVM's loopback address, which may be ::1 rather than 127.0.0.1.
        last: OSError | None = None
        for host in ("::1", "127.0.0.1"):
            try:
                self.sock = socket.create_connection((host, port), timeout=10)
                break
            except OSError as e:
                last = e
        else:
            raise SystemExit(f"pilot not reachable on port {port}: {last}")
        self.rf = self.sock.makefile("r", encoding="utf-8")

    def send(self, line: str) -> dict:
        self.sock.sendall((line + "\n").encode())
        return json.loads(self.rf.readline())

    def state(self) -> dict:
        return self.send("state")


def brief(s: dict) -> str:
    if "pos" not in s:
        return json.dumps(s)
    x, y, z = s["pos"]
    near = ", ".join(f"{e['type'].split(':')[-1]}@{e['dist']}" for e in s.get("near", [])[:6])
    held = (s.get("held") or {}).get("id", "-")
    route = f" wp {s['waypoint']}/{s['of']}" if "waypoint" in s else ""
    return (f"pos {x:.1f} {y:.1f} {z:.1f} yaw {s['yaw']} hp {s['hp']} food {s['food']} t {s['time']} "
            f"held {held} screen {s['screen'] or '-'} walking {s['walking']}{route} {s.get('result', '')} | near: {near}")


def shot(name: str) -> str:
    out = os.path.join(SHOTS, name + ".png")
    env = dict(os.environ, DISPLAY=DISPLAY)
    wins = subprocess.run(["xdotool", "search", "--onlyvisible", "--name", "Minecraft"], env=env,
                          capture_output=True, text=True).stdout.split()
    if wins:
        subprocess.run(["import", "-window", wins[-1], out], env=env)
    return out


def run(steps: list[str]) -> None:
    p = Pilot()
    for step in steps:
        words = step.split()
        if words[0] == "sleep":
            time.sleep(float(words[1]))
        elif words[0] == "until" and words[1] == "arrive":
            limit = time.time() + (float(words[2]) if len(words) > 2 else 30)
            while time.time() < limit:
                s = p.state()
                if not s.get("walking") or s.get("dead"):
                    break
                time.sleep(0.25)
            print("until arrive:", brief(p.state()))
        elif words[0] == "shot":
            print(shot(words[1]))
        elif words[0] == "state":
            print(brief(p.state()))
        elif words[0] == "raw":
            print(json.dumps(p.state()))
        else:
            r = p.send(step)
            print(step, "->", r if not r.get("ok") or len(r) > 1 else "ok")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
    else:
        run(sys.argv[1:])
