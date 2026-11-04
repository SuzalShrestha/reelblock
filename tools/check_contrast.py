#!/usr/bin/env python3
"""Checks every colour pair of the design system (docs/design-system.md, "Contrast pairs") for WCAG contrast.

    python3 tools/check_contrast.py        # exits 1 if any pair fails

Text needs 4.5:1, large text, icons and the edges of controls need 3:1. Three colour sets are checked:
  1. the fixed brand tokens (res/values/colors.xml, res/values-night/colors.xml), resolved through
     colors_semantic.xml exactly like Android does;
  2. Android 12-13 dynamic colour (values-v31): each role is a wallpaper shade. The shade number fixes its
     tone (tone = 100 - shade/10), and tone is CIE lightness L*, so the contrast follows from the tones
     whatever the wallpaper hue is;
  3. Android 14+ dynamic colour (values-v34): the system gives the Material 3 roles, with the M3 tones below.
No third-party packages: standard library only.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "res")

# (foreground, background, minimum ratio, where it is used). Names are colour tokens.
PAIRS = [
    ("text_primary", "bg_window", 4.5, "body text, app bar wordmark"),
    ("text_primary", "bg_card", 4.5, "card and row titles, switch label"),
    ("text_primary", "bg_dialog", 4.5, "dialog title and options"),
    ("text_secondary", "bg_window", 4.5, "onboarding body, version"),
    ("text_secondary", "bg_card", 4.5, "row supporting text, chart labels"),
    ("text_secondary", "bg_dialog", 4.5, "dialog body"),
    ("primary", "bg_window", 4.5, "section headers, text buttons"),
    ("primary", "bg_dialog", 4.5, "dialog Cancel"),
    ("primary", "bg_card", 4.5, "chart 'today' label"),
    ("on_primary", "primary", 4.5, "filled button label"),
    ("on_secondary_container", "secondary_container", 4.5, "tonal button label, row icons"),
    ("on_primary_container", "primary_container", 4.5, "step numbers"),
    ("hero_on_fg", "hero_on_bg", 4.5, "hero text: on"),
    ("hero_paused_fg", "hero_paused_bg", 4.5, "hero text: paused"),
    ("hero_off_fg", "hero_off_bg", 4.5, "hero text: off"),
    ("hero_problem_fg", "hero_problem_bg", 4.5, "hero text and its text button: stopped / Instagram changed"),
    ("on_error_container", "error_container", 4.5, "self-check problem icon"),
    ("hero_on_badge_icon", "hero_on_badge", 3.0, "hero icon: on"),
    ("hero_paused_badge_icon", "hero_paused_badge", 3.0, "hero icon: paused"),
    ("hero_off_badge_icon", "hero_off_badge", 3.0, "hero icon: off"),
    ("hero_problem_badge_icon", "hero_problem_badge", 3.0, "hero icon: problem"),
    ("icon_default", "bg_card", 3.0, "chevrons, dialog option icons"),
    ("icon_default", "bg_dialog", 3.0, "dialog option icons"),
    ("primary", "bg_card", 3.0, "disclosure 'can' icons, switch track (on) against the switch bar"),
    ("success", "bg_card", 3.0, "disclosure 'never' icons"),
    ("chart_bar", "bg_card", 3.0, "chart bars"),
    ("chart_bar_today", "bg_card", 3.0, "chart bar today"),
    ("on_primary", "primary", 3.0, "switch thumb (on)"),
    ("outline", "surface_container_highest", 3.0, "switch thumb (off) on its track"),
    ("outline", "bg_card", 3.0, "switch track edge (off) against the switch bar"),
    ("outline", "bg_card", 3.0, "outlined button edge"),
    ("outline", "bg_window", 3.0, "outlined button edge on the window"),
    ("primary", "bg_window", 3.0, "page indicator (current page)"),
    ("brand_fg", "brand_bg_bottom", 3.0, "logo: card on teal"),
    ("brand_glyph", "brand_fg", 3.0, "logo: pause bars on the card"),
]

# Material 3 tones per role (light, dark), used for the Android 14+ system role colours.
M3_TONES = {
    "primary": (40, 80), "on_primary": (100, 20), "primary_container": (90, 30), "on_primary_container": (30, 90),
    "secondary": (40, 80), "on_secondary": (100, 20), "secondary_container": (90, 30), "on_secondary_container": (30, 90),
    "tertiary": (40, 80), "on_tertiary": (100, 20), "tertiary_container": (90, 30), "on_tertiary_container": (30, 90),
    "surface": (98, 6), "surface_dim": (87, 6), "surface_bright": (98, 24),
    "surface_container_lowest": (100, 4), "surface_container_low": (96, 10), "surface_container": (94, 12),
    "surface_container_high": (92, 17), "surface_container_highest": (90, 22),
    "on_surface": (10, 90), "on_surface_variant": (30, 80), "outline": (50, 60), "outline_variant": (80, 30),
}


def load(folder):
    path = os.path.join(RES, folder)
    out = {}
    if not os.path.isdir(path):
        return out
    for f in sorted(os.listdir(path)):
        if f.startswith("colors") and f.endswith(".xml"):
            for c in ET.parse(os.path.join(path, f)).getroot().iter("color"):
                out[c.get("name")] = c.text.strip()
    return out


def hex_to_rgb(v):
    v = v.lstrip("#")
    if len(v) == 8:
        if v[:2].upper() != "FF":
            raise ValueError("translucent colour #%s cannot be checked alone" % v)
        v = v[2:]
    return tuple(int(v[i:i + 2], 16) / 255 for i in (0, 2, 4))


def luminance_rgb(rgb):
    lin = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in rgb]
    return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]


def luminance_tone(tone):
    # CIE L* (= HCT tone) to relative luminance Y.
    f = (tone + 16) / 116
    return f ** 3 if f ** 3 > 216 / 24389 else tone / (24389 / 27)


def ratio(a, b):
    hi, lo = max(a, b), min(a, b)
    return (hi + 0.05) / (lo + 0.05)


class Resolver:
    """Resolves a colour name for one configuration, like Android picks the most specific folder."""

    def __init__(self, folders, night, api):
        self.layers = [load(f) for f in folders]  # most specific first
        self.night, self.api = night, api

    def raw(self, name):
        for layer in self.layers:
            if name in layer:
                return layer[name]
        raise KeyError(name)

    def lum(self, name, seen=()):
        v = self.raw(name)
        if v.startswith("@color/"):
            return self.lum(v[7:], seen + (name,))
        m = re.match(r"@android:color/system_(accent[123]|neutral[12])_(\d+)$", v)
        if m:
            return luminance_tone(100 - int(m.group(2)) / 10)
        m = re.match(r"@android:color/system_([a-z_]+)_(light|dark)$", v)
        if m:
            role = m.group(1)
            if role not in M3_TONES:
                raise KeyError("no M3 tone for system role " + role)
            return luminance_tone(M3_TONES[role][1 if m.group(2) == "dark" else 0])
        if v.startswith("#"):
            return luminance_rgb(hex_to_rgb(v))
        raise ValueError("%s = %s: unknown colour value" % (name, v))


CONFIGS = [
    ("light", Resolver(["values"], False, 26)),
    ("dark", Resolver(["values-night", "values"], True, 26)),
    ("light, Android 12-13 dynamic", Resolver(["values-v31", "values"], False, 31)),
    ("dark, Android 12-13 dynamic", Resolver(["values-night-v31", "values-v31", "values-night", "values"], True, 31)),
    ("light, Android 14+ dynamic", Resolver(["values-v34", "values-v31", "values"], False, 34)),
    ("dark, Android 14+ dynamic", Resolver(["values-night-v34", "values-night-v31", "values-v34", "values-v31",
                                             "values-night", "values"], True, 34)),
]


def main():
    failures = 0
    for label, r in CONFIGS:
        print("== %s" % label)
        for fg, bg, need, use in PAIRS:
            got = ratio(r.lum(fg), r.lum(bg))
            ok = got + 1e-9 >= need
            failures += not ok
            print("  %s %5.2f:1 (needs %.1f)  %s on %s  [%s]" % ("ok  " if ok else "FAIL", got, need, fg, bg, use))
    print("\n%d pairs x %d colour sets: %s" % (len(PAIRS), len(CONFIGS), "all pass" if not failures else "%d FAIL" % failures))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
