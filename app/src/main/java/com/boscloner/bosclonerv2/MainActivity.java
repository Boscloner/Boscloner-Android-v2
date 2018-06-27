package com.boscloner.bosclonerv2;

import android.Manifest;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.boscloner.bosclonerv2.history.HistoryFragment;
import com.boscloner.bosclonerv2.room.BosclonerDatabase;
import com.boscloner.bosclonerv2.room.HistoryItem;
import com.boscloner.bosclonerv2.util.AppExecutors;
import com.boscloner.bosclonerv2.util.permissions_fragment.PermissionsFragment;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements HasSupportFragmentInjector, PermissionsFragment.PermissionGrantedCallback, HistoryFragment.OnListFragmentInteractionListener {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 42;

    @Inject
    NavigationController navigationController;

    @Inject
    DispatchingAndroidInjector<Fragment> dispatchingAndroidInjector;

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    @Inject
    AppExecutors appExecutors;

    @Inject
    BosclonerDatabase bosclonerDatabase;

    private SharedViewModel sharedViewModel;

    private Snackbar snackbar;

    private BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener
            = (item) -> {
        switch (item.getItemId()) {
            case R.id.navigation_home:
                navigationController.navigateToHomeFragment(this);
                return true;
            case R.id.navigation_history:
                navigationController.navigateToHistoryFragment(this);
                return true;
            case R.id.navigation_settings:
                navigationController.navigateToSettingsFragment(this);
                return true;
        }
        return false;
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent i) {
            String action = i.getAction();
            if (action != null) {
                switch (i.getAction()) {
                    case ForegroundService.NO_PERMISSION_BROADCAST: {
                        Timber.d("No permission for the location");
                        onPermissionGranted(LOCATION_PERMISSION_REQUEST_CODE);
                        break;
                    }
                    case ForegroundService.STOP_SELF:
                        finish();
                        break;
                    case ForegroundService.UI_UPDATE_BROADCAST:
                        ForegroundService.ConnectionState state = (ForegroundService.ConnectionState)
                                i.getSerializableExtra(ForegroundService.UI_UPDATE_BROADCAST_KEY);
                        onUiUpdated(state);
                        break;
                }
            }
        }
    };

    private void onUiUpdated(ForegroundService.ConnectionState state) {
        sharedViewModel.setConnectionState(state);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerServiceBroadcastReceiver();
        sharedViewModel = ViewModelProviders.of(this, viewModelFactory).get(SharedViewModel.class);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView navigationView = findViewById(R.id.bottom_navigation_view);
        navigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        View coordinator = findViewById(R.id.coordinator);
        snackbar = Snackbar.make(coordinator, "Permission needed",
                Snackbar.LENGTH_INDEFINITE)
                .setAction("Grant", v -> onPermissionGranted(LOCATION_PERMISSION_REQUEST_CODE));

        Intent service = new Intent(MainActivity.this, ForegroundService.class);
        startService(service);

        sharedViewModel.getHomeFragmentActionsMutableLiveData().observe(this, homeFragmentActions -> {
            if (homeFragmentActions != null) {
                switch (homeFragmentActions) {
                    case CUSTOM_WRITE:
                        showInputDialog();
                        break;
                    case AUTO_CLONE_ON:
                        sendAutoCloneToTheIntentService(true);
                        break;
                    case AUTO_CLONE_OFF:
                        sendAutoCloneToTheIntentService(false);
                        break;
                }
            }
        });

        navigationController.navigateToHomeFragment(this);
    }

    private void registerServiceBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ForegroundService.NO_PERMISSION_BROADCAST);
        filter.addAction(ForegroundService.STOP_SELF);
        filter.addAction(ForegroundService.UI_UPDATE_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPermissionGranted(int requestCode) {
        if (isPermissionGranted()) {
            snackbar.dismiss();
            navigationController.removePermissionFragment(this);
            sendPermissionResultToService(true);
        } else {
            navigationController.navigateToPermissionFragment(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    getString(R.string.permission_retry_message),
                    getString(R.string.permission_request_rationale),
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onPermissionDenied(int requestCode) {
        snackbar.show();
        navigationController.removePermissionFragment(this);
        sendPermissionResultToService(false);
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return dispatchingAndroidInjector;
    }

    private void sendPermissionResultToService(boolean permissionGranted) {
        Intent service = new Intent(MainActivity.this, ForegroundService.class);
        service.setAction(Constants.Action.PERMISSION_RESULT_ACTION);
        service.putExtra(Constants.Action.PERMISSION_RESULT_DATA, permissionGranted);
        startService(service);
    }

    private void showInputDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.input)
                .content(R.string.input_content)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(R.string.input_hint, R.string.input_prefill, (dialog, input) -> {
                    Timber.d("user input %s", input);
                    sendWriteInstructionToService(input.toString(), false);
                }).show();
    }

    private void sendAutoCloneToTheIntentService(boolean isChecked) {
        Intent service = new Intent(MainActivity.this, ForegroundService.class);
        service.setAction(Constants.Action.AUTO_CLONE_ACTION);
        service.putExtra(Constants.Action.AUTO_CLONE_DATA, isChecked);
        startService(service);
    }

    private void sendWriteInstructionToService(String input, boolean history) {
        Intent service = new Intent(MainActivity.this, ForegroundService.class);
        service.setAction(Constants.Action.WRITE_MAC_ADDRESS);
        service.putExtra(Constants.Action.WRITE_MAC_ADDRESS_DATA, input);
        service.putExtra(Constants.Action.WRITE_MAC_ADDRESS_HISTORY, history);
        startService(service);
    }

    @Override
    public void onListFragmentInteraction(HistoryItem item) {
        Timber.d("On item selected " + item.deviceMacAddress + " " + item.localDateTime);
        sendWriteInstructionToService(item.deviceMacAddress.replaceAll(":", ""), true);
    }

    @Override
    public void shareItems() {
        appExecutors.diskIO().execute(() -> {
            String macAddresses = bosclonerDatabase.historyItemDao().deviceMacAddresses();
            appExecutors.mainThread().execute(() -> {
                shareMacAddresses(macAddresses);
            });
        });
    }

    private void shareMacAddresses(String macAddresses) {
        if (macAddresses == null || macAddresses.isEmpty()) {
            Toast.makeText(this, R.string.nothing_to_share, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, macAddresses);
        sendIntent.setType("text/plain");
        if (sendIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(sendIntent);
        } else {
            Toast.makeText(this, R.string.no_app_that_can_share, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void clearHistory() {
        showDeleteHistoryDialog();
    }

    private void showDeleteHistoryDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.delete_history)
                .content(R.string.delete_history_dialog_content)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    clearHistoryDatabase();
                })
                .show();
    }

    private void clearHistoryDatabase() {
        appExecutors.diskIO().execute(() -> {
            bosclonerDatabase.historyItemDao().clearTable();
        });
    }
}