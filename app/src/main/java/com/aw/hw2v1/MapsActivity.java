package com.aw.hw2v1;

import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener,
        SensorEventListener
{
    private GoogleMap mMap;
    List<Point> points;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private SensorEventListener seListener;
    private Point accelerometerValues;

    private FloatingActionButton accelerometerFloatingButton;
    private FloatingActionButton closeFloatingButton;
    private TextView accelerometerTextView;

    private final String POINTS_JSON_FILE = "points.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        points = new ArrayList<Point>();
        restoreFromJson();

        accelerometerTextView = findViewById(R.id.accelerometer);
        accelerometerTextView.setVisibility(View.INVISIBLE);

        accelerometerFloatingButton = findViewById(R.id.accelerometerFloatingButton);
        accelerometerFloatingButton.setVisibility(View.INVISIBLE);

        closeFloatingButton = findViewById(R.id.closeFloatingButton);
        closeFloatingButton.setVisibility(View.INVISIBLE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        seListener = this;
        mSensorManager.registerListener(seListener, mSensor,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);

        for(Point point : points) {
            LatLng wsp = new LatLng(point.x, point.y);
            String title = "Acceleration: x = " + round(point.x) + ", y = " + round(point.y);
            mMap.addMarker(new MarkerOptions().position(wsp).title(title));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(wsp));
        }

        final Button clearButton = this.findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoogleMap mGoogleMap;
                points.clear();
                mGoogleMap = googleMap;
                mGoogleMap.clear();
            }
        });

        accelerometerFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(accelerometerTextView.getVisibility() != View.VISIBLE){
                    String txt = "Coordinates: x = " + round(accelerometerValues.x) + ", y = " + round(accelerometerValues.y);
                    accelerometerTextView.setText(txt);
                    mSensorManager.registerListener(seListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
                    accelerometerTextView.setVisibility(View.VISIBLE);
                } else {
                    accelerometerTextView.setVisibility(View.INVISIBLE);
                    mSensorManager.unregisterListener(seListener);
                }
            }
        });

        closeFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accelerometerFloatingButton.setVisibility(View.INVISIBLE);
                accelerometerFloatingButton.animate().translationX(accelerometerFloatingButton.getWidth()).setDuration(400);

                closeFloatingButton.setVisibility(View.INVISIBLE);
                closeFloatingButton.animate().translationX(closeFloatingButton.getWidth()).setDuration(400);
            }
        });
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).title(latLng.latitude + " " + latLng.longitude));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        Point p = new Point(latLng.latitude, latLng.longitude);
        points.add(p);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        String title = "Coordinates: x = " + round(marker.getPosition().latitude) + ", y = " + round(marker.getPosition().longitude);
        marker.setTitle(title);
        accelerometerFloatingButton.animate().translationX(0).setDuration(400);
        accelerometerFloatingButton.setVisibility(View.VISIBLE);

        closeFloatingButton.animate().translationX(0).setDuration(400);
        closeFloatingButton.setVisibility(View.VISIBLE);

        return false;
    }

    private void savePointToJson() {
        Gson gson = new Gson();
        String listJson = gson.toJson(points);
        FileOutputStream outputStream;
        try{
            outputStream = openFileOutput(POINTS_JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreFromJson() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput(POINTS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n < DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<Point>>() {
            }.getType();
            List<Point> o = gson.fromJson(readJson, collectionType);
            if(o != null) {
                points.clear();
                for(Point point : o) {
                    points.add(point);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearMap() {
        points.clear();
    }

    private double round(double number) {
        number = Math.round(number * 100);
        number = number / 100;

        return number;
    }

    @Override
    protected void onStop() {
        savePointToJson();
        super.onStop();
    }

    public void zoomInClick(View v) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double x = event.values[0];
        double y = event.values[1];
        accelerometerValues = new Point(x, y);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
