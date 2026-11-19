package com.google.android.datatransport.runtime;

import android.content.Context;
import com.google.android.datatransport.TransportFactory;

// Stand-in for Google telemetry (datatransport), which Reel Block does not ship. See build.sh.
// The billing library calls initialize() inside a catch-all; this throw makes it log
// "Skipping logging since initialization failed" and send nothing.
public final class TransportRuntime {
    private TransportRuntime() {}

    public static void initialize(Context c) {
        throw new UnsupportedOperationException("Reel Block sends no telemetry");
    }

    public static TransportRuntime getInstance() {
        throw new UnsupportedOperationException("Reel Block sends no telemetry");
    }

    public TransportFactory newFactory(Destination d) {
        throw new UnsupportedOperationException("Reel Block sends no telemetry");
    }
}
