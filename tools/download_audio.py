#!/usr/bin/env python3
"""Download American English MP3 audio from Youdao for all SnapWord dictionary words.
Multi-threaded, stores MP3 directly (MediaPlayer supports it natively)."""

import json
import os
import re
import sys
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

BASE = '/data/data/com.termux/files/home/ccd-workspace/SnapWord'
ASSETS = f'{BASE}/app/src/main/assets'
AUDIO_DIR = f'{ASSETS}/audio'
YAODAO_URL = 'https://dict.youdao.com/dictvoice?audio={word}&type=0'  # type=0=US

def load_base_words():
    with open(f'{ASSETS}/dictionary.json') as f:
        entries = json.load(f)

    base_words = {}
    inflected_map = {}

    for e in entries:
        w = e['w']
        t = e['t']

        # Check if this is an inflected form
        is_inflected = any(kw in t for kw in [
            '的过去式', '的过去分词', '的现在分词', '的第三人称',
            '的复数', '的比较级', '的最高级', '的复数形式',
            '的复数或第三人称单数'
        ])

        if is_inflected:
            # Try "原形：word" first (irregular forms)
            match = re.search(r'原形：(\w+)', t)
            if match:
                inflected_map[w] = match.group(1)
            else:
                # Try "word 的过去式/复数..." pattern (regular forms)
                match = re.match(r'^(\w+)\s+的', t)
                if match:
                    inflected_map[w] = match.group(1)
            continue

        if w not in base_words:
            base_words[w] = t

    return base_words, inflected_map


def download_one(word):
    """Download audio for one word. Returns (word, True/False)."""
    url = YAODAO_URL.format(word=word)
    out_path = f'{AUDIO_DIR}/{word}.mp3'

    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=10) as resp:
            if resp.status != 200:
                return (word, False)
            data = resp.read()
            if len(data) < 500:
                return (word, False)

        with open(out_path, 'wb') as f:
            f.write(data)
        return (word, True)

    except Exception:
        return (word, False)


def main():
    base_words, inflected_map = load_base_words()
    print(f"Base words: {len(base_words)}, Inflected: {len(inflected_map)}", file=sys.stderr)

    os.makedirs(AUDIO_DIR, exist_ok=True)

    # Resume from existing downloads
    existing = {f.stem for f in Path(AUDIO_DIR).glob('*.mp3')}
    to_download = [w for w in base_words if w not in existing]
    print(f"Already have: {len(existing)}, To download: {len(to_download)}", file=sys.stderr)

    if not to_download:
        print("All done!", file=sys.stderr)
        save_index(inflected_map)
        return

    total = len(to_download)
    success = 0
    fail = 0
    start_time = time.time()

    with ThreadPoolExecutor(max_workers=8) as executor:
        futures = {executor.submit(download_one, w): w for w in to_download}

        for i, future in enumerate(as_completed(futures)):
            word, ok = future.result()
            if ok:
                success += 1
            else:
                fail += 1

            done = i + 1
            elapsed = time.time() - start_time
            rate = done / elapsed if elapsed > 0 else 0
            eta = (total - done) / rate if rate > 0 else 0

            if done % 50 == 0 or done == total:
                print(
                    f"[{done/total*100:5.1f}%] {done}/{total} "
                    f"OK={success} FAIL={fail} {rate:.0f}w/s ETA:{eta:.0f}s",
                    file=sys.stderr
                )
                save_index(inflected_map)

    elapsed = time.time() - start_time
    print(f"\nDone in {elapsed:.0f}s! Success: {success}, Fail: {fail}", file=sys.stderr)
    save_index(inflected_map)

    total_size = sum(f.stat().st_size for f in Path(AUDIO_DIR).glob('*.mp3'))
    print(f"Total audio: {total_size/1024/1024:.1f} MB ({len(list(Path(AUDIO_DIR).glob('*.mp3')))} files)", file=sys.stderr)


def save_index(inflected_map):
    with open(f'{AUDIO_DIR}/index.json', 'w') as f:
        json.dump({'inflected_map': inflected_map}, f, ensure_ascii=False)


if __name__ == '__main__':
    main()
