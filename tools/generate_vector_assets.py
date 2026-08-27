"""Generate editable SVG bases for the live parametric vector renderer."""
from pathlib import Path

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

def tower_svg(name, main, tier):
    accent = "#FFD166" if tier >= 4 else "#F7E9FF"
    left = 22 - min(tier, 5); right = 78 + min(tier, 5)
    ornaments = []
    if tier >= 2: ornaments.append('<path d="M20 58H8V75H25M80 58H92V75H75" fill="none" stroke="#F7E9FF" stroke-width="4"/>')
    if tier >= 4: ornaments.append('<path d="M31 25L22 8 38 18M69 25L78 8 62 18" fill="none" stroke="#FFD166" stroke-width="4"/>')
    if tier >= 6: ornaments.append('<path d="M14 42L3 34M86 42L97 34M18 84L7 93M82 84L93 93" stroke="#98E36E" stroke-width="4" stroke-linecap="round"/>')
    if tier >= 8: ornaments.append('<circle cx="50" cy="6" r="5" fill="#FFFFFF"/><path d="M50 0V12M44 6H56" stroke="#FFFFFF" stroke-width="2"/>')
    body = f'<path d="M{left} 88 L28 30 L42 18 L50 {7 - min(tier, 5)} L58 18 L72 30 L{right} 88 Z" fill="{main}" stroke="#F7E9FF" stroke-width="3"/><circle cx="50" cy="36" r="{14 + tier//3}" fill="{accent}"/><path d="M34 70H66M38 55H62" stroke="#25152F" stroke-width="5"/>{"".join(ornaments)}'
    (ROOT / f"{name}.svg").write_text(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><g>{body}</g></svg>\n')

def enemy_svg(name, main, variant):
    accent = "#FF476F" if variant >= 4 else "#F7E9FF"
    armor = ''.join(f'<path d="M{22+i*8} 22L{18+i*8} 10" stroke="{accent}" stroke-width="3"/>' for i in range(min(variant, 5)))
    body = f'<circle cx="50" cy="51" r="31" fill="{main}" stroke="#F7E9FF" stroke-width="3"/><circle cx="61" cy="44" r="5" fill="{accent}"/><path d="M28 77L20 91M72 77L80 91" stroke="{accent}" stroke-width="7" stroke-linecap="round"/>{armor}'
    (ROOT / f"{name}.svg").write_text(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><g>{body}</g></svg>\n')

for family, base in towers.items():
    for tier in range(1, 11): tower_svg(f"tower_{family}_tier_{tier:02d}", base, tier)
for family, base in enemies.items():
    for variant in range(3 if family in ("mini_boss", "boss") else 9): enemy_svg(f"enemy_{family}_variant_{variant:02d}", base, variant)
