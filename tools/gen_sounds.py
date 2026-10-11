#!/usr/bin/env python3
"""Generate SIGNAL's original sound effects by synthesis (no samples, no recordings).

Deterministic (fixed seeds): the same script always writes the same audio. Every sound here is built from
noise, sine partials and filters in this file; see docs/ASSET_PROVENANCE.md.

Writes mono 44.1 kHz Ogg Vorbis to src/main/resources/assets/signal/sounds/ and the matching sounds.json.

Usage (from the project root):
    ~/.venvs/signal-tools/bin/python tools/gen_sounds.py
Requires numpy, scipy and soundfile (libsndfile with Vorbis): python3 -m venv ~/.venvs/signal-tools &&
~/.venvs/signal-tools/bin/pip install numpy scipy soundfile
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy import signal

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/signal/sounds"
SR = 44100


def t(seconds: float) -> np.ndarray:
    return np.arange(int(SR * seconds)) / SR


def noise(seconds: float, seed: int) -> np.ndarray:
    return np.random.default_rng(seed).standard_normal(int(SR * seconds))


def band(x: np.ndarray, lo: float, hi: float, order: int = 4) -> np.ndarray:
    sos = signal.butter(order, [lo, hi], btype="bandpass", fs=SR, output="sos")
    return signal.sosfilt(sos, x)


def low(x: np.ndarray, cutoff: float, order: int = 4) -> np.ndarray:
    return signal.sosfilt(signal.butter(order, cutoff, btype="lowpass", fs=SR, output="sos"), x)


def high(x: np.ndarray, cutoff: float, order: int = 2) -> np.ndarray:
    return signal.sosfilt(signal.butter(order, cutoff, btype="highpass", fs=SR, output="sos"), x)


def env(n: int, attack: float, decay: float) -> np.ndarray:
    """Linear attack then exponential decay (time constant {@code decay} seconds)."""
    tt = np.arange(n) / SR
    a = np.clip(tt / max(attack, 1e-5), 0, 1)
    return a * np.exp(-np.maximum(tt - attack, 0) / decay)


def pad(x: np.ndarray, seconds: float) -> np.ndarray:
    n = int(SR * seconds)
    return np.concatenate([x, np.zeros(max(0, n - len(x)))])[:n]


def tail(x: np.ndarray, seconds: float, seed: int, damp: float = 1800) -> np.ndarray:
    """A plain synthetic room/forest tail: x convolved with decaying low-passed noise."""
    ir = low(noise(seconds, seed), damp) * env(int(SR * seconds), 0.002, seconds / 5)
    ir /= np.max(np.abs(ir)) + 1e-9
    return signal.fftconvolve(x, ir)[: len(x) + len(ir) - 1]


def norm(x: np.ndarray, peak: float = 0.89) -> np.ndarray:
    x = x - np.mean(x)
    fade = min(len(x), int(SR * 0.01))
    x[-fade:] *= np.linspace(1, 0, fade)
    return x * (peak / (np.max(np.abs(x)) + 1e-9))


# ------------------------------------------------------------------ guns


def revolver_shot(seed: int) -> np.ndarray:
    n = int(SR * 0.6)
    crack = high(noise(0.6, seed), 900) * env(n, 0.0004, 0.012)
    body = band(noise(0.6, seed + 1), 120, 2400) * env(n, 0.001, 0.05)
    tt = t(0.6)
    thump = np.sin(2 * np.pi * (70 + 60 * np.exp(-tt * 30)) * tt) * env(n, 0.001, 0.08)
    dry = 1.0 * crack + 0.9 * body + 0.8 * thump
    wet = tail(dry, 1.6, seed + 2, damp=1400)
    return norm(pad(dry, 2.2) + 0.35 * pad(wet, 2.2))


def click(seconds: float, freqs: tuple[float, ...], seed: int, decay: float = 0.004) -> np.ndarray:
    n = int(SR * seconds)
    tt = t(seconds)
    tone = sum(np.sin(2 * np.pi * f * tt) for f in freqs) / len(freqs)
    return (0.6 * tone + 0.6 * high(noise(seconds, seed), 2500)) * env(n, 0.0002, decay)


def revolver_dry(seed: int) -> np.ndarray:
    return norm(pad(click(0.12, (3100, 4700, 6900), seed), 0.2), 0.6)


def revolver_load(seed: int) -> np.ndarray:
    a = click(0.08, (2300, 3900), seed, 0.006)
    b = click(0.08, (1700, 5200), seed + 1, 0.009)
    gap = np.zeros(int(SR * 0.07))
    return norm(pad(np.concatenate([a, gap, 0.8 * b]), 0.35), 0.55)


def revolver_cock(seed: int) -> np.ndarray:
    a = click(0.06, (1500, 2900), seed, 0.008)
    b = click(0.1, (2700, 4100, 6300), seed + 1, 0.005)
    return norm(pad(np.concatenate([a, np.zeros(int(SR * 0.05)), b]), 0.3), 0.6)


# ------------------------------------------------------------------ the Drawn
# They listen. Their voices are built from the same stuff as radio static: band-limited noise, chopped,
# with a throat underneath. Nothing animal is sampled.


def static_chatter(seconds: float, seed: int, rate: float, lo: float, hi: float) -> np.ndarray:
    rnd = np.random.default_rng(seed)
    n = int(SR * seconds)
    gate = np.zeros(n)
    pos = 0
    while pos < n:
        length = int(SR * rnd.uniform(0.01, 0.06))
        if rnd.random() < 0.55:
            gate[pos:pos + length] = rnd.uniform(0.3, 1.0)
        pos += length + int(SR * rnd.uniform(0.0, 1.0 / rate))
    gate = low(gate, 120, 2)
    return band(noise(seconds, seed + 7), lo, hi) * gate


def throat(seconds: float, f0: float, glide: float, seed: int) -> np.ndarray:
    tt = t(seconds)
    f = f0 * (1 + glide * tt / seconds) * (1 + 0.03 * np.sin(2 * np.pi * 5.3 * tt))
    phase = 2 * np.pi * np.cumsum(f) / SR
    saw = signal.sawtooth(phase)
    voiced = band(saw, 180, 1400, 2) + 0.4 * band(saw, 2200, 3200, 2)
    return voiced * (0.7 + 0.3 * low(noise(seconds, seed), 8, 1) / 3)


def drawn_ambient(seed: int) -> np.ndarray:
    s = 2.4
    x = 0.9 * static_chatter(s, seed, 9, 900, 4800) + 0.35 * throat(s, 62 + seed % 5 * 6, -0.15, seed)
    x *= env(int(SR * s), 0.25, 1.4)
    return norm(x, 0.7)


def drawn_alert(seed: int) -> np.ndarray:
    s = 1.1
    tt = t(s)
    sweep = np.sin(2 * np.pi * np.cumsum(500 + 2600 * (tt / s) ** 1.6) / SR)
    x = 0.6 * band(sweep * (1 + high(noise(s, seed), 3000)), 600, 6000) + 0.7 * static_chatter(s, seed + 1, 30, 1500, 7000)
    x += 0.4 * throat(s, 95, 0.8, seed + 2)
    x *= env(int(SR * s), 0.04, 0.45)
    return norm(pad(x + 0.25 * tail(x, 0.9, seed + 3, 2500)[: len(x)], 1.4))


def drawn_hurt(seed: int) -> np.ndarray:
    s = 0.45
    x = 0.8 * static_chatter(s, seed, 60, 1200, 6500) + 0.8 * throat(s, 140, -0.4, seed + 1)
    x *= env(int(SR * s), 0.005, 0.12)
    return norm(x)


def drawn_death(seed: int) -> np.ndarray:
    s = 1.8
    x = 0.8 * throat(s, 110, -0.7, seed) + 0.6 * static_chatter(s, seed + 1, 14, 700, 5200)
    x *= env(int(SR * s), 0.01, 0.55)
    return norm(x + 0.3 * tail(x, 1.2, seed + 2, 1600)[: len(x)])


# ------------------------------------------------------------------ catalogue

SOUNDS = {
    "revolver_shot": (revolver_shot, 3, "subtitles.signal.revolver_shot"),
    "revolver_dry": (revolver_dry, 1, "subtitles.signal.revolver_dry"),
    "revolver_load": (revolver_load, 2, "subtitles.signal.revolver_load"),
    "revolver_cock": (revolver_cock, 1, "subtitles.signal.revolver_cock"),
    "drawn_ambient": (drawn_ambient, 3, "subtitles.signal.drawn_ambient"),
    "drawn_alert": (drawn_alert, 2, "subtitles.signal.drawn_alert"),
    "drawn_hurt": (drawn_hurt, 2, "subtitles.signal.drawn_hurt"),
    "drawn_death": (drawn_death, 1, "subtitles.signal.drawn_death"),
}


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    catalogue = {}
    for name, (fn, variants, subtitle) in SOUNDS.items():
        files = []
        for v in range(variants):
            seed = (zlib_hash(name) + v * 101) % 2**31
            audio = fn(seed).astype(np.float32)
            rel = f"{name}{v + 1 if variants > 1 else ''}"
            sf.write(OUT / f"{rel}.ogg", audio, SR, format="OGG", subtype="VORBIS")
            files.append(f"signal:{rel}")
        catalogue[name] = {"subtitle": subtitle, "sounds": files}
    (ROOT / "src/main/resources/assets/signal/sounds.json").write_text(json.dumps(catalogue, indent=2) + "\n")
    print(f"{sum(v[1] for v in SOUNDS.values())} sounds written to {OUT}")


def zlib_hash(s: str) -> int:
    import zlib
    return zlib.crc32(s.encode())


if __name__ == "__main__":
    main()
