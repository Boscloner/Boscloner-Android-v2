package com.boscloner.bosclonerv2.util.permissions_fragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;

import com.boscloner.bosclonerv2.R;
import com.boscloner.bosclonerv2.di.Injectable;

public class PermissionsFragment extends Fragment implements Injectable {

    private static final String PERMISSIONS_KEY = "permission_key";
    private static final String SHOULD_RETRY_KEY = "should_retry_key";
    private static final String RETRY_MESSAGE_KEY = "should_retry_message_key";
    private static final String SHOULD_FORCE_SETTINGS = "should_force_settings";
    private static final String FORCE_SETTINGS_MESSAGE_KEY = "should_force_settings_message";
    private static final String REQUEST_CODE_KEY = "request_code_key";
    private final int PERMISSION_REQUEST_CODE = 11;

    private Context context;
    private PermissionGrantedCallback listener;

    private boolean shouldResolve;
    private boolean shouldRetry;
    private boolean retryDialogShown;
    private boolean externalGrantNeeded;
    private boolean externalGrantDialogShown;

    private String[] permissions;
    private boolean shouldRetryFromBuilder;
    private String retryMessage;
    private boolean shouldForceSettingsFromBuilder;
    private String forceSettingsMessage;
    private int requestCode;

    public static PermissionsFragment newInstance(@NonNull String[] permissions,
                                                  boolean shouldRetry,
                                                  @NonNull String retryMessage,
                                                  boolean shouldForceAppSettings,
                                                  @NonNull String forceSettingsMessage,
                                                  int requestCode) {
        Bundle bundle = new Bundle();
        bundle.putStringArray(PERMISSIONS_KEY, permissions);
        bundle.putBoolean(SHOULD_RETRY_KEY, shouldRetry);
        bundle.putString(RETRY_MESSAGE_KEY, retryMessage);
        bundle.putBoolean(SHOULD_FORCE_SETTINGS, shouldForceAppSettings);
        bundle.putString(FORCE_SETTINGS_MESSAGE_KEY, forceSettingsMessage);
        bundle.putInt(REQUEST_CODE_KEY, requestCode);
        PermissionsFragment fragment = new PermissionsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof PermissionGrantedCallback) {
            listener = (PermissionGrantedCallback) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle bundle = getArguments();
        if (bundle != null) {
            permissions = bundle.getStringArray(PERMISSIONS_KEY);
            shouldRetryFromBuilder = bundle.getBoolean(SHOULD_RETRY_KEY);
            retryMessage = bundle.getString(RETRY_MESSAGE_KEY);
            shouldForceSettingsFromBuilder = bundle.getBoolean(SHOULD_FORCE_SETTINGS);
            forceSettingsMessage = bundle.getString(FORCE_SETTINGS_MESSAGE_KEY);
            requestCode = bundle.getInt(REQUEST_CODE_KEY);
        }
        requestNecessaryPermissions();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (shouldResolve) {
            if (externalGrantNeeded && !externalGrantDialogShown) {
                showAppSettingsDialog();
                externalGrantDialogShown = true;
            } else if (shouldRetry && !retryDialogShown) {
                showRetryDialog();
                retryDialogShown = true;
            } else {
                //permissions have been accepted
                if (listener != null) {
                    listener.onPermissionGranted(requestCode);
                }
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        context = null;
        listener = null;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            shouldResolve = true;
            shouldRetry = false;

            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (!shouldShowRequestPermissionRationale(permission) &&
                        grantResult != PackageManager.PERMISSION_GRANTED) {
                    externalGrantNeeded = shouldForceSettingsFromBuilder;
                    if (!shouldForceSettingsFromBuilder && listener != null) {
                        listener.onPermissionDenied(this.requestCode);
                    }
                    return;
                } else if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    shouldRetry = shouldRetryFromBuilder;
                    if (!shouldRetryFromBuilder && listener != null) {
                        listener.onPermissionDenied(this.requestCode);
                    }
                    return;
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestNecessaryPermissions() {
        requestPermissions(permissions, PERMISSION_REQUEST_CODE);
    }

    private void showAppSettingsDialog() {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.MyAlertDialogTheme))
                .setTitle("Permissions Required")
                .setMessage(forceSettingsMessage)
                .setCancelable(false)
                .setPositiveButton("App Settings", (dialogInterface, i) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package",
                            context.getApplicationContext().getPackageName(), null);
                    intent.setData(uri);
                    context.startActivity(intent);
                    externalGrantDialogShown = false;
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                    if (listener != null) {
                        listener.onPermissionDenied(requestCode);
                    }
                    externalGrantDialogShown = false;
                }).create().show();
    }

    private void showRetryDialog() {
        new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.MyAlertDialogTheme))
                .setTitle("Permissions Declined")
                .setCancelable(false)
                .setMessage(retryMessage)
                .setPositiveButton("Retry", (dialogInterface, i) -> {
                            requestNecessaryPermissions();
                            retryDialogShown = false;
                        }
                )
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                    if (listener != null) {
                        listener.onPermissionDenied(requestCode);
                    }
                    retryDialogShown = false;
                }).create().show();
    }

    public interface PermissionGrantedCallback {
        void onPermissionGranted(int requestCode);

        void onPermissionDenied(int requestCode);
    }
}