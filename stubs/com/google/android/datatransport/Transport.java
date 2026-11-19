package com.google.android.datatransport;

// Stand-in for Google telemetry (datatransport), which Reel Block does not ship. See build.sh.
public interface Transport<T> {
    void send(Event<T> event);
}
