package com.boscloner.bosclonerv2.history;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.boscloner.bosclonerv2.NavigationController;
import com.boscloner.bosclonerv2.R;

import javax.inject.Inject;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Inject
    NavigationController navigationController;

    public SettingsFragment() {
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }
}