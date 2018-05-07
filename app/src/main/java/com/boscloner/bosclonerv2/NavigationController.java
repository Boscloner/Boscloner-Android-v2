package com.boscloner.bosclonerv2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.boscloner.bosclonerv2.util.permissions_fragment.PermissionsFragmentBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NavigationController {

    public static final String PERMISSION_FRAGMENT_TAG = "permissions_fragment_tag";

    @Inject
    public NavigationController() {
    }

    public void navigateToPermissionFragment(FragmentActivity fragmentActivity,
                                             @NonNull String[] permissions,
                                             @Nullable String retryMessage,
                                             @Nullable String forceSettingMessage,
                                             int requestCode) {
        FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(PERMISSION_FRAGMENT_TAG);
        if (fragment == null) {
            PermissionsFragmentBuilder builder = new PermissionsFragmentBuilder(permissions);
            builder.requestCode(requestCode);
            if (retryMessage != null) {
                if (forceSettingMessage != null) {
                    builder.shouldForceAppSetting(forceSettingMessage, retryMessage);
                }
                builder.shouldRetry(retryMessage);
            }
            fragment = builder.build();
            fragmentActivity.getSupportFragmentManager().beginTransaction()
                    .add(fragment, PERMISSION_FRAGMENT_TAG)
                    .commit();
        }
    }

    public void removePermissionFragment(@NonNull FragmentActivity fragmentActivity) {
        FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(PERMISSION_FRAGMENT_TAG);
        if (fragment != null) {
            fragmentActivity.getSupportFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss();
        }
    }
}
