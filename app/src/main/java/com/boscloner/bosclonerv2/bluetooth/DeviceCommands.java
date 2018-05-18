package com.boscloner.bosclonerv2.bluetooth;

public enum DeviceCommands {
    SCAN("SCAN"),
    CLONE("CLONE"),
    STATUS_MCU("STATUS,MCU"),
    END_DELIMITER("?$");

    private String value;

    DeviceCommands(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}