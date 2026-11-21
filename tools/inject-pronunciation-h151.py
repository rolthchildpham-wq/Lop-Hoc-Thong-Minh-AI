#!/usr/bin/env python3
"""Inject the verified H151 enhancement into the complete 1+ MB root HTML.
Never reconstruct index.html from a truncated API response.
"""
from pathlib import Path

path = Path("index.html")
content = path.read_text(encoding="utf-8")
snippet = '<script defer src="./pronunciation-h151.js?v=h151-20261121"></script>'
if 'pronunciation-h151.js' in content:
    print("H151 pronunciation already present; no change")
elif "</body>" not in content:
    raise SystemExit("Missing </body>, refusing partial modification")
elif 'data-inline-source="voice-manager.js"' not in content:
    raise SystemExit("Missing existing voice manager, refusing unsafe modification")
else:
    result = content.replace("</body>", "    " + snippet + "\n</body>", 1)
    if len(result) < 1000000:
        raise SystemExit("Safety check: unexpected root HTML size")
    path.write_text(result, encoding="utf-8")
    print("Patched full root index.html:", len(result), "characters")
