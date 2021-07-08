package com.example.qiblalocationfinder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;

import java.util.Arrays;
import java.util.List;

import static android.content.Context.SENSOR_SERVICE;

public class QiblaLocationFinder extends GodotPlugin implements SensorEventListener {
    LocationManager locationManager;
    LocationListener locationListener;
    Location userLocation;

    Location destinationLoc;

    float direction;
    float head;
    float bearTo;

    SensorManager sensorManager;
    Sensor accelerometer;
    Sensor magnetometer;

    Float azimut;

    float[] mGravity;
    float[] mGeomagnetic;


    @SuppressLint("MissingPermission")
    public QiblaLocationFinder(Godot godot) {
        super(godot);

        destinationLoc = new Location("");

        destinationLoc.setLatitude(21.422487); //kaaba latitude setting
        destinationLoc.setLongitude(39.826206);

        sensorManager =  (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (magnetometer != null && accelerometer != null) {
            // for the system's orientation sensor registered listeners
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);//SensorManager.SENSOR_DELAY_Fastest
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);//SensorManager.SENSOR_DELAY_Fastest
        }
        else {
            Toast.makeText(getActivity() ,"Not Supported", Toast.LENGTH_SHORT).show();
        }

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, godot);
        MyLocationListener myLocationListener = new MyLocationListener();

        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0,
                0,
                myLocationListener
        );
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "QiblaLocationFinder";
    }

    @NonNull
    @Override
    public List<String> getPluginMethods() {
        return Arrays.asList("getDirection", "get_location_on");
    }

    public boolean get_location_on() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (userLocation != null) {
            if (event.sensor == accelerometer)
                mGravity = event.values;

            if (event.sensor == magnetometer)
                mGeomagnetic = event.values;

            if (mGravity != null && mGeomagnetic != null) {
                float[] R = new float[9];

                boolean success = SensorManager.getRotationMatrix(R, null, mGravity, mGeomagnetic);

                if (success) {
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    azimut = orientation[0]; // orientation contains: azimut, pitch and roll
                }
            }


            if (azimut != null) {
//                Log.d("AZIMUT", Float.toString(azimut));
                head = (float) Math.toDegrees(azimut);

                GeomagneticField geoField = new GeomagneticField(Double.valueOf(userLocation.getLatitude()).floatValue(), Double
                        .valueOf(userLocation.getLongitude()).floatValue(),
                        Double.valueOf(userLocation.getAltitude()).floatValue(),
                        System.currentTimeMillis());

                head -= geoField.getDeclination(); // converts magnetic north into true north


                direction = bearTo - head;

                if (direction < 0) {
                    direction += 360;
                }

            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public float getDirection() {
        return direction;
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            userLocation = location;

            bearTo = userLocation.bearingTo(destinationLoc);

            if (bearTo < 0) {
                bearTo = bearTo + 360;
            }
        }
    }
}

