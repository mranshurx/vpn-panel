#!/usr/bin/env python3
import os, sys, glob
from pathlib import Path

LOG_DIR = "vpn_logged"
OUT_DIR = "vpn_manipulated"

def hex_to_bytes(h: str) -> bytes:
    return bytes.fromhex(h)

def bytes_to_hex(b: bytes) -> str:
    return b.hex()

def apply_transform(payload_hex: str) -> str:
    payload = hex_to_bytes(payload_hex)
    # LAB TEMPLATE: Insert exact byte mutations discovered from hex analysis.
    # Example: flip a marker pattern for tracking/manipulation.
    if b"\xde\xad" in payload:
        payload = payload.replace(b"\xde\xad", b"\xbe\xef")
    return bytes_to_hex(payload)

def main():
    Path(OUT_DIR).mkdir(exist_ok=True)
    files = glob.glob(os.path.join(LOG_DIR, "*.hex"))
    if not files:
        print("[!] No .hex files in", LOG_DIR)
        sys.exit(1)
    for f in files:
        raw_hex = Path(f).read_text().strip()
        modified = apply_transform(raw_hex)
        out_path = os.path.join(OUT_DIR, Path(f).name)
        Path(out_path).write_text(modified)
        print(f"[+] {f} -> {out_path} ({len(raw_hex)//2} bytes)")

if __name__ == "__main__":
    main()
