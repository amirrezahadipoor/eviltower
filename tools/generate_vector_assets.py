"""Generate editable, bold SVG bases used by the parametric sprite library.
These are deliberately small vector source files; the live renderer adds level particles/details.
"""
from pathlib import Path
import colorsys

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/assets/svg"
ROOT.mkdir(parents=True, exist_ok=True)

towers = {
    "archer": "#65D6A0", "cannon": "#FF9B54", "frost": "#70D6FF", "fire": "#FF5B4D",
    "lightning": "#FFE36E", "sky_archer": "#C19BFF", "arcane": "#E68CFF",
}
enemies = {
    "grunt": "#98E36E", "wolf": "#7B61A8", "bat": "#B48AFF", "skeleton": "#E5D6B8",
    "spider": "#C65FA2", "ogre": "#71836F", "wraith": "#9B74D2", "imp": "#F15A4A",
    "mini_boss": "#C96B57", "boss": "#B52F5B",
}

def svg(name, main, accent, kind):
    if kind == "tower":
        body = f'<path d="M22 88 L28 30 L42 18 L50 7 L58 18 L72 30 L78 88 Z" fill="{main}" stroke="#F7E9FF" stroke-width="3"/><circle cx="50" cy="36" r="14" fill="{accent}"/><path d="M34 70H66M38 55H62" stroke="#25152F" stroke-width="5"/>'
    else:
        body = f'<circle cx="50" cy="51" r="31" fill="{main}" stroke="#F7E9FF" stroke-width="3"/><circle cx="61" cy="44" r="5" fill="{accent}"/><path d="M28 77L20 91M72 77L80 91" stroke="{accent}" stroke-width="7" stroke-linecap="round"/>'
    Path(ROOT / f"{name}.svg").write_text(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><g>{body}</g></svg>\n')

for family, base in towers.items():
    for tier in range(1, 11):
        hue = (tier - 1) / 12
        accent = "#FFD166" if tier >= 4 else "#F7E9FF"
        svg(f"tower_{family}_tier_{tier:02d}", base, accent, "tower")
for family, base in enemies.items():
    for variant in range(3 if family in ("mini_boss", "boss") else 9):
        accent = "#FF476F" if variant >= 4 else "#F7E9FF"
        svg(f"enemy_{family}_variant_{variant:02d}", base, accent, "enemy")
