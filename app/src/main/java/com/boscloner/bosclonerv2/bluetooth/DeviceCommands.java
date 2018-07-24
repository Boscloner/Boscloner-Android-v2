package com.boscloner.bosclonerv2.bluetooth;

public enum DeviceCommands {
    CLONE("$!CLONE,%s?$"),
    DISABLE_CLONE("$!DISABLE_CLONE?$"),
    ENABLE_CLONE("$!ENABLE_CLONE?$");

    private String value;

    DeviceCommands(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
