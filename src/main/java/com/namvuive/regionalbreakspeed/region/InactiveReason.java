package com.namvuive.regionalbreakspeed.region;

public enum InactiveReason {

    WORLD_NOT_LOADED("reason-world-not-loaded", "world '%s' is not loaded"),
    WORLDGUARD_MISSING("reason-worldguard-missing", "WorldGuard is not installed"),
    WORLDGUARD_REGION_NOT_FOUND("reason-worldguard-region-not-found", "WorldGuard region '%s' not found in world '%s'");

    private final String messageKey;
    private final String consoleFormat;

    InactiveReason(String messageKey, String consoleFormat) {
        this.messageKey = messageKey;
        this.consoleFormat = consoleFormat;
    }

    public String messageKey() {
        return messageKey;
    }

    public String consoleMessage(Object... args) {
        return String.format(consoleFormat, args);
    }
}
