#!/usr/bin/env python3
"""Write the empty GameTest structure templates used by SIGNAL's integration tests.

The templates contain no blocks: they only reserve a test volume of the given size (the GameTest
framework clears the volume before each test). Minimal gzip-compressed NBT writer, no dependencies.

Usage (from the project root):  python3 tools/gen_test_structures.py
"""
import gzip
import os
import struct

DATA_VERSION_1_21_1 = 3955
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "signal", "structure")

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def name(n):
    b = n.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def t_int(n, v):
    return bytes([TAG_INT]) + name(n) + struct.pack(">i", v)


def t_string(n, v):
    return bytes([TAG_STRING]) + name(n) + name(v)


def t_list(n, elem_type, payloads):
    return bytes([TAG_LIST]) + name(n) + bytes([elem_type]) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def compound_payload(*tags):
    return b"".join(tags) + bytes([TAG_END])


def template(size):
    root = compound_payload(
        t_int("DataVersion", DATA_VERSION_1_21_1),
        t_list("size", TAG_INT, [struct.pack(">i", v) for v in size]),
        t_list("palette", TAG_COMPOUND, [compound_payload(t_string("Name", "minecraft:air"))]),
        t_list("blocks", TAG_END, []),
        t_list("entities", TAG_END, []),
    )
    return bytes([TAG_COMPOUND]) + name("") + root


def write(file, size):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, file)
    with gzip.GzipFile(path, "wb", mtime=0) as f:
        f.write(template(size))
    print("wrote", os.path.normpath(path), size)


if __name__ == "__main__":
    write("empty_5.nbt", (5, 5, 5))
    # Full Bellwether site on synthetic terrain: 176 x 72 x 144.
    write("bellwether_site.nbt", (176, 72, 144))
