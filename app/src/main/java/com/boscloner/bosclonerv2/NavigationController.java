package com.boscloner.bosclonerv2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.boscloner.bosclonerv2.history.HistoryFragment;
import com.boscloner.bosclonerv2.history.SettingsFragment;
import com.boscloner.bosclonerv2.home.HomeFragment;
import com.boscloner.bosclonerv2.util.permissions_fragment.PermissionsFragmentBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NavigationController {

    public static final String PERMISSION_FRAGMENT_TAG = "permissions_fragment_tag";
    public static final String SETTINGS_FRAGMENT_TAG = "settings_fragment_tag";
    public static final String HISTORY_FRAGMENT_TAG = "history_fragment_tag";
    public static final String MAIN_ACTIVITY_FRAGMENT_TAG = "home_fragment_tag";
    public Fragment activeFragment;
    public Fragment homeFragment;
    public Fragment historyFragment;
    public Fragment settingsFragment;

    @Inject
    public NavigationController() {
        homeFragment = HomeFragment.newInstance();
        historyFragment = HistoryFragment.newInstance();
        settingsFragment = SettingsFragment.newInstance();
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
                    builder.shouldForceAppSetting(forceSettingMessage, forceSettingMessage);
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

    public void navigateToSettingsFragment(@NonNull FragmentActivity activity) {
        navigateToFragment(activity, settingsFragment, SETTINGS_FRAGMENT_TAG);
    }

    public void navigateToHistoryFragment(@NonNull FragmentActivity activity) {
        navigateToFragment(activity, historyFragment, HISTORY_FRAGMENT_TAG);
    }

    public void navigateToHomeFragment(@NonNull FragmentActivity activity) {
        navigateToFragment(activity, homeFragment, MAIN_ACTIVITY_FRAGMENT_TAG);
    }

    public void navigateToFragment(@NonNull FragmentActivity activity, Fragment newFragment, String tag) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment == null) {
            fragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, newFragment, tag)
                    .commit();
        }
        if (activeFragment == null || activeFragment != newFragment) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (activeFragment != null) {
                fragmentTransaction.hide(activeFragment);
            }
            fragmentTransaction.show(newFragment);
            fragmentTransaction.commit();
            activeFragment = newFragment;
        }
    }
}
