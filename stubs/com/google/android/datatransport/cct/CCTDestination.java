package com.google.android.datatransport.cct;

import com.google.android.datatransport.runtime.Destination;

// Stand-in for Google telemetry (datatransport), which Reel Block does not ship. See build.sh.
public final class CCTDestination implements Destination {
    public static final CCTDestination INSTANCE = new CCTDestination();

    private CCTDestination() {}
}
