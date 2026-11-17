#!/usr/bin/env python3
# Make the five Workshop Core connected texture tiles from one bordered texture.
# usage: ctm-tiles.py <texture.png> <out-dir> [border-px]
# Writes 0.png (as given), 1.png (no border), 2.png (top and bottom border),
# 3.png (left and right border), 4.png (corner pixels only). Copies a .mcmeta next to each tile if present.
import shutil
import sys
from pathlib import Path

from PIL import Image


def strip(im, b, rows, cols):
    w, h = im.size
    out = im.copy()
    px, src = out.load(), im.load()
    for y in range(h):
        for x in range(w):
            sx, sy = x, y
            if cols and (x < b or x >= w - b):
                sx = b if x < b else w - b - 1
            if rows and (y < b or y >= h - b):
                sy = b if y < b else h - b - 1
            if not rows and (y < b or y >= h - b):
                continue
            if not cols and (x < b or x >= w - b):
                continue
            px[x, y] = src[sx, sy]
    return out


def main():
    if len(sys.argv) < 3:
        sys.exit(__doc__ or "usage: ctm-tiles.py <texture.png> <out-dir> [border-px]")
    src = Path(sys.argv[1])
    out = Path(sys.argv[2])
    b = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    im = Image.open(src).convert("RGBA")
    w, h = im.size
    out.mkdir(parents=True, exist_ok=True)
    none = strip(im, b, True, True)
    corners = none.copy()
    for x0 in (0, w - b):
        for y0 in (0, h - b):
            corners.paste(im.crop((x0, y0, x0 + b, y0 + b)), (x0, y0))
    tiles = [im, none, strip(im, b, False, True), strip(im, b, True, False), corners]
    meta = src.with_name(src.name + ".mcmeta")
    for i, t in enumerate(tiles):
        t.save(out / f"{i}.png")
        if meta.exists():
            shutil.copy(meta, out / f"{i}.png.mcmeta")
    print(f"wrote 5 tiles to {out}")


if __name__ == "__main__":
    main()
