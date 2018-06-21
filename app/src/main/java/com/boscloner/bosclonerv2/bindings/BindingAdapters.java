package com.boscloner.bosclonerv2.bindings;

import android.databinding.BindingAdapter;
import android.view.View;

import timber.log.Timber;

/**
 * Data Binding adapters specific to the app.
 */
public class BindingAdapters {

    @BindingAdapter("visibleGone")
    public static void showGone(View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("visibleInvisible")
    public static void showHide(View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    @BindingAdapter("enabledState")
    public static void enabledState(View view, boolean enabled) {
        view.setEnabled(enabled);
    }
}