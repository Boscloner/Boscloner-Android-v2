package com.boscloner.bosclonerv2.util.permissions_fragment;

import android.support.annotation.NonNull;

public class PermissionsFragmentBuilder {

    private String[] permissions;
    private boolean shouldRetry;
    private String retryMessage;
    private boolean shouldForceAppSettings;
    private String forceSettingsMessage;
    private int requestCode;

    public PermissionsFragmentBuilder(@NonNull String[] permissions) {
        if (permissions.length == 0) {
            throw new IllegalArgumentException("You should ask for at least one permission");
        }
        this.permissions = permissions;
    }

    public PermissionsFragmentBuilder shouldRetry(@NonNull String retryMessage) {
        this.shouldRetry = true;
        this.retryMessage = retryMessage;
        return this;
    }

    public PermissionsFragmentBuilder shouldForceAppSetting(@NonNull String retryMessage,
                                                            @NonNull String forceSettingsMessage) {
        this.shouldForceAppSettings = true;
        this.forceSettingsMessage = forceSettingsMessage;
        return shouldRetry(retryMessage);
    }

    public PermissionsFragmentBuilder requestCode(int requestCode) {
        this.requestCode = requestCode;
        return this;
    }

    public PermissionsFragment build() {
        return PermissionsFragment.newInstance(permissions, shouldRetry, retryMessage,
                shouldForceAppSettings, forceSettingsMessage, requestCode);
    }
}