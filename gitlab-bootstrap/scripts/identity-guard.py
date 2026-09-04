#!/usr/bin/env python3
"""CLI wrapper — implementation is identity_guard.py (importable)."""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from identity_guard import main

if __name__ == "__main__":
    sys.exit(main())
