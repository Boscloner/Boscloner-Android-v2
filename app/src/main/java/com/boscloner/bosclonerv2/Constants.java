package com.boscloner.bosclonerv2;

public class Constants {
    public interface Action {
        String MAIN_ACTION = "main_action";
        String INIT_ACTION = "init_action";
        String STARTFOREGROUND_ACTION = "startforeground_action";
        String STOPFOREGROUND_ACTION = "stopforeground_action";
    }

    public interface NotificationId {
        int FOREGROUND_SERVICE = 42;
        String CHANNEL_ID = "boscloner_channel";
    }
}
