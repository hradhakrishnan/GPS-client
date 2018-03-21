package com.kulu.gpsclient;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapsActivity extends AppCompatActivity  implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,LocationListener {

    protected static final String TAG = "MainActivity";
    public static final String PREFS_NAME = "GeoClient_Settings";

    //permission constants
    private final int REQUEST_LOCATION_PERMISSION = 12345;

    /**
     * Provides the entry point to Google Play services.
     */
    public GoogleApiClient mGoogleApiClient;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;

    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected TextView mLatitudeText;
    protected TextView mLongitudeText;
    protected TextView mVehicleNameText;
    protected TextView mVehicleDriverText;
    protected TextView mParam1;
    protected TextView mParam2;
    protected TextView mHttpEndPointText;
    protected TextView mPollingIntervalText;
    protected ImageView mHelpButton;

    private JobScheduler mJobScheduler;
    SharedPreferences settings;
    private LocationRequest mLocationRequest;
    long poll_interval = 10000;

    MapView mapView;
    GoogleMap map;
    PendingResult<Status> pendingResult;


    // Tab titles
    private String[] tabs = {"Geo", "Vehicle", "Settings"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Inflate your custom layout
        final ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(
                R.layout.actionbar_layout,
                null);

        // Set up your ActionBar
        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(actionBarLayout);

        mHelpButton = (ImageView) actionBarLayout.findViewById(R.id.helpButton);
        mHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MapsActivity.this, HelpActivity.class);
                startActivity(i);
            }
        });

        // mLatitudeLabel = getResources().getString(R.string.latitude_label);
        //  mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLatitudeText = (TextView) findViewById((R.id.latitude_text));
        mLongitudeText = (TextView) findViewById((R.id.longitude_text));
        mVehicleNameText = (TextView) findViewById((R.id.vehiclename));
        mVehicleDriverText = (TextView) findViewById((R.id.drivername));
        mParam1 = (TextView) findViewById((R.id.param1));
        mParam2 = (TextView) findViewById((R.id.param2));
        mHttpEndPointText = (TextView) findViewById((R.id.http_endpoint));
        mPollingIntervalText = (TextView) findViewById((R.id.polling_interval));

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        TabHost host = (TabHost) findViewById(R.id.tabHost);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Geo");
        spec.setContent(R.id.tab1);
        spec.setIndicator("Geo");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Vehicle");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Vehicle");
        host.addTab(spec);

        //Tab 3
        spec = host.newTabSpec("Settings");
        spec.setContent(R.id.tab3);
        spec.setIndicator("Settings");
        host.addTab(spec);


        settings = getSharedPreferences(PREFS_NAME, 0);


        if (settings.contains("vehiclename")) {
            mVehicleNameText.setText(settings.getString("vehiclename", ""));
        }
        if (settings.contains("drivername")) {
            mVehicleDriverText.setText(settings.getString("drivername", ""));
        }
        if (settings.contains("param1")) {
            mParam1.setText(settings.getString("param1", ""));
        }
        if (settings.contains("param2")) {
            mParam2.setText(settings.getString("param2", ""));
        }
        if (settings.contains("http_endpoint")) {
            mHttpEndPointText.setText(settings.getString("http_endpoint", ""));
        }
        if (settings.contains("polling_interval")) {
            mPollingIntervalText.setText(settings.getString("polling_interval", ""));
        }


        mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        Button mScheduleJobButton = (Button) findViewById(R.id.schedule_job);
        Button mCancelAllJobsButton = (Button) findViewById(R.id.cancel_all);

        mScheduleJobButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                JobInfo.Builder builder = new JobInfo.Builder(1, new ComponentName(getPackageName(), JobSchedulerService.class.getName()));
                PersistableBundle bundle = new PersistableBundle();
                bundle.putString("latitude", mLatitudeText.getText().toString());
                bundle.putString("longitude", mLongitudeText.getText().toString());
                bundle.putString("endpoint", mHttpEndPointText.getText().toString());
                bundle.putString("vehiclename", mVehicleNameText.getText().toString());
                bundle.putString("drivername", mVehicleDriverText.getText().toString());
                bundle.putString("param1", mParam1.getText().toString());
                bundle.putString("param2", mParam2.getText().toString());
                bundle.putString("pollinginterval", mPollingIntervalText.getText().toString());


                if (!mPollingIntervalText.getText().toString().isEmpty()) {
                    poll_interval = Integer.parseInt(mPollingIntervalText.getText().toString());
                }
                if (poll_interval > 0) {
                    builder.setPeriodic(poll_interval);
                } else {
                    builder.setPeriodic(10000);
                }
                builder.setExtras(bundle);
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);


                if (mJobScheduler.schedule(builder.build()) <= 0) {
                    Toast.makeText(getApplicationContext(), "Error with JobService task", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mCancelAllJobsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mJobScheduler.cancelAll();
             /*  if (mGoogleApiClient != null)
                    mGoogleApiClient.disconnect();*/
            }
        });

        createLocationRequest();
        buildGoogleApiClient();
       // getPermissionDetails();
    }

    public void Save(View view) {

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("vehiclename", mVehicleNameText.getText().toString());
        editor.putString("drivername", mVehicleDriverText.getText().toString());
        editor.putString("param1", mParam1.getText().toString());
        editor.putString("param2", mParam2.getText().toString());

        // Commit the edits!
        editor.apply();
        Toast.makeText(this, "Vehicle Info Saved", Toast.LENGTH_LONG).show();


    }

    public void Settings(View view) {

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("http_endpoint", mHttpEndPointText.getText().toString());
        editor.putString("polling_interval", mPollingIntervalText.getText().toString());

        // Commit the edits!
        editor.apply();
        Toast.makeText(this, "Settings Saved", Toast.LENGTH_LONG).show();


    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public void getPermissionDetails(){
        int permissionCheck = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // User may have declined earlier, ask Android if we should show him a reason
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // show an explanation to the user
                // Good practise: don't block thread after the user sees the explanation, try again to request the permission.
                Toast.makeText(this, "Enable Location Permission in your settings", Toast.LENGTH_LONG).show();
            } else {
                // request the permission.
                // CALLBACK_NUMBER is a integer constants
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                // The callback method gets the result of the request.
            }
        } else {
            // got permission use it
            getLocationDetails();
        }
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
  @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        int permissionCheck = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // User may have declined earlier, ask Android if we should show him a reason
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // show an explanation to the user
                // Good practise: don't block thread after the user sees the explanation, try again to request the permission.
                Toast.makeText(this, "Enable Location Permission in your settings", Toast.LENGTH_LONG).show();
            } else {
                // request the permission.
                // CALLBACK_NUMBER is a integer constants
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                // The callback method gets the result of the request.
            }
        } else {
            // got permission use it
            getLocationDetails();
        }


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, do your work....
                    getLocationDetails();

                } else {
                    // permission denied
                    // Disable the functionality that depends on this permission.
                    Toast.makeText(this, "Enable Location Permission in your settings", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' statements for other permssions
        }
    }

    public void getLocationDetails() {
        int permissionCheck = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if(pendingResult==null)
            pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
           // mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
//                mLatitudeText.setText(String.format("%s: %f", mLatitudeLabel,
//                        mLastLocation.getLatitude()));
//                mLongitudeText.setText(String.format("%s: %f", mLongitudeLabel,
//                        mLastLocation.getLongitude()));
                Log.e(TAG, "lastlocation " + mLastLocation.getLatitude() + " " + mLastLocation.getLongitude());
                mLatitudeText.setText("" + mLastLocation.getLatitude());
                mLongitudeText.setText("" + mLastLocation.getLongitude());

                // Intent intent = new Intent(getApplicationContext(), GPSLocationService.class);
                // startService(intent);
                try {
                    MapsInitializer.initialize(getApplicationContext());

                } catch (Exception e) {
                    e.printStackTrace();

                }
                mapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        map = googleMap;
                        int permissionCheck = ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            map.setMyLocationEnabled(true);
                            LatLng myLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                            map.addMarker(new MarkerOptions().position(myLocation).title("You"));
                            CameraPosition cameraPosition = new CameraPosition.Builder().target(myLocation).zoom(12).build();
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        }


                    }
                });
            } //else {
             //   Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
           // }


        }
    }


    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(poll_interval);
        mLocationRequest.setFastestInterval(poll_interval / 2);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onLocationChanged(Location location) {
        // In this example, we merely log change in location.
        Log.e(TAG, "Location changed.onLocationChanged" + location.getLatitude() + " " + location.getLongitude());
        if(mLastLocation==null || location.getTime()>mLastLocation.getTime()+10000) {
            mLastLocation = location;
            getLocationDetails();
        }
       // mLatitudeText.setText("" + location.getLatitude());
        //mLongitudeText.setText("" + location.getLongitude());
      //  SharedPreferences.Editor ed = settings.edit();
      //  ed.putString("latitude", "" + location.getLatitude());
      //  ed.putString("longitude", "" + location.getLongitude());
      //  ed.apply();

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null)
            mapView.onPause();
        if (mGoogleApiClient != null)
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
       // unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null)
            mapView.onResume();
      //  registerReceiver(broadcastReceiver, new IntentFilter(GPSLocationService.str_receiver));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null)
            mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null)
            mapView.onLowMemory();
    }


    public GoogleApiClient getmGoogleApiClient() {
        return mGoogleApiClient;
    }

  /*  private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {

            mLatitudeText.setText(intent.getStringExtra("latitude"));
            mLongitudeText.setText(intent.getStringExtra("longitude"));
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    map = googleMap;
                    int permissionCheck = ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        map.setMyLocationEnabled(true);
                        LatLng myLocation = new LatLng(Double.valueOf(intent.getStringExtra("latitude")),
                                Double.valueOf(intent.getStringExtra("longitude")));
                        map.addMarker(new MarkerOptions().position(myLocation).title("You"));
                        CameraPosition cameraPosition = new CameraPosition.Builder().target(myLocation).zoom(12).build();
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    }


                }
            });

        }
    };*/
}
