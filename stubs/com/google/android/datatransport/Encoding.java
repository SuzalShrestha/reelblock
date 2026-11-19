package com.google.android.datatransport;

// Stand-in for Google telemetry (datatransport), which Reel Block does not ship. See build.sh.
public final class Encoding {
    private Encoding() {}

    public static Encoding of(String name) {
        throw new UnsupportedOperationException("Reel Block sends no telemetry");
    }
}
