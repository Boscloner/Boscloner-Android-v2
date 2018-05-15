package com.boscloner.bosclonerv2.util;

import com.boscloner.bosclonerv2.BuildConfig;

import java.util.HashMap;
import java.util.UUID;

public class SampleGattAttributes {
    public static String BOSCLONER_SERVICE = BuildConfig.BOSCLONER_SERVICE;
    public static String BOSCLONER_WRITE_CHARACTERISTIC = BuildConfig.BOSCLONER_WRITE_CHARACTERISTIC;
    public static final UUID BOSCLONER_WRITE_UUID = UUID.fromString(BOSCLONER_WRITE_CHARACTERISTIC);
    public static String BOSCLONER_READ_CHARACTERISTIC = BuildConfig.BOSCLONER_READ_CHARACTERISTIC;
    public static final UUID BOSCLONER_READ_UUID = UUID.fromString(BOSCLONER_READ_CHARACTERISTIC);
    public static String NOTIFY_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
    private static HashMap<String, String> attributes = new HashMap<>();

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