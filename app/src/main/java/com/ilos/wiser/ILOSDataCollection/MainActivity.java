package com.ilos.wiser.ILOSDataCollection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.ilos.wiser.dottracker.Info;
import com.ilos.wiser.dottracker.OpenCVPath;
import com.ilos.wiser.dottracker.R;
import com.ilos.wiser.dottracker.StepCalibrationPath;

//Author: Mitchell Cooke -> cookem4@mcmaster.ca
//Date: May-August 2018

//TODO IMPORTANT READ HERE FOR PERSONALIZING APP
//This application uses the MapBox API and requires an access token to display a map
//To change start location that the map zooms to, go to activity_open_cvpath and change the following in XML:
//mapbox:mapbox_cameraTargetLat="43.261203"
//mapbox:mapbox_cameraTargetLng=" -79.919288"
//Set the above to desired location
//For tracking foot motion, magenta and cyan dots are used. The HSV min & max values for each type of dot can be modified in Object.java
//Any other color or dot can be used as long as the HSV colour ranges are changed in the Object.java
//TODO IMPORTANT READ HERE FOR PERSONALIZING APP

public class MainActivity extends AppCompatActivity {

    Button openCVCam;
    Button stepBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(com.ilos.wiser.dottracker.R.layout.activity_main);
        goRequestPermissions();
        openCVCam = (Button)findViewById(R.id.openCVCamBtn);
        stepBtn = (Button)findViewById(R.id.calibrateBtn);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        openCVCam.setAlpha((float)0.8);
        stepBtn.setAlpha((float)0.8);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Main Menu");

    }
    void goRequestPermissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                }
            }
            else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

            }
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
                }
            }
            else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},2);

            }
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 1000);
                }
            }
            else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},1);

            }
        }
    }
    public void goToOpenCVCam(View v){
        Intent intent = new Intent(this, OpenCVPath.class);
        startActivity(intent);
    }
    public void goToCalibration(View v){
        Intent intent = new Intent(this, StepCalibrationPath.class);
        startActivity(intent);
    }
    public void goToInfo(View v){
        Intent Intent = new Intent(this, Info.class);
        startActivity(Intent);
    }
}


