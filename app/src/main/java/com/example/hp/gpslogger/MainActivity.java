package com.example.hp.gpslogger;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;

import java.io.FileOutputStream;

import java.io.InputStreamReader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.util.List;


import butterknife.OnClick;

import butterknife.BindView;
import butterknife.ButterKnife;



public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.location_result)
    TextView txtLocationResult;

    @BindView(R.id.updated_on)
    TextView txtUpdatedOn;

    @BindView(R.id.btn_start_location_updates)
    Button btnStartUpdates;

    @BindView(R.id.btn_stop_location_updates)
    Button btnStopUpdates;
    GoogleMap map;
    // location last updated time
    private String mLastUpdateTime;

    // location updates interval - 10sec
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 500;

    private static final int REQUEST_CHECK_SETTINGS = 100;
    // bunch of location related apis
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private Location mlastLocation;

    // boolean flag to toggle the ui
    private Boolean mRequestingLocationUpdates;

    EditText text;
    StringBuilder data = new StringBuilder();
    private String FILE_NAME = "";
    boolean clicked=false;
    ArrayList<String> allFiles = new ArrayList<String>();
    ArrayList<String> files = new ArrayList<String>();
    LinearLayout linearmain;
    CheckBox checkBox;

    boolean[] addedBoxes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        text = (EditText)findViewById(R.id.filename);
         linearmain = (LinearLayout) findViewById(R.id.linearmain);
        data.append("Latitude,Longitude,Speed");
        // initialize the necessary libraries
        init();
        // restore the values from saved instance state
        restoreValuesFromBundle(savedInstanceState);
    }

    private void init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                mCurrentLocation = locationResult.getLastLocation();
                mlastLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

                updateLocationUI();
            }
        };
        mRequestingLocationUpdates = false;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }


    private void restoreValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("is_requesting_updates")) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates");
            }

            if (savedInstanceState.containsKey("last_known_location")) {
                mCurrentLocation = savedInstanceState.getParcelable("last_known_location");
            }

            if (savedInstanceState.containsKey("last_updated_on")) {
                mLastUpdateTime = savedInstanceState.getString("last_updated_on");
            }
        }

        updateLocationUI();
    }
    private void updateLocationUI() {
        if (mCurrentLocation != null) {
            String speed = "0m/s";
            if (mCurrentLocation.hasSpeed()) {
                speed = String.valueOf(mCurrentLocation.getSpeed()) + "m/s";
            }
            else {
                mlastLocation = mCurrentLocation;

                double radius = 6371000;
                double dLat = Math.toRadians(mCurrentLocation.getLatitude()-mlastLocation.getLatitude());
                double dLon = Math.toRadians(mCurrentLocation.getLongitude() - mlastLocation.getLongitude());
                double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(Math.toRadians(mCurrentLocation.getLatitude())) * Math.cos(Math.toRadians(mlastLocation.getLatitude())) *
                                Math.sin(dLon/2) * Math.sin(dLon/2);
                double c = 2 * Math.asin(Math.sqrt(a));
                double distance =  Math.round(radius * c);
                double newTime= System.currentTimeMillis();
                double oldtime = 0.0;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = sdf.parse(mLastUpdateTime);
                    long millis = date.getTime();
                    oldtime = (double)millis;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                double timeDifferent = newTime - oldtime;
                speed = String.valueOf(distance*1000.0/timeDifferent) + "m/s";
            }
            txtLocationResult.setText(
                    "Lat: " + mCurrentLocation.getLatitude() + ", " +
                            "Lng: " + mCurrentLocation.getLongitude() + "," + "Speed: " + speed
            );

            data.append("\n" + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude() + "," + speed);
//            Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
            // giving a blink animation on TextView
            txtLocationResult.setAlpha(0);
            txtLocationResult.animate().alpha(1).setDuration(300);

            // location last updated time
            txtUpdatedOn.setText("Last updated on: " + mLastUpdateTime);
        }

        toggleButtons();
    }
    public void save(View v) {

        linearmain.removeAllViews();

        FILE_NAME = text.getText().toString();
        files.clear();
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            outputStream.write((data.toString()).getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File dirFiles = getApplicationContext().getFilesDir();

//                String[] files = new String[20];

//                int k = 0;

        for (String strFile : dirFiles.list())
        {
            if(strFile.endsWith(".csv")) {
                 if(!files.contains(strFile)) {
                     files.add(strFile);
                     Toast.makeText(getApplicationContext(), strFile, Toast.LENGTH_SHORT).show();
                 }

            }


        }
//        Toast.makeText(getApplicationContext(), set.size(), Toast.LENGTH_SHORT).show();

//        set.clear();
//        Toast.makeText(getApplicationContext(), files.size(), Toast.LENGTH_SHORT).show();



        for(int i=0;i<files.size();i++)
        {

            checkBox = new CheckBox(this);

                checkBox.setId(i);
                checkBox.setText(files.get(i));
                checkBox.setOnClickListener(getOnClickDo(checkBox));
                linearmain.addView(checkBox);

        }


    }
    public void viewmap(View v) {
        clicked=true;

        onMapReady(map);

    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_requesting_updates", mRequestingLocationUpdates);
        outState.putParcelable("last_known_location", mCurrentLocation);
        outState.putString("last_updated_on", mLastUpdateTime);

    }

    private void toggleButtons() {
        if (mRequestingLocationUpdates) {
            btnStartUpdates.setEnabled(false);
            btnStopUpdates.setEnabled(true);
        } else {
            btnStartUpdates.setEnabled(true);
            btnStopUpdates.setEnabled(false);
        }
    }
    private void startLocationUpdates() {
        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        Toast.makeText(getApplicationContext(), "Started location updates!", Toast.LENGTH_SHORT).show();

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateLocationUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }

                        updateLocationUI();
                    }
                });
    }
    @OnClick(R.id.btn_start_location_updates)
    public void startLocationButtonClick() {
        // Requesting ACCESS_FINE_LOCATION using Dexter library
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        mRequestingLocationUpdates = true;
                        startLocationUpdates();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            // open device settings when the permission is
                            // denied permanently
                            openSettings();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    @OnClick(R.id.btn_stop_location_updates)
    public void stopLocationButtonClick() {
        mRequestingLocationUpdates = false;
        stopLocationUpdates();
    }

    public void stopLocationUpdates() {
        // Removing location updates
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(getApplicationContext(), "Location updates stopped!", Toast.LENGTH_SHORT).show();
                        toggleButtons();
                    }
                });
    }

    @OnClick(R.id.btn_get_last_location)
    public void showLastKnownLocation() {
        if (mCurrentLocation != null) {
            Toast.makeText(getApplicationContext(), "Lat: " + mCurrentLocation.getLatitude()
                    + ", Lng: " + mCurrentLocation.getLongitude(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Last known location is not available!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.e(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.e(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package",
                BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Resuming location updates depending on button state and
        // allowed permissions
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        }

        updateLocationUI();
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mRequestingLocationUpdates) {
            // pausing location updates
            stopLocationUpdates();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (clicked == true) {
//            Toast.makeText(getApplicationContext(), "maps called", Toast.LENGTH_SHORT).show();
//
            try {
                for(int i=0;i<allFiles.size();i++) {
                    FileInputStream in = openFileInput(allFiles.get(i));
                    InputStreamReader inputStreamReader = new InputStreamReader(in);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    List<LatLng> latLngList = new ArrayList<LatLng>();
                    String line;
                    boolean var = false;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (var == false) {
                            var = true;
                            continue;
                        }
                        String[] lines = line.split(",");
//                    Toast.makeText(getApplicationContext(), lines[0], Toast.LENGTH_SHORT).show();
//                    Toast.makeText(getApplicationContext(), "hey", Toast.LENGTH_SHORT).show();
//                Double.parseDouble(line[0]);
//                Double.parseDouble(line[1]);

                        latLngList.add(new LatLng(Double.parseDouble(lines[0]), Double.parseDouble(lines[1])));
                        LatLng Maharas1 = new LatLng(Double.parseDouble(lines[0]), Double.parseDouble(lines[1]));
                        map.addMarker(new MarkerOptions().position(Maharas1).title(line));
                        map.moveCamera(CameraUpdateFactory.newLatLng(Maharas1));

                    }
                    inputStreamReader.close();
                }



            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
//            try {
////            Toast.makeText(getApplicationContext(), "map1 called", Toast.LENGTH_SHORT).show();
//                InputStream is = getResources().openRawResource(R.raw.readings);
//                BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
//                Toast.makeText(getApplicationContext(), FILE_NAME, Toast.LENGTH_SHORT).show();
//                List<LatLng> latLngList = new ArrayList<LatLng>();
//
//
//                String info = "";
//                boolean var = false;
//                while ((info = reader.readLine()) != null) {
//                    if (var == false) {
//                        var = true;
//                        continue;
//                    }
////                Toast.makeText(getApplicationContext(), "hey", Toast.LENGTH_SHORT).show();
//                    String[] line = info.split(",");
//                    Toast.makeText(getApplicationContext(), line[0], Toast.LENGTH_SHORT).show();
////                Double.parseDouble(line[0]);
////                Double.parseDouble(line[1]);
//
//                    latLngList.add(new LatLng(Double.parseDouble(line[0]), Double.parseDouble(line[1])));
//                    LatLng Maharas1 = new LatLng(Double.parseDouble(line[0]), Double.parseDouble(line[1]));
//                    map.addMarker(new MarkerOptions().position(Maharas1).title("Maharashtra"));
//                    map.moveCamera(CameraUpdateFactory.newLatLng(Maharas1));
////                siteList.add(new String(siteName));
//                }
////            for  (LatLng towerLatLong : latLngList){
////                Toast.makeText(getApplicationContext(), towerLatLong.toString(), Toast.LENGTH_SHORT).show();
//////                map.addMarker(new MarkerOptions().position(towerLatLong).title("Maharashtra"));
//////                    map.moveCamera(CameraUpdateFactory.newLatLng(towerLatLong));
////
////                LatLng Maharas = towerLatLong;
////        map.addMarker(new MarkerOptions().position(Maharas).title("Maharashtra"));
////        map.moveCamera(CameraUpdateFactory.newLatLng(Maharas));
////            }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

        }
    }
    View.OnClickListener getOnClickDo(final Button button)
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allFiles.add(button.getText().toString());
            }
        };

    }
}