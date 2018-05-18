package com.boscloner.bosclonerv2;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.boscloner.bosclonerv2.history.HistoryFragment;
import com.boscloner.bosclonerv2.history.dummy.DummyContent;
import com.boscloner.bosclonerv2.util.permissions_fragment.PermissionsFragment;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements HasSupportFragmentInjector, PermissionsFragment.PermissionGrantedCallback, HistoryFragment.OnListFragmentInteractionListener{

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 42;

    @Inject
    NavigationController navigationController;

    @Inject
    DispatchingAndroidInjector<Fragment> dispatchingAndroidInjector;

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

    private BroadcastReceiver noPermissionBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent i) {
            String action = i.getAction();
            if (action != null) {
                switch (i.getAction()) {
                    case ForegroundService.NO_PERMISSION_BROADCAST: {
                        Timber.d("No permission for the location");
                        onPermissionGranted(LOCATION_PERMISSION_REQUEST_CODE);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView navigationView = findViewById(R.id.bottom_navigation_view);
        navigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        TextView textView = findViewById(R.id.text_view_icon_create);
        textView.setOnClickListener(v -> showInputDialog());

        View coordinator = findViewById(R.id.coordinator);
        snackbar = Snackbar.make(coordinator, "Permission needed",
                Snackbar.LENGTH_INDEFINITE)
                .setAction("Grant", v -> onPermissionGranted(LOCATION_PERMISSION_REQUEST_CODE));

        if (!isServiceRunning()) {
            Intent service = new Intent(MainActivity.this, ForegroundService.class);
            startService(service);
        }

        navigationController.navigateToHomeFragment(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ForegroundService.NO_PERMISSION_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(noPermissionBroadcastReceiver,
                filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(noPermissionBroadcastReceiver);
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

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (ForegroundService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
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
                }).show();
    }

    @Override
    public void onListFragmentInteraction(DummyContent.DummyItem item) {

    }
}