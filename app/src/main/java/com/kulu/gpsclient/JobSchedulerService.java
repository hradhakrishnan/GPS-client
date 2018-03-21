package com.kulu.gpsclient;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by Hari on 20/03/2017.
 */
public class JobSchedulerService extends JobService implements GoogleApiClient.ConnectionCallbacks,
       GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = JobSchedulerService.class.getSimpleName();
    public static final String PREFS_NAME = "GeoClient_Settings";
    private GoogleApiClient mGoogleApiClient;
    protected String driver_name = "";
    protected String vehicle_name = "";
    protected String param1 = "";
    protected String param2 = "";
    protected String endpoint = "";
    protected String latitude = "";
    protected String longitude = "";
    protected String pollinginterval = "";
    Location mNewLocation;
    Context mContext;
    //LocationListener mLocationListener;
    //private Handler mServiceHandler;
    //private NotificationManager mNotificationManager;
    private LocationRequest mLocationRequest;

    private static long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final int NOTIFICATION_ID = 12345678;

    @Override
    public void onCreate() {
        super.onCreate();

      if (mGoogleApiClient == null) {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
            mGoogleApiClient.connect();

        }

        // HandlerThread handlerThread = new HandlerThread(TAG);
        // handlerThread.start();
        // mServiceHandler = new Handler(handlerThread.getLooper());
        // mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    }

    @Override
    public boolean onStartJob(JobParameters params) {

        PersistableBundle pb = params.getExtras();
        driver_name = pb.getString("drivername");
        vehicle_name = pb.getString("vehiclename");
        param1 = pb.getString("param1");
        param2 = pb.getString("param2");
        endpoint = pb.getString("endpoint");
       // SharedPreferences sd = getApplication().getSharedPreferences(PREFS_NAME, 0);
        latitude = pb.getString("latitude", "");
        longitude = pb.getString("longitude", "");
        pollinginterval = pb.getString("pollinginterval");
        if (pollinginterval != null && !pollinginterval.isEmpty()) {
            UPDATE_INTERVAL_IN_MILLISECONDS = Long.parseLong(pollinginterval);
        }
       // buildGoogleApiClient();
        createLocationRequest();
       /* int permissionCheck = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED && mGoogleApiClient.isConnected()) {

            PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }*/
        int permissionCheck = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if(mGoogleApiClient.isConnected()) {
                createLocationRequest();
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, JobSchedulerService.this);
                mNewLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                if (mNewLocation != null) {
                    latitude = ""+ mNewLocation.getLatitude();
                    longitude = "" + mNewLocation.getLongitude();
                    PushLocationDetails();
                }
            } else {
                Log.e(TAG, "Google Client not connected. onStartJob. ");
            }

        }
        //PushLocationDetails();
        mJobHandler.sendMessage( Message.obtain( mJobHandler, 1, params ) );
        return true;
    }



    @Override
    public boolean onStopJob(JobParameters params) {
        mJobHandler.removeMessages( 1 );
       // mGoogleApiClient.disconnect();
       // mLocationRequest=null;
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        Toast.makeText( getApplicationContext(), "Job cancelled", Toast.LENGTH_SHORT ).show();
        return false;
    }


    private Handler mJobHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage( Message msg ) {
            Toast.makeText( getApplicationContext(), "JobService task running", Toast.LENGTH_SHORT ).show();
            jobFinished( (JobParameters) msg.obj, false );
            return true;
        }

    } );




    public void PushLocationDetails() {

        String url = endpoint;
        JSONObject obj = new JSONObject();
        try {
            obj.put("vehicle_name", vehicle_name);
            obj.put("driver_name", driver_name);
            obj.put("param1", param1);
            obj.put("param2", param2);
            obj.put("latitude", latitude);
            obj.put("longitude", longitude);

            System.out.println(obj.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST,url,obj,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                       // System.out.println("json went thru");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                       // System.out.println("Something went wrong with json request! "+ error.toString());
                    }
                });

        queue.add(jsObjRequest);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
        try {
            int permissionCheck = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                if(mGoogleApiClient.isConnected()) {
                    mNewLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (mNewLocation != null) {
                        latitude = ""+ mNewLocation.getLatitude();
                        longitude = "" + mNewLocation.getLongitude();
                        PushLocationDetails();
                    }
                }else {
                    Log.e(TAG, "Google Client not connected. onConnected ");
                }
            }
        }
        catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        // In this example, we merely log the suspension.
        Log.e(TAG, "GoogleApiClient connection suspended. onConnectionSuspended");

    }

    @Override
    public void onLocationChanged(Location location) {
        // In this example, we merely log change in location.
        Log.e(TAG, "Location changed.onLocationChanged");
        latitude = ""+location.getLatitude();
        longitude = ""+location.getLongitude();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Connection failed. onConnectionFailed");

    }

    /**
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
}
