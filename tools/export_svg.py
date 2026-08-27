#!/usr/bin/env python3
"""
Export the SVG source files of برج شیطانی.

The single source of truth for the game art is `SvgSprites.kt` (object `SvgPaths`), because the
game engine parses those path strings at runtime with Compose's `PathParser`. This script mirrors
every path into a real, standalone `.svg` file under `app/src/main/assets/svg/` so the art can be
opened and edited in Inkscape / Figma / Illustrator and pasted back into `SvgSprites.kt`.

Usage:
    python3 tools/export_svg.py
"""
from __future__ import annotations

import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KT = os.path.join(ROOT, "app/src/main/java/ir/hadipoor/eviltower/game/render/SvgSprites.kt")
OUT = os.path.join(ROOT, "app/src/main/assets/svg")

# Rough colour hints so the exported files are readable in a browser/vector editor.
COLORS = {
    "HERO": "#8A93B5", "SERPENT": "#7A8B6B", "BAT": "#35254D", "SKELETON": "#E8E2D0",
    "WOLF": "#2E2242", "GUARDIAN": "#6B2F3E", "LORD": "#2E1B4C", "TRAP": "#C9CEDD",
    "COIN": "#FFC93C", "GEM": "#4FD6FF", "HEART": "#C8203C", "SHIELD": "#4F7FD6",
    "WINGS": "#E8E2D0", "SPEED": "#FFE45C", "MAGNET": "#D6444F", "BRICK": "#4A4159",
    "TORCH": "#FF7A18", "FOG": "#B39CFF", "GATE": "#1A1526", "TOWER": "#5B3E8C",
    "SKULL": "#E8E2D0",
}

# Sprites that get a looping SMIL animation in the exported file (documentation of intent).
SPIN = {"TRAP_BLADE", "TRAP_BLADE_CORE", "COIN_BODY", "COIN_INNER"}
PULSE = {"TORCH_FLAME", "TRAP_FIRE", "TRAP_FIRE_CORE", "HEART_BODY", "TRAP_GAS", "FOG_BLOB"}

CONST_RE = re.compile(r'const\s+val\s+([A-Z0-9_]+)\s*=\s*((?:"(?:[^"\\]|\\.)*"\s*(?:\+\s*)?)+)', re.M)
STRING_RE = re.compile(r'"((?:[^"\\]|\\.)*)"')


def main() -> int:
    with open(KT, encoding="utf-8") as handle:
        source = handle.read()

    os.makedirs(OUT, exist_ok=True)
    written = 0
    for match in CONST_RE.finditer(source):
        name = match.group(1)
        data = "".join(STRING_RE.findall(match.group(2)))
        if not data.strip():
            continue
        prefix = name.split("_")[0]
        color = COLORS.get(prefix, "#B6ADC8")
        anim = ""
        if name in SPIN:
            anim = ('\n      <animateTransform attributeName="transform" type="rotate" '
                    'from="0 50 50" to="360 50 50" dur="1.2s" repeatCount="indefinite"/>')
        elif name in PULSE:
            anim = ('\n      <animateTransform attributeName="transform" type="scale" '
                    'values="1 1;1.06 0.94;1 1" dur="0.9s" additive="sum" repeatCount="indefinite"/>'
                    '\n      <animate attributeName="opacity" values="1;0.82;1" dur="0.9s" '
                    'repeatCount="indefinite"/>')
        svg = (
            '<?xml version="1.0" encoding="UTF-8"?>\n'
            '<!-- برج شیطانی (Evil Tower) sprite: {name}\n'
            '     Source of truth: app/src/main/java/ir/hadipoor/eviltower/game/render/SvgSprites.kt\n'
            '     Regenerate with: python3 tools/export_svg.py -->\n'
            '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">\n'
            '  <g fill="{color}" fill-rule="evenodd">\n'
            '    <path d="{data}">{anim}\n'
            '    </path>\n'
            '  </g>\n'
            '</svg>\n'
        ).format(name=name, color=color, data=data, anim=anim)

        with open(os.path.join(OUT, name.lower() + ".svg"), "w", encoding="utf-8") as out:
            out.write(svg)
        written += 1

    print(f"exported {written} svg files to {os.path.relpath(OUT, ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
