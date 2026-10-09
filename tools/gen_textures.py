#!/usr/bin/env python3
"""Generate SIGNAL's original 16x16 block and item textures.

Deterministic (seeded) so the output is reproducible and reviewable. All art is original,
drawn procedurally by this script; see docs/ASSET_PROVENANCE.md.

Usage (from the project root):  python3 tools/gen_textures.py
Requires Pillow.
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "signal", "textures")

# Palette: charcoal, weathered concrete, olive, oxidized brown, dirty cream, subdued amber, limited cool light.
CHAR = (46, 47, 44)
CHAR_D = (32, 33, 31)
STEEL = (82, 84, 78)
STEEL_L = (108, 110, 102)
STEEL_D = (62, 63, 59)
OLIVE = (86, 92, 58)
OLIVE_D = (64, 69, 43)
OLIVE_L = (108, 114, 74)
RUST = (112, 70, 42)
RUST_D = (84, 52, 33)
CREAM = (196, 186, 156)
CREAM_D = (150, 142, 118)
AMBER = (232, 158, 52)
AMBER_L = (255, 206, 120)
AMBER_D = (120, 78, 30)
COOL = (120, 196, 204)
COOL_D = (52, 92, 98)
BLACK = (18, 18, 17)
GLASS = (54, 60, 58)


def img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def jitter(c, rnd, amt=6):
    d = rnd.randint(-amt, amt)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def fill(im, x0, y0, x1, y1, c, rnd=None, amt=5):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), jitter(c, rnd, amt) if rnd else c + (255,) if len(c) == 3 else c)


def px(im, x, y, c):
    im.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def rect(im, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        px(im, x, y0, c)
        px(im, x, y1, c)
    for y in range(y0, y1 + 1):
        px(im, x0, y, c)
        px(im, x1, y, c)


def weathered_panel(seed, base=OLIVE):
    """Painted steel housing with a frame, rivets, localized wear and rust streaks at the lower edge."""
    rnd = random.Random(seed)
    im = img()
    fill(im, 0, 0, 15, 15, base, rnd, 5)
    rect(im, 0, 0, 15, 15, OLIVE_D if base == OLIVE else STEEL_D)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px(im, x, y, STEEL_L)
    # wear on the handled lower area, rust bleeding down from the bottom seam
    for _ in range(10):
        x, y = rnd.randint(1, 14), rnd.randint(9, 14)
        px(im, x, y, jitter(STEEL, rnd, 8))
    for x in (3, 4, 10, 12):
        for y in range(15 - rnd.randint(1, 3), 15):
            px(im, x, y, RUST if rnd.random() < 0.7 else RUST_D)
    return im, rnd


def save(im, rel):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)


def machine_side():
    im, rnd = weathered_panel(11)
    # horizontal stiffener and a stencilled maintenance strip
    for x in range(1, 15):
        px(im, x, 6, OLIVE_D)
        px(im, x, 7, OLIVE_L)
    fill(im, 3, 3, 8, 4, CREAM_D, rnd, 4)
    save(im, "block/machine_side.png")


def machine_top():
    rnd = random.Random(12)
    im = img()
    fill(im, 0, 0, 15, 15, STEEL_D, rnd, 4)
    rect(im, 0, 0, 15, 15, CHAR)
    for y in range(3, 13, 2):
        for x in range(3, 13):
            px(im, x, y, CHAR_D)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px(im, x, y, STEEL_L)
    save(im, "block/machine_top.png")


def generator_front(running):
    im, rnd = weathered_panel(21)
    # louvred grille
    fill(im, 2, 2, 13, 8, CHAR_D)
    for y in range(3, 8, 2):
        for x in range(3, 13):
            px(im, x, y, STEEL if not running else STEEL_L)
    # isolator window: open switch = gap (isolated); closed switch = continuous bar (supplying)
    fill(im, 3, 10, 12, 13, BLACK)
    px(im, 4, 12, CREAM)
    px(im, 11, 12, CREAM)
    if running:
        for x in range(5, 11):
            px(im, x, 12, AMBER_L)
        px(im, 7, 11, AMBER)
        px(im, 8, 11, AMBER)
    else:
        # blade lifted: diagonal from left contact, gap before right contact
        px(im, 5, 12, CREAM_D)
        px(im, 6, 11, CREAM_D)
        px(im, 7, 10, CREAM_D)
    save(im, "block/generator_front_%s.png" % ("running" if running else "isolated"))


def panel_front(fault):
    im, rnd = weathered_panel(31, base=STEEL)
    fill(im, 2, 2, 13, 13, CHAR_D)
    # three breaker rows (pump, local tx, relay) with toggles
    for i, y in enumerate((3, 6, 9)):
        fill(im, 3, y, 9, y + 1, STEEL_D)
        px(im, 4 + i, y, CREAM)
        px(im, 11, y, AMBER_D)
    # main fuse carrier F1 at bottom
    if fault:
        # scorched carrier with a hazard mark (triangle shape, readable without colour)
        fill(im, 3, 12, 12, 12, RUST_D)
        px(im, 10, 11, AMBER)
        px(im, 9, 12, AMBER)
        px(im, 11, 12, AMBER)
        px(im, 4, 12, BLACK)
        px(im, 6, 12, BLACK)
    else:
        fill(im, 3, 12, 12, 12, CREAM_D)
        fill(im, 5, 12, 10, 12, CREAM)
    save(im, "block/distribution_panel_front_%s.png" % ("fault" if fault else "ok"))


def pump_front(level):
    im, rnd = weathered_panel(41)
    # round gauge face
    cx, cy, r = 7.5, 7, 5.6
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                px(im, x, y, CREAM if d < r - 1 else CREAM_D)
    # tick marks
    for x, y in ((3, 9), (4, 4), (7, 2), (11, 4), (12, 9)):
        px(im, x, y, CHAR)
    # needle: 0 = low-left, 3 = high-right
    ends = {0: [(6, 8), (5, 9), (4, 9)], 1: [(7, 6), (6, 5), (6, 4)],
            2: [(8, 6), (9, 5), (9, 4)], 3: [(8, 7), (9, 8), (10, 8), (11, 9)]}
    px(im, 7, 7, BLACK)
    for x, y in ends[level]:
        px(im, x, y, BLACK if level < 3 else RUST_D)
    # flow lamp strip below gauge: number of lit cells mirrors pressure
    for i in range(3):
        fill(im, 3 + i * 4, 14, 4 + i * 4, 14, AMBER if i < level else CHAR_D)
    save(im, "block/pump_controller_front_%d.png" % level)


def relay_front(state):
    im, rnd = weathered_panel(51, base=STEEL)
    fill(im, 2, 2, 13, 9, GLASS)
    rect(im, 2, 2, 13, 9, CHAR_D)
    # carrier trace: flat when off, single bump when powered, full when linked
    trace = {"off": [6] * 10,
             "powered": [6, 6, 6, 5, 4, 5, 6, 6, 6, 6],
             "linked": [6, 4, 7, 3, 8, 3, 7, 4, 6, 5]}[state]
    col = {"off": STEEL_D, "powered": AMBER, "linked": COOL}[state]
    for i, y in enumerate(trace):
        px(im, 3 + i, y, col)
    # signal bars below: 0 / 1 / 3
    bars = {"off": 0, "powered": 1, "linked": 3}[state]
    for i in range(3):
        h = i + 1
        for y in range(14 - h, 14):
            px(im, 5 + i * 3, y, (COOL if state == "linked" else AMBER) if i < bars else CHAR_D)
    save(im, "block/relay_controller_front_%s.png" % state)


def service_lamp(on):
    rnd = random.Random(61)
    im = img()
    fill(im, 0, 0, 15, 15, CHAR, rnd, 3)
    glow = AMBER_L if on else (70, 64, 50)
    mid = AMBER if on else (58, 54, 44)
    for y in range(2, 14):
        for x in range(2, 14):
            d = abs(x - 7.5) + abs(y - 7.5)
            px(im, x, y, glow if d < 4 else mid if d < 8 else (AMBER_D if on else CHAR_D))
    # cage bars (shape cue independent of colour)
    for i in (5, 10):
        for t in range(1, 15):
            px(im, i, t, STEEL_D)
            px(im, t, i, STEEL_D)
    rect(im, 0, 0, 15, 15, STEEL_D)
    if on:
        px(im, 7, 7, (255, 236, 190))
        px(im, 8, 8, (255, 236, 190))
    save(im, "block/service_lamp_%s.png" % ("on" if on else "off"))


def radio_item():
    im = img()
    fill(im, 4, 4, 11, 14, OLIVE)
    rect(im, 4, 4, 11, 14, OLIVE_D)
    for y in range(6, 10):
        for x in range(5, 11, 2):
            px(im, x, y, CHAR_D)
    fill(im, 6, 11, 9, 12, GLASS)
    px(im, 7, 11, AMBER)
    for y in range(0, 4):
        px(im, 10, y, CHAR)
    px(im, 10, 0, STEEL_L)
    px(im, 6, 13, CREAM_D)
    px(im, 9, 13, CREAM_D)
    save(im, "item/handheld_radio.png")


def fuse_item():
    im = img()
    # ceramic body with brass end caps, diagonal
    for i in range(3, 13):
        for w in (-1, 0, 1):
            x, y = i + w, 15 - i
            if 0 <= x < 16 and 0 <= y < 16:
                px(im, x, y, CREAM if w == 0 else CREAM_D)
    for i in (2, 3, 12, 13):
        for w in (-1, 0, 1):
            x, y = i + w, 15 - i
            if 0 <= x < 16 and 0 <= y < 16:
                px(im, x, y, (176, 140, 64) if w <= 0 else (128, 98, 44))
    px(im, 7, 8, RUST_D)
    px(im, 8, 7, RUST_D)
    save(im, "item/replacement_fuse.png")


if __name__ == "__main__":
    machine_side()
    machine_top()
    for r in (False, True):
        generator_front(r)
    for f in (True, False):
        panel_front(f)
    for lv in range(4):
        pump_front(lv)
    for s in ("off", "powered", "linked"):
        relay_front(s)
    for o in (False, True):
        service_lamp(o)
    radio_item()
    fuse_item()
    print("textures written to", os.path.normpath(ROOT))
