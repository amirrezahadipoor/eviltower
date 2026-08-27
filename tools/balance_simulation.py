"""Deterministic economy/TTK sanity check for برج شیطانی.

This is intentionally conservative: it starts with two towers, buys the remaining ten whenever
there is gold, and spends remaining income on the lowest-level tower. It does not use the inferno
ability. The Kotlin Balance object is the authority; this script mirrors its constants so a designer
can see whether wave 300 is a real target instead of a README claim.
"""
from math import ceil, floor, pow

HP_GROWTH = 1.03
SPEED_GROWTH = 1.0018
TOWER_DAMAGE_GROWTH = 1.07
TOWER_COST_GROWTH = 1.017

# cost, damage, interval, base speed for a representative enemy family
TOWERS = {
    "archer": (90, 16, .62), "cannon": (145, 45, 1.65), "frost": (120, 9, .72),
    "fire": (135, 13, .88), "lightning": (175, 29, 1.20), "sky": (155, 22, .78),
}
BUILD_ORDER = ["archer", "frost", "cannon", "fire", "lightning", "sky", "archer", "frost", "cannon", "fire"]

def hp(w): return 24 * HP_GROWTH ** (w - 1)
def count(w): return 7 + floor(w ** .72)
def variant(w): return 1 + .12 * floor((w - 1) / 40)
def reward(w): return ceil(13 * (1 + .028 * max(w - 1, 0) ** .78))
def clear_reward(w): return ceil(28 * w ** .58)
def tower_cost(name, level): return ceil(TOWERS[name][0] * TOWER_COST_GROWTH ** (level - 1))
def tower_dps(name, level): return TOWERS[name][1] * TOWER_DAMAGE_GROWTH ** (level - 1) / max(.18, TOWERS[name][2] * .992 ** (level - 1))
def regular_speed(w): return .03 * SPEED_GROWTH ** (w - 1)

gold = 520
# two towers are available at the first prep window.
towers = [("archer", 1), ("frost", 1)]
rows = []
for w in range(1, 301):
    n = count(w)
    gold += n * reward(w) + clear_reward(w)
    while len(towers) < 12:
        name = BUILD_ORDER[len(towers) - 2]
        if gold < tower_cost(name, 1): break
        gold -= tower_cost(name, 1); towers.append((name, 1))
    while True:
        index = min(range(len(towers)), key=lambda i: towers[i][1])
        name, level = towers[index]
        price = tower_cost(name, level + 1)
        if level >= 100 or gold < price: break
        gold -= price; towers[index] = (name, level + 1)

    dps = sum(tower_dps(name, level) for name, level in towers)
    # Enemies enter over spawn time, then have one path travel window. This is a more useful
    # playable pressure measure than pretending all enemies spawn on the same frame.
    window = n * .52 + 1 / max(.001, regular_speed(w))
    regular_time = n * hp(w) * variant(w) / max(1, dps)
    threat_ratio = regular_time / window
    if w % 10 == 0:
        boss_hp = hp(w) * (7 + w / 90) * variant(w) * 1.18
        boss_window = 1 / max(.001, .01 * SPEED_GROWTH ** (w - 1)) + 12
        threat_ratio = max(threat_ratio, boss_hp / max(1, dps) / boss_window)
    if w in (1, 50, 100, 200, 300):
        rows.append((w, len(towers), sum(level for _, level in towers) / len(towers), gold, round(dps), round(threat_ratio, 3)))

print("wave | towers | avg level | gold reserve | DPS | threat ratio")
for row in rows: print(f"{row[0]:>4} | {row[1]:>7} | {row[2]:>9.1f} | {row[3]:>12} | {row[4]:>7} | {row[5]:>12}")
print("\nchecks:")
print("HP adjacent ratio:", round(hp(301) / hp(300), 4), "(target 1.03..1.08)")
print("HP growth wave 1 -> 300:", round(hp(300) / hp(1), 1), "x")
print("reward growth wave 1 -> 300:", round(reward(300) / reward(1), 1), "x")
print("wave 300 threat ratio:", rows[-1][-1], "(<= 1 means the conservative model can clear inside the path window)")
assert 1.03 <= hp(301) / hp(300) <= 1.08
assert rows[-1][-1] <= 1.0
assert reward(300) / reward(1) < hp(300) / hp(1)
print("PASS")
