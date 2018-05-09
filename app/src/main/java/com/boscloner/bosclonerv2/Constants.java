package com.boscloner.bosclonerv2;

public interface Constants {

    String DEVICE_NAME = "DSD TECH";

    interface Action {
        String MAIN_ACTION = "main_action";
        String STOPFOREGROUND_ACTION = "stopforeground_action";
        String PERMISSION_RESULT_ACTION = "permission_result_action";
        String PERMISSION_RESULT_DATA = "permission_result_data";
    }

    interface NotificationId {
        int FOREGROUND_SERVICE = 42;
        String CHANNEL_ID = "boscloner_channel";
    }
}
