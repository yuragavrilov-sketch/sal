package ru.copperside.sal.starter.web;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks the runtime state of the adapter (online/offline).
 * <p>
 * C# origin: {@code TCB.SAL.Common.Configuration.AdapterState}
 * Replaces {@code IsOnline} / {@code ShutDownStateOn} fields in C# AdapterState.
 */
public class AdapterState {

    private final AtomicBoolean online = new AtomicBoolean(false);
    private final AtomicBoolean shutDown = new AtomicBoolean(false);

    public boolean isOnline() {
        return online.get();
    }

    public void setOnline(boolean value) {
        online.set(value);
    }

    public boolean isShutDown() {
        return shutDown.get();
    }

    public void setShutDown(boolean value) {
        shutDown.set(value);
    }
}
