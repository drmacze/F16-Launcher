#!/usr/bin/env python3
"""
Generate cinematic splash sound WAV for DLavie Launcher.

Layers:
  - 110Hz (A2) drone (main body, with slight detune for warmth)
  - 220Hz (A3) warmth
  - 440Hz (A4) pad (slow vibrato)
  - 880Hz (A5) sparkle (only first 1s, very low amplitude)
Envelope: attack 300ms (quadratic), sustain 500ms, exponential release 1500ms.
Master gain 0.3 to leave headroom.
Simple one-pole lowpass filter for warmer overall tone.
Total duration: 2.3 seconds.

Output: app/src/main/res/raw/splash_sound.wav (16-bit PCM mono, 44100 Hz).
"""
import struct, math, wave, os, sys

SAMPLE_RATE = 44100
DURATION = 2.3
total_samples = int(SAMPLE_RATE * DURATION)

# Cinematic sound: 110Hz drone + 220Hz warmth + 440Hz pad + 880Hz sparkle
samples = []
for i in range(total_samples):
    t = i / SAMPLE_RATE

    # Envelope: attack 0.3s, sustain to 0.8s, release to end
    if t < 0.3:
        env = (t / 0.3) ** 2  # quadratic attack
    elif t < 0.8:
        env = 1.0  # sustain
    else:
        release_progress = (t - 0.8) / (DURATION - 0.8)
        env = math.exp(-release_progress * 3.0)  # exponential release

    drone = math.sin(2 * math.pi * 110 * t) * 0.5
    drone_detune = math.sin(2 * math.pi * 110 * 1.005 * t) * 0.3
    warmth = math.sin(2 * math.pi * 220 * t) * 0.25
    vibrato = 1.0 + 0.005 * math.sin(2 * math.pi * 4.0 * t)
    pad = math.sin(2 * math.pi * 440 * t * vibrato) * 0.15
    sparkle_env = math.exp(-t * 2.0) if t < 1.0 else 0.0
    sparkle = math.sin(2 * math.pi * 880 * t) * 0.08 * sparkle_env

    sample = (drone + drone_detune + warmth + pad + sparkle) * env * 0.3
    samples.append(max(-1.0, min(1.0, sample)))

# Simple one-pole lowpass filter
filtered = []
prev = 0.0
coeff = 0.85
for s in samples:
    prev = prev * coeff + s * (1 - coeff)
    filtered.append(max(-1.0, min(1.0, prev)))

# Write WAV
out_path = sys.argv[1] if len(sys.argv) > 1 else 'app/src/main/res/raw/splash_sound.wav'
os.makedirs(os.path.dirname(out_path), exist_ok=True)
with wave.open(out_path, 'w') as w:
    w.setnchannels(1)
    w.setsampwidth(2)  # 16-bit
    w.setframerate(SAMPLE_RATE)
    for s in filtered:
        w.writeframes(struct.pack('<h', int(s * 32767)))

size = os.path.getsize(out_path)
print(f"WAV generated: {out_path}")
print(f"  Duration: {DURATION}s, Sample rate: {SAMPLE_RATE}Hz, Channels: 1, 16-bit PCM")
print(f"  Size: {size} bytes ({size/1024:.1f} KB)")
print(f"  Samples: {len(filtered)}")
