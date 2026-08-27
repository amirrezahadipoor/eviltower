#!/usr/bin/env python3
"""
Generate every sound of برج شیطانی procedurally (no third-party assets, no licensing worries).

Writes 16-bit mono WAV files into `app/src/main/res/raw/`:
  * 13 short SFX loaded by SoundPool
  * one looping dark-fantasy music bed played with MediaPlayer

Usage:
    python3 tools/generate_audio.py
"""
from __future__ import annotations

import math
import os
import struct
import wave

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app/src/main/res/raw")
SR = 22050

rng = np.random.default_rng(20260827)


def t(dur):
    return np.linspace(0, dur, int(SR * dur), endpoint=False)


def env(x, attack=0.01, release=0.2, power=2.0):
    n = len(x)
    a = max(1, int(attack * SR))
    r = max(1, int(release * SR))
    e = np.ones(n)
    e[:a] = np.linspace(0, 1, a)
    if r < n:
        e[n - r:] = np.linspace(1, 0, r) ** power
    return x * e


def square(freq, dur, duty=0.5):
    x = t(dur)
    phase = np.cumsum(np.full_like(x, 1.0)) * (freq / SR) if np.isscalar(freq) else np.cumsum(freq) / SR
    return np.where((phase % 1.0) < duty, 1.0, -1.0)


def tone(freq_start, freq_end, dur, kind="square", duty=0.5):
    x = t(dur)
    freq = np.linspace(freq_start, freq_end, len(x))
    phase = np.cumsum(freq) / SR
    if kind == "square":
        return np.where((phase % 1.0) < duty, 1.0, -1.0)
    if kind == "saw":
        return 2.0 * (phase % 1.0) - 1.0
    if kind == "tri":
        return 2.0 * np.abs(2.0 * (phase % 1.0) - 1.0) - 1.0
    return np.sin(2 * np.pi * phase)


def noise(dur):
    return rng.uniform(-1, 1, int(SR * dur))


def lowpass(x, alpha=0.15):
    out = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += alpha * (v - acc)
        out[i] = acc
    return out


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def write(name, data, gain=0.6):
    peak = np.max(np.abs(data)) or 1.0
    data = (data / peak) * gain
    pcm = np.clip(data * 32767, -32768, 32767).astype("<i2")
    path = os.path.join(OUT, name + ".wav")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    print(f"  {name}.wav  {len(pcm)/SR:.2f}s  {os.path.getsize(path)//1024}KB")


# --------------------------------------------------------------------------------------- SFX
def sfx():
    write("sfx_jump", env(tone(240, 660, 0.18, "square", 0.35), 0.005, 0.12))
    write("sfx_double_jump", env(tone(380, 980, 0.22, "tri") * (1 + 0.2 * np.sin(2 * np.pi * 18 * t(0.22))), 0.005, 0.15))
    write("sfx_land", env(mix(lowpass(noise(0.12), 0.08) * 0.8, tone(150, 60, 0.12, "sine")), 0.002, 0.09))
    write("sfx_coin", env(mix(
        env(tone(988, 988, 0.05, "square", 0.25), 0.002, 0.03),
        np.concatenate([np.zeros(int(SR * 0.05)), env(tone(1319, 1319, 0.10, "square", 0.25), 0.002, 0.08)]),
    ), 0.002, 0.06))
    gem = mix(*[
        np.concatenate([np.zeros(int(SR * i * 0.05)), env(tone(f, f, 0.28, "sine"), 0.004, 0.24)])
        for i, f in enumerate([1046, 1318, 1568, 2093])
    ])
    write("sfx_gem", env(gem, 0.004, 0.2))
    write("sfx_powerup", env(mix(
        tone(330, 1320, 0.35, "square", 0.3) * 0.6,
        tone(660, 2640, 0.35, "tri") * 0.3,
    ), 0.01, 0.2))
    write("sfx_hit", env(mix(noise(0.22) * 0.7, tone(320, 70, 0.22, "saw") * 0.8), 0.002, 0.18))
    write("sfx_enemy_death", env(mix(
        tone(420, 90, 0.32, "square", 0.4) * 0.7,
        lowpass(noise(0.32), 0.3) * 0.5,
    ), 0.003, 0.26))
    swoosh = lowpass(noise(0.16), 0.5) * np.linspace(1, 0.2, int(SR * 0.16))
    write("sfx_attack", env(swoosh + 0.25 * tone(900, 300, 0.16, "tri"), 0.002, 0.12))
    write("sfx_crumble", env(lowpass(noise(0.45), 0.05) * np.linspace(1, 0.1, int(SR * 0.45)), 0.005, 0.35))
    write("sfx_fire", env(mix(lowpass(noise(0.5), 0.12), 0.2 * tone(120, 80, 0.5, "saw")), 0.02, 0.3))
    roar = mix(
        tone(90, 62, 1.1, "saw") * (1 + 0.35 * np.sin(2 * np.pi * 6.5 * t(1.1))),
        lowpass(noise(1.1), 0.05) * 0.6,
        tone(180, 120, 1.1, "square", 0.3) * 0.25,
    )
    write("sfx_boss_roar", env(roar, 0.05, 0.5), gain=0.75)
    fanfare = mix(*[
        np.concatenate([np.zeros(int(SR * i * 0.16)), env(tone(f, f, 0.9, "square", 0.35), 0.01, 0.6)])
        for i, f in enumerate([523, 659, 784, 1046])
    ])
    write("sfx_victory", env(fanfare, 0.01, 0.5))
    fall = mix(
        tone(900, 70, 1.4, "saw") * 0.7,
        lowpass(noise(1.4), 0.03) * 0.5,
    )
    write("sfx_fall", env(fall, 0.01, 0.6))


# ------------------------------------------------------------------------------------- MUSIC
def music():
    """A 32-second seamless loop: minor drone, plucked arpeggio, heartbeat drum."""
    bpm = 84.0
    beat = 60.0 / bpm
    bars = 8
    total = beat * 4 * bars
    x = t(total)
    out = np.zeros(len(x))

    # low drone (A1 + E2), slow tremolo
    drone = (
        0.5 * np.sin(2 * np.pi * 55.0 * x)
        + 0.3 * np.sin(2 * np.pi * 82.4 * x)
        + 0.12 * np.sin(2 * np.pi * 110.0 * x)
    )
    out += drone * (0.55 + 0.15 * np.sin(2 * np.pi * 0.12 * x))

    # A natural-minor arpeggio, one note per eighth
    scale = [220.0, 261.6, 293.7, 329.6, 392.0, 440.0, 523.3, 587.3]
    pattern = [0, 2, 4, 2, 5, 4, 2, 0, 3, 4, 5, 4, 2, 1, 0, 2]
    step = beat / 2
    for i in range(int(total / step)):
        f = scale[pattern[i % len(pattern)]]
        if (i // len(pattern)) % 2 == 1:
            f *= 0.5
        note = env(tone(f, f, step * 1.8, "tri"), 0.006, step * 1.4, power=2.5)
        harmonic = 0.25 * env(tone(f * 2, f * 2, step * 1.8, "sine"), 0.006, step)
        note = note + harmonic[: len(note)]
        start = int(i * step * SR)
        end = min(len(out), start + len(note))
        out[start:end] += 0.28 * note[: end - start]

    # heartbeat drum on 1 and 3
    for barn in range(bars * 4):
        for off in (0.0, 0.22):
            start = int((barn * beat + off) * SR)
            hit = env(mix(tone(120, 45, 0.2, "sine"), lowpass(noise(0.2), 0.06) * 0.35), 0.002, 0.16)
            end = min(len(out), start + len(hit))
            if start < len(out):
                out[start:end] += 0.45 * hit[: end - start]

    # distant wind
    out += lowpass(noise(total), 0.006)[: len(out)] * 0.25

    # crossfade the tail into the head so the loop is seamless
    fade = int(SR * 1.2)
    head = out[:fade].copy()
    ramp = np.linspace(0, 1, fade)
    out[-fade:] = out[-fade:] * (1 - ramp) + head * ramp
    write("music_tower", out, gain=0.55)


def main():
    os.makedirs(OUT, exist_ok=True)
    print("generating SFX...")
    sfx()
    print("generating music...")
    music()
    print("done ->", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
