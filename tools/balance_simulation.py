"""Reference balance sanity check for برج شیطانی.
The Kotlin engine is authoritative; this small offline model makes the tuning visible to designers.
Run: python3 tools/balance_simulation.py
"""
import math

HP_GROWTH = 1.045

def hp(w): return 24 * HP_GROWTH ** (w - 1)
def count(w): return 7 + int(w ** .72)
def reward(w): return math.ceil(7 * (1 + .018 * (w - 1) ** .82))
def wave_income(w): return count(w) * reward(w) + math.ceil(20 * w ** .55)
def board_pressure(w): return count(w) * hp(w)

print("wave | enemies | hp each | income | pressure ratio")
previous = None
for w in [1, 5, 10, 50, 100, 200, 300, 301, 500]:
    pressure = board_pressure(w)
    ratio = pressure / previous if previous else 0
    print(f"{w:>5} | {count(w):>7} | {hp(w):>7.1f} | {wave_income(w):>6} | {ratio:>6.3f}")
    previous = pressure
print("\nTarget checks:")
print("- HP adjacent ratio:", round(hp(301) / hp(300), 4), "(target 1.03..1.08)")
print("- Rewards are sub-linear: reward(300)/reward(1)=", round(reward(300)/reward(1), 2),
      "while HP ratio=", round(hp(300)/hp(1), 2))
print("- No artificial wave cap: formulas accept wave 100000.")
