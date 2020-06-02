package com.example.mymap;

import androidx.core.app.ActivityCompat;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        OnMapLoadedCallback, OnMarkerClickListener, OnMapLongClickListener, SensorEventListener {

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    List<Marker> markerList;
    Marker gpsMarker = null;
    List<Double> savedMarkers;
    private final String MyJsonFile = "markers.json";
    private TextView textView;
    static private SensorManager sensorManager;
    private Sensor sensor;
    boolean hideTheseButtons = false;
    boolean boolForHiding = false;


    public void zoomOutClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    public void zoomInClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        markerList = new ArrayList<>();
        savedMarkers = new ArrayList<>();
        FloatingActionButton hide = findViewById(R.id.floatingActionButton);
        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolForHiding = !boolForHiding;
                Log.i("TAG - Bool", "" + boolForHiding);
                if (boolForHiding) {
                    textView.setVisibility(View.VISIBLE);
                } else textView.setVisibility(View.INVISIBLE);
            }
        });
        FloatingActionButton show = findViewById(R.id.floatingActionButton2);
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hideTheseButtons) {
                    FlingAnimation variable = new FlingAnimation(findViewById(R.id.animationButtons), DynamicAnimation.SCROLL_X);
                    variable.setStartVelocity(-2000).setMinValue(0).setFriction(1.1f).start();
                    variable.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
                    hideTheseButtons = false;
                    if (boolForHiding) {
                        textView.setVisibility(View.INVISIBLE);
                        boolForHiding = false;
                    }
                }
            }
        });
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        textView.setVisibility(View.INVISIBLE);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        restoreFromJson();

    }

    private void createLocationREQ(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    if (gpsMarker != null)
                        gpsMarker.remove();

                    Location location = locationResult.getLastLocation();

                }
            }
        };
    }

    @Override
    public void onMapLoaded() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        createLocationREQ();
        createLocationCallback();
        startLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, 10000);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            saveThisToJson();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensor != null) {
            sensorManager.unregisterListener(this, sensor);
        }

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Marker longClickMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                .alpha(0.7f).title(String.format("Position(%.3f, %.3f) ", latLng.latitude, latLng.longitude)));
        markerList.add(longClickMarker);
        savedMarkers.add(latLng.latitude);
        savedMarkers.add(latLng.longitude);
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        double x = event.values[0];
        double y = event.values[1];
        textView.setText(String.format("Acceleration: \n \t x: %.4f\t\t\t y: %.4f\t", x, y));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mMap.getUiSettings().setMapToolbarEnabled(false);
        if (hideTheseButtons == false) {
            FlingAnimation variable = new FlingAnimation(findViewById(R.id.animationButtons), DynamicAnimation.SCROLL_X);
            variable.setStartVelocity(2000).setMinValue(-100).setMaxValue(500).setFriction(2.1f).start();
            variable.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
            hideTheseButtons = true;
        }
        return false;
    }


    public void clearMemoryOnClick(View view) throws JSONException {
        markerList.clear();
        savedMarkers.clear();
        mMap.clear();
        saveThisToJson();
        if (boolForHiding) {
            textView.setVisibility(View.INVISIBLE);
            boolForHiding = false;
        } //else textView.setVisibility(View.VISIBLE);

        if (hideTheseButtons == true) {
            FlingAnimation variable = new FlingAnimation(findViewById(R.id.animationButtons), DynamicAnimation.SCROLL_X);
            variable.setStartVelocity(-2000).setMinValue(0).setFriction(1.1f).start();
            variable.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA);
            hideTheseButtons = false;
            if (boolForHiding) {
                textView.setVisibility(View.INVISIBLE);
                boolForHiding = false;
            }
        }
    }



    private void saveThisToJson() throws JSONException {
        Gson gson = new Gson();
        String list = gson.toJson(savedMarkers);
        FileOutputStream FoutputStream;
        try {
            FoutputStream = openFileOutput(MyJsonFile, MODE_PRIVATE);
            FileWriter writer = new FileWriter(FoutputStream.getFD());
            writer.write(list);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void restoreFromJson() {
        FileInputStream inputStream;
        int BUFFER_SIZE = 1000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput(MyJsonFile);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();

            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n < BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<Double>>() {
            }.getType();
            List<Double> o = gson.fromJson(readJson, collectionType);
            savedMarkers.clear();
            if (o != null) {
                savedMarkers.addAll(o);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        markerList.clear();
        try {
            for (int i = 0; i < savedMarkers.size(); i += 2) {
                Marker onSavedMarked = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(savedMarkers.get(i), savedMarkers.get(i + 1)))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                        .alpha(0.7f).title(String.format("Position(%.2f, %.2f) ", savedMarkers.get(i), savedMarkers.get(i + 1))));
                markerList.add(onSavedMarked);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }


}

