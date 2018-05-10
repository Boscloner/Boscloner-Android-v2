package com.boscloner.bosclonerv2.util;

import java.util.HashMap;
import java.util.UUID;

public class SampleGattAttributes {
    public static String BOSCLONER_SERVICE = "82e58b98-4965-487d-8591-46b335d71aea";
    public static String BOSCLONER_WRITE_CHARACTERISTIC = "06942fc2-cf09-447b-a6e2-a0e86d16f3fa";
    public static String BOSCLONER_READ_CHARACTERISTIC = "9038d1e1-66f1-4a31-b660-fbcd2c4931f4";
    public static String NOTIFY_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
    private static HashMap<String, String> attributes = new HashMap<>();

    public static final UUID BOSCLONER_READ_UUID = UUID.fromString(BOSCLONER_READ_CHARACTERISTIC);
    public static final UUID BOSCLONER_WRITE_UUID = UUID.fromString(BOSCLONER_WRITE_CHARACTERISTIC);

    static {
        // Sample Services.
        attributes.put(BOSCLONER_SERVICE, "Boscloner Service");
        // Sample Characteristics.
        attributes.put(BOSCLONER_WRITE_CHARACTERISTIC, "Boscloner write characteristic");
        attributes.put(BOSCLONER_READ_CHARACTERISTIC, "Boscloner read characteristic");
        attributes.put(MANUFACTURER_NAME_STRING, "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
