#!/usr/bin/env python3
"""Create a tiny synthetic dump used to validate RecoverX forensic carving."""
from pathlib import Path

out = Path("test-storage-dump.bin")
jpeg = b"\xFF\xD8\xFF" + b"RECOVERX-TEST-JPEG" + b"\xFF\xD9"
png = bytes([0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A]) + b"RECOVERX-TEST-PNG" + bytes([0x49,0x45,0x4E,0x44,0xAE,0x42,0x60,0x82])
out.write_bytes(b"noise" * 100 + jpeg + b"padding" * 200 + png + b"tail")
print(out.resolve())
