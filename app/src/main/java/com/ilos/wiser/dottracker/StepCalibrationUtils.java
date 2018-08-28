package com.ilos.wiser.dottracker;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ilos.wiser.ILOSDataCollection.StepDetector;
import com.ilos.wiser.ILOSDataCollection.StepListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class StepCalibrationUtils extends AppCompatActivity implements SensorEventListener, StepListener {


    public static double startLat;
    public static double startLong;
    public static double endLat;
    public static double endLong;
    //The check point is an optional turn point to calibrate user turning speed if chosen
    public static List<Double> checkPointLat = new ArrayList<>();
    public static List<Double> checkPointLong = new ArrayList<>();
    public static String userName = null;
    boolean switchChecked = false;
    public static boolean turnEnabled = false;
    double lat = 0;
    double lon = 0;
    int stepCount = 0;
    Handler mRepeatHandler;
    Runnable mRepeatRunnable;
    SensorManager SM;
    StepDetector simpleStepDetector;
    Sensor accel;
    Sensor gyro;
    Switch aSwitch;
    TextView stepText;
    double pathDistance;
    final int UPDATE_INTERVAL = 100;
    double stepLength = 0;
    long startTime;
    long endTime;
    double stepTime;
    public static float SENSITIVITY;
    double userTurnSpeed;
    List<Double> gyroList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startTime = System.currentTimeMillis();
        setContentView(R.layout.activity_step_calibration_utils);
        lat = startLat;
        lon = startLat;
        aSwitch = (Switch)findViewById(R.id.startStepSwitch);
        stepText = (TextView) findViewById(R.id.stepText);
        SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyro = SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        SM.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_FASTEST);
        accel = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.STEP_THRESHOLD = SENSITIVITY;
        System.out.println("SENSITIVITY" + simpleStepDetector.STEP_THRESHOLD);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Step Calibration");
        simpleStepDetector.registerListener(this);
        if(turnEnabled){

        }
        getPathDistance();
        mRepeatHandler = new Handler();
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                if(!isChecked){
                    //Once switch becomes unchecked
                    switchChecked = false;
                    saveProfile();
                }
                else{
                    stepCount = 0;
                    SM.registerListener(StepCalibrationUtils.this, accel, SensorManager.SENSOR_DELAY_FASTEST);
                    switchChecked = true;
                    mRepeatRunnable.run();
                }
            }
        });
        mRepeatRunnable = new Runnable() {
            @Override
            public void run() {
                if(switchChecked) {
                    //Users a timer to update the text on screen after a given interval
                    updateText();
                    mRepeatHandler.postDelayed(mRepeatRunnable, UPDATE_INTERVAL);
                }
            }
        };
        mRepeatHandler.postDelayed(mRepeatRunnable, UPDATE_INTERVAL);

    }
    SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                //Adds gyroscope values to a list when the user walks their path. Only uses this data if the turn option has been enabled
                double magnitude = Math.sqrt(Math.pow(event.values[0],2) + Math.pow(event.values[1],2) + Math.pow(event.values[2],2));
                gyroList.add(magnitude);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    //Update UI call from the timer
    void updateText(){
        stepText.setText(userName + "\n" + "Number of Steps: " + stepCount + "\n" + "Path Distance: " + pathDistance + "\n" +"Flip Switch Once Path is Completed");
    }
    void getPathDistance(){
        //Finds distance between two coordinates in metres
        double degreeDistance = Math.sqrt(Math.pow((startLat - endLat),2) + Math.pow((startLong - endLong),2));
        double R = 6378.137; // Radius of earth in KM
        double dLat = startLat * Math.PI / 180 - endLat* Math.PI / 180;
        dLat = Math.abs(dLat);
        double dLon = startLong * Math.PI / 180 - endLong* Math.PI / 180;
        dLon = Math.abs(dLon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(endLat * Math.PI / 180) * Math.cos(startLat* Math.PI / 180) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance;
        distance = R * c;
        distance = distance * 1000f;
        pathDistance = distance;

    }
    //Finds largest magnitude from the user's turn. Added to the file to indicate how quickly the user turns
    double getLargestMagnitude(){
        double max = 0;
        for(int i = 0; i < gyroList.size();i++){
            if(gyroList.get(i)>max){
                max = gyroList.get(i);
            }
        }
        return max;
    }
    void saveProfile(){
        if(turnEnabled) {
            userTurnSpeed = getLargestMagnitude();
        }
        try {
            endTime = System.currentTimeMillis();
            stepTime = (endTime - startTime)/stepCount;
            stepTime/=1000;
            stepLength = pathDistance/stepCount;
            if(turnEnabled){
                stepText.setText("Calculated Step Length is: " + stepLength + "\n" + "Average time for one step is: " + stepTime + "\n" + "Turning speed is: " + String.format("%.3f", userTurnSpeed) + "rad/s" + "\n" +"Flip switch to redo step calibration");
            }
            else {
                stepText.setText("Calculated Step Length is: " + stepLength + "\n" + "Average time for one step is: " + stepTime + "\n" + "Flip switch to redo step calibration");
            }

            Log.i("START TIME", Long.toString(startTime));
            Log.i("END TIME", Long.toString(endTime));

            Toast.makeText(getBaseContext(), "Storing Data", Toast.LENGTH_LONG).show();
            //save data
            File sdCard = Environment.getExternalStorageDirectory();
            File directory = new File(sdCard.getAbsolutePath() + "/DataCollectProfiles");
            directory.mkdirs();
            String filename = userName + ".txt";
            File file = new File(directory, filename);
            PrintWriter out = new PrintWriter(file);
            if(turnEnabled){
                out.write(Double.toString(stepLength) + "," + Double.toString(stepTime) + "," + Float.toString(SENSITIVITY) + "," + Double.toString(userTurnSpeed));
            }
            else {
                out.write(Double.toString(stepLength) + "," + Double.toString(stepTime) + "," + Float.toString(SENSITIVITY));
            }
            out.close();
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (switchChecked && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    //from the stepdetector class
    @Override
    public void step(long timeNs) {
        if(switchChecked == true) {
            stepCount++;
        }
        else{
            stepText.setText("Scan complete, flip switch to begin new data collection");
            stepCount = 0;
        }
    }
}

