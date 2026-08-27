"""Create the three coherent, looping audio stems used by GameAudio.
No third-party samples: every note/drum is synthesized and written as 16-bit WAV.
"""
from pathlib import Path
import math, wave
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/src/main/res/raw'; OUT.mkdir(parents=True, exist_ok=True)
SR = 22050; SECONDS = 24; N = SR * SECONDS

def write(name, samples):
    samples = np.clip(samples, -1, 1)
    with wave.open(str(OUT/name), 'wb') as f:
        f.setnchannels(1); f.setsampwidth(2); f.setframerate(SR)
        f.writeframes((samples * 32767).astype(np.int16).tobytes())

def tone(freq, length, start=0, amp=.2, decay=2.0):
    n=max(1,int(length*SR)); t=np.arange(n)/SR
    phase=2*np.pi*freq*t
    return (np.sin(phase)+.22*np.sin(phase*2)+.08*np.sin(phase*3))*amp*np.exp(-decay*t)

def add_note(out, freq, start, length=.4, amp=.15):
    a=int(start*SR); b=min(N,a+int(length*SR)); x=tone(freq,(b-a)/SR,amp=amp,decay=1.6)
    out[a:b]+=x[:b-a]

def base_stem():
    out=np.zeros(N); bpm=72; beat=60/bpm
    # D-minor ostinato and a warm low drone.
    notes=[146.83,174.61,220.00,261.63,293.66,261.63,220.00,174.61]
    for i in range(int(SECONDS/beat)):
        add_note(out, notes[i%len(notes)], i*beat, beat*.9, .12)
    out += .025*np.sin(2*np.pi*73*np.arange(N)/SR)
    return out

def tension_stem():
    out=np.zeros(N); bpm=96; beat=60/bpm
    notes=[293.66,349.23,440.0,523.25,440.0,349.23]
    for i in range(int(SECONDS/beat)):
        add_note(out, notes[i%len(notes)], i*beat, beat*.42, .095)
        # soft heartbeat/percussion layer
        a=int(i*beat*SR); b=min(N,a+int(.07*SR)); t=np.arange(b-a)/SR
        out[a:b]+=.12*np.sin(2*np.pi*92*t)*np.exp(-30*t)
    return out

def boss_stem():
    out=np.zeros(N); bpm=84; beat=60/bpm
    notes=[73.42,73.42,87.31,65.41]
    for i in range(int(SECONDS/beat)):
        add_note(out, notes[i%4], i*beat, beat*.8, .18)
        if i%4 in (0,2):
            a=int(i*beat*SR); b=min(N,a+int(.16*SR)); t=np.arange(b-a)/SR
            out[a:b]+=.18*np.sin(2*np.pi*48*t)*np.exp(-12*t)
    return out

for name, data in [('music_base.wav',base_stem()),('music_tension.wav',tension_stem()),('music_boss.wav',boss_stem())]:
    write(name, data)
    print(name, f'{len(data)/SR:.1f}s', f'{Path(OUT/name).stat().st_size/1024:.0f}KB')
