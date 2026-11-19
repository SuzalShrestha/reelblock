package com.google.android.datatransport;

// Stand-in for Google telemetry (datatransport), which Reel Block does not ship. See build.sh.
public abstract class Event<T> {
    public static <T> Event<T> ofData(T data) {
        throw new UnsupportedOperationException("Reel Block sends no telemetry");
    }
}
