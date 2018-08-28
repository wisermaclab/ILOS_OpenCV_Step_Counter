package com.ilos.wiser.dottracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;


//TODO the purpose and format of this is nearly the same as the other classes used to display the map
//TODO perhaps these can all be merged into one single class

public class OpenCVPath extends AppCompatActivity {
    MapView mapView;
    int mapClickCount = 0;
    String markerTitle;
    LatLng startClick;
    LatLng endClick;
    EditText buildingName;
    LinearLayout buildingLinLay;
    EditText usersName;
    LinearLayout nameLinLay;
    int currentFloor = 1;
    public volatile List<LatLng> pointList = new ArrayList<>();
    public volatile List<Integer> taggedFloors = new ArrayList<>();
    //Poly line list for drawing paths
    public volatile List<PolylineOptions> polyLinesList = new ArrayList<>();
    MapboxMap map;
    public final static String MAPBOX_LAYER_STRING = "layer";
    public final static String MAPBOX_ROOM_STRING = "rooms";
    public final static String MAPBOX_LABELS_STRING = "labels";
    public final static String MAPBOX_ELEVATOR = "elevator";
    public final static int MAPBOX_LAYER_CHOICE_ROOM = 0;
    public final static int MAPBOX_LAYER_CHOICE_LABELS = 1;
    public final static int MAPBOX_LAYER_CHOICE_FILL = 2;
    public final static int MAPBOX_LAYER_CHOICE_STAIR = 3;
    public final static int MAPBOX_LAYER_CHOICE_WASHROOM = 4;
    public final static int MAPBOX_LAYER_CHOICE_ELEVATOR = 5;
    public final static String MAPBOX_FILL_STRING = "fill";
    public final static String MAPBOX_WASHROOM = "washroom";
    public final static String MAPBOX_STAIRCASE = "staircase";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermisions();
        Mapbox.getInstance(this, "pk.eyJ1Ijoid2lzZXJsYWJ0ZW1wIiwiYSI6ImNqa2NsM2UxeDJ3bnkzcW1lemhndHJyNXoifQ.bvW1WnTq3dUrSjBF49Y8Sw");
        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File(sdCard.getAbsolutePath() + "/" + "GaitFootCoordsBlue");
        directory.mkdirs();
        setContentView(R.layout.activity_open_cvpath);
        mapView = (MapView) findViewById(R.id.mapView2);
        mapView.onCreate(savedInstanceState);
        usersName = (EditText)findViewById(R.id.nameText2);
        nameLinLay = (LinearLayout) findViewById(R.id.nameLinLay2);
        nameLinLay.setBackgroundColor(Color.WHITE);
        nameLinLay.setAlpha((float)0.8);
        buildingName = (EditText)findViewById(R.id.buildingText2);
        buildingLinLay = (LinearLayout)findViewById(R.id.buildingLinLays2);
        buildingLinLay.setBackgroundColor(Color.WHITE);
        buildingLinLay.setAlpha((float)0.8);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Data Collection");
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                drawCollectedPaths();
                mapboxMap.addOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(@NonNull LatLng point) {
                        mapClickCount++;
                        if(mapClickCount <= 2) {
                            if(mapClickCount == 1){
                                markerTitle = "Start";
                                startClick = point;
                            }
                            else{
                                markerTitle = "End";
                                endClick = point;
                            }
                            mapboxMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(point.getLatitude(), point.getLongitude()))
                                    .title(markerTitle)
                                    .snippet(Double.toString(point.getLatitude()) + "," + Double.toString(point.getLongitude())));
                        }
                    }
                });
            }
        });
    }
    public void increaseMapBoxLayer(View v){
        if(currentFloor < 8) {
            currentFloor++;
            map.clear();
            drawCollectedPaths();
            mapClickCount = 0;
            loopLayers();
        }
    }
    public void decreaseMapBoxLayer(View v){
        if(currentFloor < 8) {
            currentFloor--;
            map.clear();
            drawCollectedPaths();
            mapClickCount = 0;
            loopLayers();
        }
    }
    void loopLayers(){
        for (int i = -1; i < 8; i++) {
            if (i == currentFloor) {
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_FILL, currentFloor)).setProperties(visibility(VISIBLE));
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_ROOM, currentFloor)).setProperties(visibility(VISIBLE));
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_LABELS, currentFloor)).setProperties(visibility(VISIBLE));
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_WASHROOM, currentFloor)).setProperties(visibility(VISIBLE));
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_STAIR, currentFloor)).setProperties(visibility(VISIBLE));
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_ELEVATOR, currentFloor)).setProperties(visibility(VISIBLE));
            } else {
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_FILL, i)).setProperties(visibility(NONE));
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_ROOM, i)).setProperties(visibility(NONE));
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_LABELS, i)).setProperties(visibility(NONE));
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_WASHROOM, i)).setProperties(visibility(NONE));
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_STAIR, i)).setProperties(visibility(NONE));
                map.getLayer(getLayerName(MAPBOX_LAYER_CHOICE_ELEVATOR, i)).setProperties(visibility(NONE));
            }
        }
    }
    String getLayerName(int choice, int floor){
        switch (choice) {
            case MAPBOX_LAYER_CHOICE_ROOM:
                return MAPBOX_LAYER_STRING + floor + MAPBOX_ROOM_STRING;
            case MAPBOX_LAYER_CHOICE_LABELS:
                return MAPBOX_LAYER_STRING + floor + MAPBOX_LABELS_STRING;
            case MAPBOX_LAYER_CHOICE_FILL:
                return MAPBOX_LAYER_STRING + floor + MAPBOX_FILL_STRING;
            case MAPBOX_LAYER_CHOICE_WASHROOM:
                return MAPBOX_LAYER_STRING + floor + MAPBOX_WASHROOM;
            case MAPBOX_LAYER_CHOICE_STAIR:
                return MAPBOX_LAYER_STRING + floor + MAPBOX_STAIRCASE;
            case MAPBOX_LAYER_CHOICE_ELEVATOR:
                return MAPBOX_LAYER_STRING + floor + MAPBOX_ELEVATOR;
            default:
                return MAPBOX_LAYER_STRING + "1" + MAPBOX_ROOM_STRING;
        }
    }
    public void goToOpenCV(View v){
        //TODO add information that changes fields of the openCV step counter
        if(startClick!=null && endClick!=null && buildingName.getText().toString().length() != 0 && usersName.getText().toString().length() != 0) {
            Intent Intent = new Intent(this, OpenCVStepCounter.class);
            OpenCVStepCounter.startLat = startClick.getLatitude();
            OpenCVStepCounter.startLong = startClick.getLongitude();
            OpenCVStepCounter.endLat = endClick.getLatitude();
            OpenCVStepCounter.endLong = endClick.getLongitude();
            OpenCVStepCounter.FLOOR_NUMBER = Integer.toString(currentFloor);
            OpenCVStepCounter.BUILDING_NAME = buildingName.getText().toString();
            OpenCVStepCounter.USER_NAME = usersName.getText().toString();
            startActivity(Intent);
        }
        else{
            if (buildingName.getText().toString().length() == 0 || usersName.getText().toString().length() == 0) {
                Toast.makeText(getBaseContext(), "Please select a building and user profile", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(getBaseContext(), "Please select start and end points", Toast.LENGTH_LONG).show();
            }
        }
    }
    void drawCollectedPaths(){
        try {
            //save data
            File sdCard = Environment.getExternalStorageDirectory();
            File directory = new File(sdCard.getAbsolutePath() + "/GaitFootCoordsBlue");
            Log.i("Save Dir", sdCard.getAbsolutePath() + "/GaitFootCoordsBlue");
            directory.mkdirs();
            String filename = "README.txt";
            File file = new File(directory, filename);
            PrintWriter out = new PrintWriter(file);
            out.write("This folder contains the path information collected.");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        getPoints("GaitFootCoordsBlue");
        for(int i = 0; i < pointList.size();i++){
            if(taggedFloors.get(i) == currentFloor) {
                map.addPolyline(polyLinesList.get(i)
                        .add(pointList.get(i))
                        .width(3)
                        .color(this.getResources().getColor(R.color.colorPrimaryDark)));
            }
        }
    }
    //Gets points based on filenames in local storage
    void getPoints(String fileSource){
        try {
            List<String> fileNames = new ArrayList<>();
            File sdCard = Environment.getExternalStorageDirectory();
            File[] directory = new File(sdCard.getAbsolutePath() + "/" + fileSource).listFiles();
            //Adds all file names to a list
            for (int i = 0; i < directory.length; i++) {
                if (directory[i].isFile() && directory[i].toString().indexOf("README")==-1) {
                    fileNames.add(directory[i].toString());
                }
            }
            //File names are formatted: BUILDING(startLat,startLon)to(endLat,endLon).txt
            for (int i = 0; i < fileNames.size(); i++) {
                String startPoint;
                String endPoint;
                int openBracket = fileNames.get(i).indexOf("(");
                int closeBracket = fileNames.get(i).indexOf(")");
                startPoint = fileNames.get(i).substring(openBracket + 1, closeBracket);
                endPoint = fileNames.get(i).substring(closeBracket + 4, fileNames.get(i).length() - 5);
                //0th index is lat, 1st index is lon
                String[] start = startPoint.split(",");
                String[] end = endPoint.split(",");
                taggedFloors.add(Integer.parseInt(Character.toString(fileNames.get(i).charAt(openBracket - 1))));
                taggedFloors.add(Integer.parseInt(Character.toString(fileNames.get(i).charAt(openBracket - 1))));
                pointList.add(new LatLng(Double.parseDouble(start[0]), Double.parseDouble(start[1])));
                pointList.add(new LatLng(Double.parseDouble(end[0]), Double.parseDouble(end[1])));
                //A single polyItem is added to two different numbered entries corresponding to the start and ending numbers
                PolylineOptions polyItem = new PolylineOptions();
                polyLinesList.add(polyItem);
                polyLinesList.add(polyItem);
            }
        }
        catch(Exception e){
            polyLinesList = null;
            pointList = null;
            taggedFloors = null;
        }
    }
    public void clearMarkers(View v){
        mapClickCount = 0;
        map.clear();
        drawCollectedPaths();
    }
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
    void requestPermisions(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1000);
                }
            }
            else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},1);

            }
        }
    }
}
