package com.google.android.datatransport;

// Stand-in for Google telemetry (datatransport), which Reel Block does not ship. See build.sh.
public interface TransportFactory {
    <T> Transport<T> getTransport(String name, Class<T> payload, Encoding encoding, Transformer<T, byte[]> transformer);
}
