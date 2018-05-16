package com.boscloner.bosclonerv2.history;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.boscloner.bosclonerv2.R;

public class SettingsFragment extends PreferenceFragment {

    public SettingsFragment() {
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}