package com.boscloner.bosclonerv2;

public interface Constants {

    String DEVICE_NAME = "DSD TECH";

    String CLONE = "$!CLONE,0102030405?$";
    String DISABLE_CLONE = "$!DISABLE_CLONE?$";
    String ENABLE_CLONE = "$!ENABLE_CLONE?$";

    interface Action {
        String MAIN_ACTION = "main_action";
        String STOPFOREGROUND_ACTION = "stopforeground_action";
        String PERMISSION_RESULT_ACTION = "permission_result_action";
        String PERMISSION_RESULT_DATA = "permission_result_data";
        String AUTO_CLONE_ACTION = "auto_clone_action";
        String AUTO_CLONE_DATA = "auto_clone_data";
        String WRITE_MAC_ADDRESS = "write_mac_address";
        String WRITE_MAC_ADDRESS_DATA = "write_mac_address_data";
        String WRITE_MAC_ADDRESS_HISTORY = "write_mac_address_history";
    }

    interface NotificationId {
        int FOREGROUND_SERVICE = 42;
        String CHANNEL_ID = "boscloner_channel";
    }

    interface Preferences {
        String AUTO_CLONE_KEY = "preferences_auto_clone_key";
        String RFID_BADGE_TYPE = "preferences_rfid_badge_type_key";
    }
}
