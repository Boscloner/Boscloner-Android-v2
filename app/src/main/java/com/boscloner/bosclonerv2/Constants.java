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
    }

    interface NotificationId {
        int FOREGROUND_SERVICE = 42;
        String CHANNEL_ID = "boscloner_channel";
    }
}
