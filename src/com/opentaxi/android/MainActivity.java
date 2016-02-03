package com.opentaxi.android;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.*;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.opentaxi.android.asynctask.LogoutTask;
import com.opentaxi.android.fragments.*;
import com.opentaxi.android.service.CoordinatesService;
import com.opentaxi.android.utils.AppPreferences;
import com.opentaxi.models.CoordinatesLight;
import com.opentaxi.models.MapRequest;
import com.opentaxi.models.NewCRequestDetails;
import com.opentaxi.models.Users;
import com.opentaxi.rest.RestClient;
import com.stil.generated.mysql.tables.pojos.Cars;
import de.greenrobot.event.EventBus;
import it.sephiroth.android.library.tooltip.Tooltip;
import org.acra.ACRA;
import org.androidannotations.annotations.*;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

@EActivity(R.layout.main)
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnCommandListener {

    private static final int REQUEST_USER_PASS_CODE = 10;
    public static final int HELP = 11;
    public static final int SERVER_CHANGE = 12;

    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;

    @ViewById(R.id.toolbar)
    Toolbar toolbar;

    @ViewById(R.id.drawer_layout)
    DrawerLayout drawer;

    @ViewById(R.id.nav_view)
    NavigationView navigationView;

    @ViewById(R.id.fab)
    FloatingActionButton fab;

    private static final String TAG = "MainActivity";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private ReactiveLocationProvider locationProvider;

    private Observable<Location> lastKnownLocationObservable;
    private Observable<Location> locationUpdatesObservable;
    private Subscription lastKnownLocationSubscription;
    private Subscription updatableLocationSubscription;
    private static final int REQUEST_CHECK_SETTINGS = 0;

    private float SUFFICIENT_ACCURACY = 300; //meters
    private long UPDATE_LOCATION_INTERVAL = 5000; //millis
    private long FASTEST_LOCATION_INTERVAL = 10000; //millis


    //private boolean havePlayService = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //LayoutInflaterCompat.setFactory(getLayoutInflater(), new IconicsLayoutInflater(getDelegate()));

        super.onCreate(savedInstanceState);

        // Tell the activity we have menu items to contribute to the toolbar
        //setHasOptionsMenu(true);

        try {
            if (playServicesConnected()) {

                locationProvider = new ReactiveLocationProvider(getApplicationContext());

                lastKnownLocationObservable = locationProvider.getLastKnownLocation();

                final LocationRequest locationRequest = LocationRequest.create() //standard GMS LocationRequest
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(UPDATE_LOCATION_INTERVAL)
                        .setFastestInterval(FASTEST_LOCATION_INTERVAL);

                locationUpdatesObservable = locationProvider.checkLocationSettings(
                        new LocationSettingsRequest.Builder()
                                .addLocationRequest(locationRequest)
                                //.setAlwaysShow(true)
                                .build()
                )
                        .doOnNext(new Action1<LocationSettingsResult>() {
                            @Override
                            public void call(LocationSettingsResult locationSettingsResult) {
                                LocationSettingsStates locationSettingsStates = locationSettingsResult.getLocationSettingsStates();
                                if (locationSettingsStates != null && (!locationSettingsStates.isGpsPresent() || !locationSettingsStates.isGpsUsable())) {

                                    Status status = locationSettingsResult.getStatus();
                                    if (status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                                        try {
                                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                        } catch (IntentSender.SendIntentException th) {
                                            Log.e(TAG, "Error opening settings activity.", th);
                                        }
                                    }
                                }
                            }
                        })
                        .flatMap(new Func1<LocationSettingsResult, Observable<Location>>() {
                            @Override
                            public Observable<Location> call(LocationSettingsResult locationSettingsResult) {
                                return locationProvider.getUpdatedLocation(locationRequest);
                            }
                        });
            }
        } catch (IllegalStateException e) {
            Log.e("stilActivity", "IllegalStateException", e);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return;
        } else {
            android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() == 1) {
                //fm.popBackStackImmediate();
                supportFinishAfterTransition();
                return;
            }
        }

        super.onBackPressed();
    }

    android.support.v4.app.Fragment redirectFragment = null;

    @Override
    protected void onNewIntent(Intent newIntent) {
        Bundle extras = newIntent.getExtras();
        if (extras != null) {
            MapRequest mapRequest = extras.getParcelable("mapRequest");
            if (mapRequest != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("mapRequest", mapRequest);
                redirectFragment = NewRequestFragment_.builder().build();
                redirectFragment.setArguments(bundle);
            }
        }
        super.onNewIntent(newIntent);
    }

    /**
     * Called when the activity is first created.
     */
    @AfterViews
    void afterMain() {

        setSupportActionBar(toolbar);

        mDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                closeKeyboard();
            }

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }
        };
        drawer.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        Menu navMenu = navigationView.getMenu();
        if (navMenu != null) {
            MenuItem navHome = navMenu.findItem(R.id.nav_home);
            if (navHome != null)
                navHome.setIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_home).actionBar().color(Color.BLACK));

            MenuItem navMap = navMenu.findItem(R.id.nav_map);
            if (navMap != null)
                navMap.setIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_map).actionBar().color(Color.BLUE));

            MenuItem navRequest = navMenu.findItem(R.id.nav_request);
            if (navRequest != null)
                navRequest.setIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_local_taxi).actionBar().color(Color.RED));

            MenuItem navHistory = navMenu.findItem(R.id.nav_history);
            if (navHistory != null)
                navHistory.setIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_history).actionBar().color(Color.GREEN));

            MenuItem navServers = navMenu.findItem(R.id.nav_servers);
            if (navServers != null)
                navServers.setIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_cloud).actionBar().color(Color.BLUE));

            MenuItem navLog = navMenu.findItem(R.id.nav_send_log);
            if (navLog != null)
                navLog.setIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_bug_report).actionBar().color(Color.RED));

            MenuItem navExit = navMenu.findItem(R.id.nav_exit);
            if (navExit != null)
                navExit.setIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_exit_to_app).actionBar().color(Color.BLACK));
        }

        fab.setIconDrawable(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_local_taxi).sizeDp(35).color(Color.GREEN));

        AppPreferences appPreferences = AppPreferences.getInstance(this);
        RestClient.getInstance().setSocketsType(appPreferences.getSocketType());
        //checkUser();
        //playServicesConnected();

        if (!checkUserLogin()) checkFbLogin();
    }

    /*private void checkUser() {
        if (AppPreferences.getInstance() != null) {
            String user = RestClient.getInstance().getUsername();
            String pass = RestClient.getInstance().getPassword();

            if (user == null || user.equals("") || pass == null || pass.equals("")) {
                com.stil.generated.mysql.tables.pojos.Users users = AppPreferences.getInstance().getUsers();
                if (users != null) {
                    String username = users.getUsername();
                    String password = users.getPassword();

                    if (username == null || password == null) {
                        beforeStartUserPass();
                    } else {
                        RestClient.getInstance().setAuthHeadersEncoded(username, password);
                        afterLogin(username);
                    }
                } else beforeStartUserPass();
            } else {
                String username = AppPreferences.getInstance().decrypt(user, "user_salt");
                String password = AppPreferences.getInstance().decrypt(pass, username);

                Log.i(TAG, "checkUser username:" + username + " password:" + password);

                if (username == null || password == null) {
                    beforeStartUserPass();
                } else {
                    RestClient.getInstance().setAuthHeaders(username, password);

                    afterLogin(username);
                }
            }
        }
    }*/

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

        if (lastKnownLocationObservable != null) {
            lastKnownLocationSubscription = lastKnownLocationObservable
                    .subscribe(new Action1<Location>() {
                        @Override
                        public void call(Location location) {
                            doObtainedLocation(location);
                        }
                    }, new ErrorHandler());
        }
        if (locationUpdatesObservable != null) {
            updatableLocationSubscription = locationUpdatesObservable
                    .doOnError(new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            String message = "Error on location update: " + throwable.getMessage();
                            Log.e("updateLocation", message, throwable);
                            //Crashlytics.logException(throwable);
                        }
                    })
                    .onErrorReturn(new Func1<Throwable, Location>() {
                        @Override
                        public Location call(Throwable throwable) {
                            //locationUnSubscribe();
                            return null;
                        }
                    }).filter(new Func1<Location, Boolean>() {
                        @Override
                        public Boolean call(Location location) {
                            return location != null && location.getAccuracy() < SUFFICIENT_ACCURACY;
                        }
                    })
                    .subscribe(new Action1<Location>() {
                        @Override
                        public void call(Location location) {
                            doObtainedLocation(location);
                        }
                    }, new ErrorHandler());
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);

        if (updatableLocationSubscription != null) updatableLocationSubscription.unsubscribe();
        if (lastKnownLocationSubscription != null) lastKnownLocationSubscription.unsubscribe();

        super.onStop();
    }

    /**
     * greenEvent after user login
     *
     * @param users
     */
    public void onEvent(Users users) {
        //setServers();
    }

    /*@Background
    void beforeStartUserPass() {
        //String token = AppPreferences.getInstance().getAccessToken();
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
            //if (token != null && !token.equals("")) {
            //Log.i(TAG, "already authorized fb token=" + accessToken.getToken());
            if (!RestClient.getInstance().haveAuthorization()) {
                //Log.i(TAG, "AppPreferences.getInstance().getAccessToken=" + token);
                Users user = RestClient.getInstance().FacebookLogin(accessToken.getToken());
                if (user != null) { //user already exist
                    if (user.getId() != null && user.getId() > 0) {
                        RestClient.getInstance().setAuthHeadersEncoded(user.getUsername(), user.getPassword());
                        afterLogin(user.getUsername());
                    } else startUserPass();
                } else startUserPass();
            } else {
                Users user = AppPreferences.getInstance().getUsers();
                if (user != null) afterLogin(user.getUsername());
                else Log.e(TAG, "facebook no user");
            }
        } else startUserPass();
    }*/

    /*@Background
    void gcmRegister() {
        if (playServicesConnected()) {

            if (TaxiApplication.getGCMRegistrationId() == null) {
                GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                try {
                    String regId = gcm.register(RestClient.getInstance().getGCMsenderIds());
                    if (regId != null && regId.length() > 0) {
                        //String oldRegid = RestClient.getInstance().getGCMRegistrationId();
                        //if (oldRegid == null || !oldRegid.equals(regId)) {
                        Boolean isRegister = RestClient.getInstance().gcmRegister(regId);
                        if (isRegister){
                            Log.i("gcmRegister", "gcmRegister successful");
                            TaxiApplication.setGCMRegistrationId(regId);
                        }
                        else {
                            Log.e("gcmRegister", "gcmRegister not registered");
                        }
                        // }
                    }
                } catch (IOException e) {
                    Log.e("gcmRegister", "gcmRegister IOException", e);
                }
            }
        } else {
            Log.i("playServicesConnected", "No valid Google Play Services APK found.");
        }
    }*/

    public boolean playServicesConnected() {
        try {
            // Check that Google Play services is available
            GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
            int result = googleAPI.isGooglePlayServicesAvailable(this);
            // If Google Play services is available
            if (ConnectionResult.SUCCESS == result) {
                // Continue
                return true;

            } else if (googleAPI.isUserResolvableError(result)) {
                setPlayServicesResolutionRequest(result);
            } else Log.e("playServicesConnected", "result:" + result);
        } catch (IllegalStateException e) {
            Log.e("playServicesConnected", "IllegalStateException", e);
        } catch (Exception e) {
            Log.e("playServicesConnected", "Exception", e);
        }

        return false;
    }

    @UiThread
    void setPlayServicesResolutionRequest(int result) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        Dialog errorDialog = googleAPI.getErrorDialog(this, result, PLAY_SERVICES_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            try {
                ErrorDialogFragment.newInstance(errorDialog).show(getFragmentManager(), "Play Service");
            } catch (Exception e) {
                Log.e("playServicesConnected", "Exception", e);
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            startHome();
        } else if (id == R.id.nav_map) {
            BubbleOverlay_.intent(this).start();
        } else if (id == R.id.nav_request) {
            startRequests(false);
            //RequestsActivity_.intent(this).startForResult(REQUEST_INFO);
        } else if (id == R.id.nav_history) {
            startRequests(true);
            //TaxiApplication.requestsHistory(true);
            //RequestsActivity_.intent(this).startForResult(REQUEST_INFO);
        } else if (id == R.id.nav_servers) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, ServersFragment_.builder().build())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
            //ServersActivity_.intent(this).startForResult(SERVER_CHANGE);
        } //else if (id == R.id.nav_book_taxi) NewRequestActivity_.intent(this).start();
        else if (id == R.id.nav_send_log) {
            RestClient.getInstance().clearCache();
            ACRA.getErrorReporter().handleSilentException(new Exception("Developer Report"));
        } else if (id == R.id.nav_exit) {
            exitButton();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Define a DialogFragment that displays the error dialog
    /*public static class MainDialogFragment extends android.support.v4.app.DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public MainDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (mDialog == null) super.setShowsDialog(false);
            return mDialog;
        }
    }*/

    @UiThread
    public void createNotification() {
        // Prepare intent which is triggered if the
        // notification is selected
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=com.opentaxi.android"));
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Build notification
        Notification noti = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.new_version))
                //.setContentText("Version")
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pIntent)
                .addAction(R.drawable.icon, "Update", pIntent).build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // hide the notification after its selected
        noti.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(0, noti);
    }

    private void doObtainedLocation(Location location) {
        try {
            CoordinatesLight coordinates = new CoordinatesLight();
            coordinates.setN(location.getLatitude());
            coordinates.setE(location.getLongitude());
            coordinates.setT(location.getTime());
            Intent i = new Intent(this, CoordinatesService.class);
            i.putExtra("coordinates", coordinates);
            startService(i);

            /*if (AppPreferences.getInstance() != null) {

                Date now = new Date();
                AppPreferences.getInstance().setNorth(location.getLatitude());
                AppPreferences.getInstance().setEast(location.getLongitude());
                AppPreferences.getInstance().setCurrentLocationTime(location.getTime());
                AppPreferences.getInstance().setGpsLastTime(now.getTime());
            }
            Log.i("doObtainedLocation", "onReceive: received location update:" + location.getLatitude() + ", " + location.getLongitude());*/
        } catch (Exception e) {
            Log.e("doObtainedLocation", "onReceive:" + e.getMessage());
        }
    }

    private class ErrorHandler implements Action1<Throwable> {
        @Override
        public void call(Throwable throwable) {
            Log.d("MainActivity", "Error occurred", throwable);
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);

        //menu.findItem(R.id.options_server).setIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_3d_rotation).actionBar().color(Color.BLACK));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            /*case R.id.options_server:
                ServersActivity_.intent(this).startForResult(SERVER_CHANGE);
                return true;*/
            case R.id.options_help:
                HelpActivity_.intent(this).startForResult(HELP);
                return true;
            case R.id.options_exit:
                //RestClient.getInstance().clearCache();
                finish();
                return true;
            /*case R.id.options_send_log:

                RestClient.getInstance().clearCache();
                ACRA.getErrorReporter().handleSilentException(new Exception("Developer Report"));
                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        // Check device for Play Services APK.
        //servicesConnected();
        startHome();
    }

    /**
     * @return true if User is login false is not
     */
    private boolean checkUserLogin() {

        String user = AppPreferences.getInstance(getApplicationContext()).getUsers().getUsername();
        String pass = AppPreferences.getInstance(getApplicationContext()).getUsers().getPassword();

        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            return false;
        } else {
            if (!RestClient.getInstance().haveAuthorization()) { //autologin
                try {
                    /*if (SecurityLevel.HIGH.getCode().equals(AppPreferences.getInstance().getUsers().getCookieexpire()))
                        RestClient.getInstance().setAuthHeadersSecure(user, pass);
                    else*/
                    RestClient.getInstance().setAuthHeaders(user, pass);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
    }

    @Background
    void checkFbLogin() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
            //if (token != null && !token.equals("")) {
            //Log.i(TAG, "already authorized fb token=" + accessToken.getToken());
            //if (!RestClient.getInstance().haveAuthorization()) {
            //Log.i(TAG, "AppPreferences.getInstance().getAccessToken=" + token);
            Users user = RestClient.getInstance().FacebookLogin(accessToken.getToken());
            if (user != null) { //user already exist
                if (user.getId() != null && user.getId() > 0) {
                    RestClient.getInstance().setAuthHeaders(user.getUsername(), user.getPassword());
                    startHomeUI();
                }
            }
        }
    }

    @UiThread
    void startHomeUI() {
        startHome();
    }

    @Override
    public void startHome() {
        Log.i(TAG, "startHome");
        android.support.v4.app.Fragment fragment = null;

        if (checkUserLogin()) {
            //showNewRequest(true);

            if (redirectFragment != null) {
                fragment = redirectFragment;
                redirectFragment = null;
            } else {
                //getSupportFragmentManager().popBackStack(null, android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fragment = HomeFragment_.builder().build();
            }
        } else {
            //checkFbLogin();
            // Log.i(TAG, "startHome no user");
            fragment = UserPassFragment_.builder().build();
        }

        if (fragment != null && !isFinishing()) {
            //Log.i("startHome", fragment.toString());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    //.disallowAddToBackStack()
                    .commitAllowingStateLoss();
        } /*else if (redirectFragment != null && !isFinishing()) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, redirectFragment)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
            redirectFragment = null;*/ else Log.i("startHome", "no fragment isFinishing=" + isFinishing());
    }

    @UiThread
    void showNewRequest(boolean show) {
        Log.i(TAG, "showNewRequest");
        if (fab != null) {
            if (show) {
                fab.setVisibility(View.VISIBLE);
                Tooltip.make(this,
                        new Tooltip.Builder(101)
                                .anchor(fab, Tooltip.Gravity.LEFT)
                                .closePolicy(new Tooltip.ClosePolicy()
                                        .insidePolicy(true, false)
                                        .outsidePolicy(true, false), 20000)
                                //.activateDelay(1800)
                                .showDelay(3000)
                                .text(getResources(), R.string.taxi_tooltip) //"Поръчай такси с едно кликване"
                                //.maxWidth(500)
                                .withArrow(true)
                                .withOverlay(true)
                                //.floatingAnimation(Tooltip.AnimationBuilder.SLOW)
                                .withStyleId(R.style.ToolTipLayoutCustomStyle)
                                .build()
                ).show();
            } else fab.setVisibility(View.INVISIBLE);
        } else Log.e(TAG, "fab=null");
    }

    @Override
    public void fabVisible(boolean isVisible) {
        showNewRequest(isVisible);
    }

    @Override
    public void closeKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void startNewRequest(Cars cars) {
        if (!isFinishing()) {
            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            NewRequestFragment fragment = NewRequestFragment_.builder().build();
            if (cars != null) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("cars", cars);
                fragment.setArguments(bundle);
            }
// Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);

// Commit the transaction
            transaction.commitAllowingStateLoss();
        }
    }

    @Override
    public void startCarDetails(Integer carsId) {
        if (!isFinishing() && carsId != null) {
            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            CarDetailsFragment fragment = CarDetailsFragment_.builder().build();
            Bundle bundle = new Bundle();
            bundle.putInt("carsId", carsId);
            fragment.setArguments(bundle);
// Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);

// Commit the transaction
            transaction.commitAllowingStateLoss();
        }
    }

    @Override
    public void startRequestDetails(NewCRequestDetails newRequest) {
        if (!isFinishing()) {
            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            RequestDetailsFragment fragment = RequestDetailsFragment_.builder().build();
            Bundle bundle = new Bundle();
            bundle.putSerializable("newCRequest", newRequest);
            fragment.setArguments(bundle);
// Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);

// Commit the transaction
            transaction.commitAllowingStateLoss();
        }
    }

    @Override
    public void startEditRequest(NewCRequestDetails newCRequest) {
        if (!isFinishing()) {
            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            NewRequestFragment fragment = NewRequestFragment_.builder().build();
            Bundle bundle = new Bundle();
            bundle.putSerializable("newCRequest", newCRequest);
            fragment.setArguments(bundle);
// Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);

// Commit the transaction
            transaction.commitAllowingStateLoss();
        }
    }

    @Override
    public void startRequests(boolean history) {
        if (!isFinishing()) {
            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            RequestsHistoryFragment fragment = RequestsHistoryFragment_.builder().build();
            Bundle bundle = new Bundle();
            bundle.putBoolean("history", history);
            fragment.setArguments(bundle);
// Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);

// Commit the transaction
            transaction.commitAllowingStateLoss();
        }
    }

    @Override
    public void setBarTitle(String title) {
        setTitle(title);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        //SimpleFacebook.getInstance(this).onActivityResult(this, requestCode, resultCode, data);

        switch (requestCode) {

            case PLAY_SERVICES_RESOLUTION_REQUEST:

            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        break;
                    case Activity.RESULT_CANCELED:
                        //TaxiApplication.setHavePlayService(false);
                        break;
                }
                break;
            case SERVER_CHANGE:
                if (resultCode == RESULT_OK) {

                }
                break;
            case REQUEST_USER_PASS_CODE:
                //Log.e(TAG, "REQUEST_USER_PASS_CODE onActivityResult requestCode:" + requestCode + " resultCode:" + resultCode);
                if (resultCode == RESULT_OK) {
                    //userLogin(data);
                    //setVersion();
                } else if (resultCode == RESULT_CANCELED) {
                    finish();
                    break;
                }
                //checkUser();
                break;
            default:
                Log.e(TAG, "onActivityResult requestCode:" + requestCode + " resultCode:" + resultCode);
                break;
        }
    }

    //@Click
    private void exitButton() {
        //showDialog(DIALOG_EXIT);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.exit);
        alertDialogBuilder.setMessage(getString(R.string.exit_confirm));
        //null should be your on click listener
        alertDialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                new LogoutTask().execute();
                //AppPreferences.getInstance().setAccessToken("");
                AppPreferences.getInstance().setLastCloudMessage(null);
                AppPreferences.getInstance().setUsers(null);
                RestClient.getInstance().removeAuthorization();

                LoginManager.getInstance().logOut();
                //facebookLogout();
                finish();
            }
        });
        alertDialogBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        Dialog exitDialog = alertDialogBuilder.create();
        exitDialog.show();
    }

   /* @Background
    void facebookLogout() {
        final OnLogoutListener onLogoutListener = new OnLogoutListener() {

            @Override
            public void onFail(String reason) {
                Log.w(TAG, "Failed to logout");
            }

            @Override
            public void onException(Throwable throwable) {
                Log.e(TAG, "Bad thing happened", throwable);
            }

            @Override
            public void onThinking() {
            }

            @Override
            public void onLogout() {
            }

        };
        SimpleFacebook.getInstance(this).logout(onLogoutListener);
    }*/

    /*@Click
    void requestButton() {
        //Intent msgIntent = new Intent(getBaseContext(), NewRequestActivity_.class);
        //startActivityForResult(msgIntent, REQUEST_INFO);
        RequestsActivity_.intent(this).startForResult(REQUEST_INFO);
    }*/

    /*@Click
    void mapButton() {
        BubbleOverlay_.intent(this).start();
        //LocationLibrary.forceLocationUpdate(getBaseContext());
    }*/

    /*@Click
    void newRequestButton() {
        //LocationLibrary.forceLocationUpdate(getBaseContext());
        NewRequestActivity_.intent(this).start();
    }*/

    @Click
    void fab() {
        startNewRequest(null);
    }

    private boolean isDataConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            return cm.getActiveNetworkInfo().isConnectedOrConnecting();
        } catch (Exception e) {
            return false;
        }
    }

    private int isHighBandwidth() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            return wm.getConnectionInfo().getLinkSpeed();
        } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getNetworkType();
        }
        return 0;
    }


    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        if (mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        return super.onPrepareOptionsMenu(menu);
    }
}
