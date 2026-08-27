"""CI-friendly validation for the editable vector sprite library."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / 'app/src/main/assets/svg'
towers = ['archer', 'cannon', 'frost', 'fire', 'lightning', 'sky_archer', 'arcane']
enemies = ['grunt', 'wolf', 'bat', 'skeleton', 'spider', 'ogre', 'wraith', 'imp']
missing=[]; parsed=0
for name in towers:
    for tier in range(1,11):
        p=ROOT/f'tower_{name}_tier_{tier:02d}.svg'
        if not p.exists(): missing.append(str(p)); continue
        ET.parse(p); parsed += 1
for name in enemies:
    for variant in range(9):
        p=ROOT/f'enemy_{name}_variant_{variant:02d}.svg'
        if not p.exists(): missing.append(str(p)); continue
        ET.parse(p); parsed += 1
for name in ['mini_boss','boss']:
    for variant in range(3):
        p=ROOT/f'enemy_{name}_variant_{variant:02d}.svg'
        if not p.exists(): missing.append(str(p)); continue
        ET.parse(p); parsed += 1
if missing: raise SystemExit('missing SVG assets:\n'+'\n'.join(missing))
print(f'PASS: parsed {parsed} SVG sprite bases')
