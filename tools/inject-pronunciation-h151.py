#!/usr/bin/env python3
"""Inject H151 into full (large) root index.html, preserving all original bytes.

The existing document lacks closing </body>, so install a deferred script in
the verified <head> instead of trying to reconstruct truncated application data.
"""
from pathlib import Path

path = Path("index.html")
content = path.read_text(encoding="utf-8")
snippet = '<script defer src="./pronunciation-h151.js?v=h151-20261121"></script>'

if 'pronunciation-h151.js' in content:
    print("Already installed; no changes")
elif "</head>" not in content or '<head>' not in content:
    raise SystemExit("Missing well-formed <head>, refusing patch")
elif 'data-inline-source="voice-manager.js"' not in content:
    raise SystemExit("Missing original MultiVoiceManager")
elif len(content) < 1_000_000:
    raise SystemExit("Unexpected root source length; refusing patch")
else:
    patched = content.replace("</head>", "    " + snippet + "\n</head>", 1)
    if patched.count("pronunciation-h151.js") != 1:
        raise SystemExit("Unexpected duplicate H151")
    if patched[len(content.split("</head>", 1)[0])+len("    "+snippet+"\n</head>"):] != content.split("</head>",1)[1]:
        raise SystemExit("Unexpected change outside document head")
    path.write_text(patched, encoding="utf-8")
    print("H151 installed in head; original body untouched", len(patched), "characters")
    if "</body>" not in content:
        print("NOTICE: original root HTML already lacks </body>. No automatic repair attempted.")
