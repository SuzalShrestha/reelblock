#!/usr/bin/env python3
"""Fails (exit 1) if a layout or drawable uses a literal size or colour instead of a token.

    python3 tools/check_tokens.py

Tokens live in res/values*/ (colors, dimens, type, motion, styles). Layouts and drawables must reference them:
no "16dp", "14sp", "#FF0000" in res/layout or res/drawable. Vector drawables are the one exception: inside a
<vector>, pathData, viewport sizes, stroke widths and group transforms are in the vector's own units, not dp.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
A = "{http://schemas.android.com/apk/res/android}"
LITERAL = re.compile(r"^-?\d+(\.\d+)?(dp|dip|sp|px|pt|mm|in)$|^#[0-9A-Fa-f]{3,8}$")
VECTOR_UNITS = {"pathData", "viewportWidth", "viewportHeight", "strokeWidth", "translateX", "translateY",
                "scaleX", "scaleY", "pivotX", "pivotY", "rotation", "fillAlpha", "strokeAlpha",
                "startX", "startY", "endX", "endY", "centerX", "centerY", "gradientRadius", "trimPathStart",
                "trimPathEnd", "strokeMiterLimit"}


def check(path, problems):
    root = ET.parse(path).getroot()
    in_vector = root.tag == "vector"
    for el in root.iter():
        for k, v in el.attrib.items():
            name = k.replace(A, "")
            if in_vector and name in VECTOR_UNITS:
                continue
            if LITERAL.match(v.strip()):
                problems.append("%s: <%s %s=\"%s\">" % (os.path.relpath(path, ROOT), el.tag, name, v))


def main():
    problems, files = [], 0
    for folder in sorted(os.listdir(os.path.join(ROOT, "res"))):
        if not (folder.startswith("layout") or folder.startswith("drawable") or folder.startswith("mipmap")):
            continue
        for f in sorted(os.listdir(os.path.join(ROOT, "res", folder))):
            if f.endswith(".xml"):
                files += 1
                check(os.path.join(ROOT, "res", folder, f), problems)
    for p in problems:
        print("literal value, use a token:", p)
    print("%d layout/drawable files checked: %s" % (files, "no literal sizes or colours" if not problems else "%d problems" % len(problems)))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
