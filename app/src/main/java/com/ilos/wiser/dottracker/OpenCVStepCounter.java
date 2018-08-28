package com.ilos.wiser.dottracker;

import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Math;

import com.ilos.wiser.ILOSDataCollection.Object;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.round;

public class OpenCVStepCounter extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {


    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

    // Used in Camera selection from menu (when implemented)
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;
    boolean collectData = false;
    //Displays coordinates of each circle and the sensor information
    public volatile TextView coordText;
    //Displays the number of steps occured and user prompts
    public volatile TextView stepText;
    //Tracks the number of times the record button has been pressed
    int clickCount = 0;
    //Adds all objects of the respective colour when they appear on the screen in order to hold information of theri respective fields
    List<com.ilos.wiser.ILOSDataCollection.Object> redList = new ArrayList<Object>();
    List<com.ilos.wiser.ILOSDataCollection.Object> blueList  = new ArrayList<com.ilos.wiser.ILOSDataCollection.Object>();
    List<com.ilos.wiser.ILOSDataCollection.Object> greenList  = new ArrayList<com.ilos.wiser.ILOSDataCollection.Object>();
    int counter = 1;
    //Steps of red foot vs blue foot
    public volatile int blueNumSteps = 0;
    public volatile int redNumSteps = 0;
    //Used to track how long each dot dissapears from the screen
    long blueGone = 0;
    long blueStart = 1;
    long blueEnd = 0;
    long redGone = 0;
    long redStart = 1;
    long redEnd = 0;
    //Tracks which colour has passed the line to register a step
    boolean redGreenLine = false;
    boolean blueGreenLine = false;
    final double peakThresh = 5; //Number of frames for minimum peak distances
    //Minimum area to register an object on screen
    final int MIN_AREA = 1000;
    public static String USER_NAME;
    public static double startLat;
    public static double startLong;
    public static double endLat;
    public static double endLong;
    public static String BUILDING_NAME;
    public static String FLOOR_NUMBER;
    double STEP_TIME_ORIGINAL;
    double STEP_LENGTH;
    //Used to track from the position of the red or blue object if its position is increasing or decreasing. For registering steps
    boolean realTimeIncreaseRed = false;
    boolean realTimeIncreaseBlue = false;
    boolean steppedOnceRed = false;
    boolean steppedOnceBlue = false;
    int stepCount = 0;
    EditText nameText;

    //Following variabels used for tracking and adding to path distance
    double totalNumSteps;
    double xCompMotion;
    double yCompMotion;
    double pathDistance;
    double degToMRatio;
    boolean finishedCollection;
    double lat;
    double lon;
    volatile double displayLat;
    volatile double dispalyLon;
    boolean xIncreasing;
    boolean yIncreasing;

    private Sensor myMagnetometer;
    private Sensor myAccel;
    private Sensor myGyro;
    //Output data that incorporates timestamps with data. For the lists of data, the index corresponds to the same index on the timestamp list
    List<float[]> magneticList = new ArrayList<>();
    List<Long> magTimeStamps = new ArrayList<>();
    List<LatLng> magCoords = new ArrayList<>();
    List<float[]> accelList = new ArrayList<>();
    List<Long> accelTimeStamp = new ArrayList<>();
    List<LatLng> accelCoords = new ArrayList<>();
    List<float[]> gyroList = new ArrayList<>();
    List<Long> gyroTimeStamp = new ArrayList<>();
    List<LatLng> gyroCoords = new ArrayList<>();
    List<Long> stepTimeStamp = new ArrayList<>();
    List<double[]> outputRedList = new ArrayList<>();
    List<Long> redFootTimeStamp = new ArrayList<>();
    List<Long> blueFootTimeStamp = new ArrayList<>();
    List<double[]> outputBlueList = new ArrayList<>();
    List<Integer> redFrameList = new ArrayList<>();
    List<Integer> blueFrameList = new ArrayList<>();
    List<LatLng> redGlobalCoords = new ArrayList<>();
    List<LatLng> blueGlobalCoords = new ArrayList<>();
    boolean flashAvailable = false;
    boolean enableVideo = false;
    private SensorManager SM;
    public static Camera cam = null;
    public static Camera.Parameters params = null;
    VideoWriter writer = new VideoWriter();
    String fileName;
    static {
        OpenCVLoader.initDebug();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_open_cvstep_counter);
        getStepProfile();
        getMotionInfo();
        lat = startLat;
        lon = startLong;
        displayLat = startLat;
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Data Collection");
        dispalyLon = startLong;
        if(endLat - startLat > 0){
            xIncreasing = true;
        }
        else{
            xIncreasing = false;
        }
        if(endLong - startLong > 0){
            yIncreasing = true;
        }
        else{
            yIncreasing = false;
        }

        SM.registerListener(listener, myMagnetometer, SensorManager.SENSOR_DELAY_FASTEST);
        SM.registerListener(listener, myAccel, SensorManager.SENSOR_DELAY_FASTEST);
        SM.registerListener(listener, myGyro, SensorManager.SENSOR_DELAY_FASTEST);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        coordText = (TextView)findViewById(R.id.coordText);
        coordText.setText("Press the record button!");
        stepText = (TextView)findViewById(R.id.stepRecorder);
        stepText.setText(USER_NAME + "'s step length: " + String.format("%.3f", STEP_LENGTH) + "m" + "\n" + "Coordinates: " + "\n" + String.format("%.8f", displayLat) + ", " + String.format("%.8f",dispalyLon));
        //This speeds things up. Decrase frame size greatly to allow for more FPS
        mOpenCvCameraView.setMaxFrameSize(400,400);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableView();
        displayToast();
    }
    //Adds sensor data. Will be tagged with location at which it occurred
    SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (this) {
                if (collectData && stepTimeStamp.size() > 0) {
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        accelList.add(event.values);
                        accelTimeStamp.add(System.nanoTime());
                    }
                    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                        magneticList.add(event.values);
                        magTimeStamps.add(System.nanoTime());
                    }
                    if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                        gyroList.add(event.values);
                        gyroTimeStamp.add(System.nanoTime());
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC3);
        mRgbaF = new Mat(height, width, CvType.CV_8UC3);
        mRgbaT = new Mat(width, width, CvType.CV_8UC3);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    //This is the important function that runs for every new openCV frame. All openCV operations are based off of this
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Imgproc.resize(mRgba, mRgba, new Size(100,100));
        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );
        //Blurs for smoother edgges
        Imgproc.blur(mRgba, mRgba, new Size(8,8));

        //Adds the counter to the instantiation of each object as it counts the frame number
        Object blue = new Object("blue", counter);
        Object green = new Object("green", counter);
        Object red = new Object("red", counter);

        Mat threshold = new Mat();
        Mat HSV = new Mat();
        //Changes from BGR to HSV as HSV allow much easier colour ranges for creating a binary mat
        Imgproc.cvtColor(mRgba,HSV,Imgproc.COLOR_RGB2HSV);
        //Creates blue binary mat
        Core.inRange(HSV, blue.HSVMin, blue.HSVMax, threshold);
        morphOps(threshold);
        trackFilteredObject(blue,threshold,HSV,mRgba, blue.type, counter);

        //TODO disabled the green markers for now
        /*
        Imgproc.cvtColor(mRgba,HSV,Imgproc.COLOR_BGR2HSV);
        Core.inRange(HSV, green.HSVMin, green.HSVMax, threshold);
        morphOps(threshold);
        trackFilteredObject(green,threshold,HSV,mRgba, green.type, counter);
        */
        //creates red binary mat
        Imgproc.cvtColor(mRgba,HSV,Imgproc.COLOR_BGR2HSV);
        Core.inRange(HSV, red.HSVMin, red.HSVMax, threshold);
        morphOps(threshold);
        trackFilteredObject(red,threshold,HSV,mRgba, red.type, counter);

        //Colours the line that registers if a step has occured accordingly
        if(redGreenLine || blueGreenLine){
            Imgproc.line(mRgba, new Point(0, 150), new Point(500, 150), new Scalar(0, 255, 0), 2);
        }
        else if(!redGreenLine && !redGreenLine){
            Imgproc.line(mRgba, new Point(0, 150), new Point(500, 150), new Scalar(255, 0, 0), 2);
        }
        //Will write each frame to storage once the reoord button is pressed. This is used for setting ground truth data
        if(enableVideo) {
            Mat output = new Mat();
            Imgproc.cvtColor(mRgba, output, Imgproc.COLOR_RGB2BGR);
            Imgproc.resize(output, output, new Size(100,100));
            Imgcodecs.imwrite(fileName + BUILDING_NAME + FLOOR_NUMBER + "(" + startLat + "," + startLong + ")" + "to" + "(" + endLat + "," + endLong + ")" + Integer.toString(counter) + ".bmp", output);
        }

        counter++;

        return mRgba; // This function must return
    }
    //Applies basic transformation to the image to allow detecting the colours to be easier
    void morphOps(Mat thresh) {
        Imgproc.blur(thresh, thresh, new Size(10,10));
        Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2));
        Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7,7));
        Imgproc.erode(thresh, thresh, erodeElement);
        Imgproc.dilate(thresh, thresh, dilateElement);
    }
    //Does all of the detection from the binary image to find the centre points of each circle
    void trackFilteredObject(com.ilos.wiser.ILOSDataCollection.Object theObject, Mat threshold, Mat HSV, Mat cameraFeed, String colour, int counter) {
        ArrayList<com.ilos.wiser.ILOSDataCollection.Object> Objects = new ArrayList<>();
        Mat temp = new Mat();
        threshold.copyTo(temp);
        //Contours track the edges of the binary image
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        boolean redFound = false;
        boolean blueFound = false;
        //Copies contours to the list contours
        Imgproc.findContours(temp,contours,hierarchy,Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
        boolean objectFound = false;
        if (!hierarchy.empty()) {
            for (int i = 0; i >=0; i = (int)hierarchy.get(i, 0)[1]) {
                MatOfPoint contour = contours.get(i);
                Moments moment = Imgproc.moments(contour);
                double area = moment.m00;
                if(area > MIN_AREA) {
                    com.ilos.wiser.ILOSDataCollection.Object object = new com.ilos.wiser.ILOSDataCollection.Object(counter);
                    //sets fields for the object based on the moment class
                    object.setXPos(moment.m10/area);
                    object.setYPos(moment.m01/area);
                    object.type = theObject.type;
                    object.colour = theObject.colour;
                    //Tags if each object has been found to track how long it has been off screen for
                    if(object.colour == new Scalar(255,0,0)){
                        redFound = true;
                    }
                    if(object.colour == new Scalar(0,255,0)){
                        blueFound = true;
                    }
                    //Appends a list of the object that are seen on the current frame
                    Objects.add(object);

                    objectFound = true;

                }else {objectFound = false;}
            }
            if(!redFound){
                if(redStart==1) {
                    redStart = System.currentTimeMillis();
                }
                else{
                    redEnd = System.currentTimeMillis();
                }
                if(redStart!=1 && redEnd!=0) {
                    redGone = redEnd - redStart;
                    if(redGone > 300) {
                        System.out.println("RED OFF SCREEN: " + redGone);
                    }
                    redStart = 1;
                    redEnd = 0;
                }

            }
            if(!blueFound){
                if(blueStart==1) {
                    blueStart = System.currentTimeMillis();
                }
                else{
                    blueEnd = System.currentTimeMillis();
                }
                if(blueStart!=1 && blueEnd!=0) {
                    blueGone = blueEnd - blueStart;
                    if(blueGone > 300) {
                        System.out.println("BLUE OFF SCREEN" + blueGone);
                    }
                    blueStart = 1;
                    blueEnd = 0;
                }
            }
            if(objectFound == true && collectData) {
                //This does all of the highlighting of the circles on the scren
                drawObject(Objects, mRgba, temp, contours, hierarchy);
            }
            //If collection is finished
            if(!collectData && redList.size()>0 && blueList.size()>0){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        coordText.setText("Press back to select a new path!");
                    }
                });
             }
        }
    }
    void drawObject(ArrayList<com.ilos.wiser.ILOSDataCollection.Object> theObjects, Mat finalFrame, Mat temp, List<MatOfPoint> contours, Mat hierarchy) {
        boolean isBlue = false;
        boolean isRed = false;
        boolean isGreen = false;
        for (int i = 0; i < theObjects.size(); i++) {
            if(theObjects.get(i).colour.equals(new Scalar(255.0,0.0,0.0,0.0))) {
                redList.add(theObjects.get(i));
                if(theObjects.get(i).getYPos()<150){
                    redGreenLine = true;
                }
                else{
                    redGreenLine = false;
                }
                isRed = true;
            }
            if(theObjects.get(i).colour.equals(new Scalar(0.0,255.0,0.0,0.0))) {
                greenList.add(theObjects.get(i));
                isGreen = true;
            }
            if(theObjects.get(i).colour.equals(new Scalar(0.0,0.0,255.0,0.0))) {
                blueList.add(theObjects.get(i));
                if(theObjects.get(i).getYPos()<150){
                    blueGreenLine = true;
                }
                else{
                    blueGreenLine = false;
                }
                isBlue = true;
            }
            //For the following if statements, an index of two less than the length gives the last item added on
            //Checks that the next point drawn is within 50 pixels between then x and y distances
            if(isBlue && blueList.size() > 2) {
                //Only draws under certain conditions. i.e. the last object was within sqrt(5000) pixels
                if ((theObjects.get(i).getXPos() - blueList.get(blueList.size()-2).getXPos() < 50 && theObjects.get(i).getYPos() - blueList.get(blueList.size()-2).getYPos() < 50)) {
                    Imgproc.drawContours(mRgba, contours, i, theObjects.get(i).colour,3);
                    Imgproc.circle(mRgba, new Point(theObjects.get(i).getXPos(), theObjects.get(i).getYPos()), 3, theObjects.get(i).colour, 10);
                }
            }
            if(isRed && redList.size() > 2) {
                if ((theObjects.get(i).getXPos() - redList.get(redList.size()-2).getXPos() < 50 && theObjects.get(i).getYPos() - redList.get(redList.size()-2).getYPos() < 50)) {
                    Imgproc.drawContours(mRgba, contours, i, theObjects.get(i).colour,3);
                    Imgproc.circle(mRgba, new Point(theObjects.get(i).getXPos(), theObjects.get(i).getYPos()), 3, theObjects.get(i).colour, 10);
                }
            }
            if(isGreen && greenList.size() > 2) {
                if (theObjects.get(i).getYPos() - greenList.get(greenList.size()-2).getYPos() < 50) {
                    Imgproc.drawContours(mRgba, contours, i, theObjects.get(i).colour,3);
                    Imgproc.circle(mRgba, new Point(theObjects.get(i).getXPos(), theObjects.get(i).getYPos()), 3, theObjects.get(i).colour, 10);
                }
            }
        }
        //Will only run if collectData
        if(redList.size()>0 && blueList.size() >0) {
            showText();
            addToLists(isRed);
        }
        if(redList.size()>1 && blueList.size()>1 && (redGreenLine || blueGreenLine) && collectData) {
            realTimeProcess();
        }
        //Checks to see if each foot has reached its peak
        if (!redGreenLine) {
            steppedOnceRed = false;
        }
        if(!blueGreenLine){
            steppedOnceBlue = false;
        }
    }
    void addToLists(boolean isRed){
        if(isRed){
            redFootTimeStamp.add(System.nanoTime());
            outputRedList.add(new double[]{redList.get(redList.size() - 1).getXPos(), redList.get(redList.size() - 1).getYPos()});
            redFrameList.add(counter);
        }
        else{
            blueFootTimeStamp.add(System.nanoTime());
            outputBlueList.add(new double[]{blueList.get(blueList.size() - 1).getXPos(), blueList.get(blueList.size() - 1).getYPos()});
            blueFrameList.add(counter);
        }

    }
    //Applies a weighted average to double check the results
    List<double[]> cmwa(List<double[]> points){
        List<double[]> outputList = new ArrayList<>();
        //output list tags the y values with the frame
        outputList.add(new double[]{(points.get(0)[0]+points.get(1)[0])/2, points.get(0)[1]});
        for (int i = 1; i < points.size() - 1; i++) {
            outputList.add(new double[]{(points.get(i-1)[0] + points.get(i)[0] + points.get(i+1)[0])/3, points.get(i)[1] });
        }
        outputList.add(new double[]{(points.get(points.size() - 1)[0] + points.get(points.size()-2)[0])/2, points.get(points.size()-1)[1]});
        return outputList;
    }
    //Finds peaks after smoothing has occured
    int findMax(List<double[]> x){
        List<double[]> maxInfo = new ArrayList<>();
        boolean increasing  = false;
        double maxNum = x.get(0)[0];
        double maxFrame = 1;
        double slope = 0;
        for(int i = 0; i<x.size()-1;i++){
            System.out.println(x.get(i)[0]);
            if(x.get(i+1)[0] < x.get(i)[0]){
                increasing = true;
                slope = x.get(i+1)[0] - x.get(i)[0];
                maxNum = x.get(i+1)[0];
                maxFrame = x.get(i+1)[1];
            }
            else if(x.get(i+1)[0] > x.get(i)[0] && increasing && maxNum<150){
                System.out.println("PEAK: " + maxNum + "," + maxFrame);
                increasing = false;
                maxInfo.add(new double[]{maxNum, maxFrame});
                maxNum = 0;
                maxFrame = 0;
            }
        }
        //removes false peaks with close frame tag near each other
        for(int i = 0; i < maxInfo.size()-1;i++){
            if(maxInfo.get(i+1)[1] - maxInfo.get(i)[1] <=10){
                System.out.println("REMOVED " + maxInfo.get(i+1)[0]);
                maxInfo.remove(i+1);
            }
        }
        return maxInfo.size();
    }
    public void showText(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(collectData) {
                    try {
                        //Add information to output lists
                        String redText;
                        String blueText;
                        redText = String.format("%.1f", (redList.get(redList.size() - 1).getXPos())) + "," + String.format("%.1f", (redList.get(redList.size() - 1).getYPos()));
                        blueText = String.format("%.1f", (blueList.get(blueList.size() - 1).getXPos())) + "," + String.format("%.1f", (blueList.get(blueList.size() - 1).getYPos()));
                        String magText = "Mag Info: " + String.format("%.2f", magneticList.get(magneticList.size() - 1)[0]) + ", " + String.format("%.2f", magneticList.get(magneticList.size() - 1)[1]) + ", " + String.format("%.2f", magneticList.get(magneticList.size() - 1)[2]);
                        String gyroText = "Gyro Info" + String.format("%.2f", gyroList.get(gyroList.size() - 1)[0]) + ", " + String.format("%.2f", gyroList.get(gyroList.size() - 1)[1]) + ", " + String.format("%.2f", gyroList.get(gyroList.size() - 1)[2]);

                        coordText.setText("Red Circle Coordinate: " + redText + "\n" + "Blue Circle Coordinates: " + blueText + "\n" + magText + "\n" + gyroText);
                    }
                    catch(Exception e){
                        collectData = false;
                        coordText.setText("Object tracking error!" + "\n" + "Press back to select a new path");
                    }
                    }

                }
        });
    }
    public void collectDataBtn(View v){
        clickCount++;

        if(clickCount%2 == 1 && !finishedCollection) {
            //Sets up the ability to save each frame to device storage
            File sdCard = Environment.getExternalStorageDirectory();
            File directory = new File(sdCard.getAbsolutePath() + "/" + "GaitVideos/" +  BUILDING_NAME + FLOOR_NUMBER + "(" + startLat + "," + startLong + ")" + "to" + "(" + endLat + "," + endLong + ")/");
            directory.mkdirs();
            fileName = sdCard.getAbsolutePath() + "/" + "GaitVideos/" + BUILDING_NAME + FLOOR_NUMBER + "(" + startLat + "," + startLong + ")" + "to" + "(" + endLat + "," + endLong + ")/";
            int fourcc = VideoWriter.fourcc('M', 'J', 'P', 'G');
            writer.set(Videoio.CAP_PROP_FOURCC, fourcc);

            enableVideo = true;
            stepTimeStamp.add(System.nanoTime());
            collectData = true;
        }
        else{
            writer.release();
            enableVideo = false;
            finishedCollection = true;
            collectData = false;
        }
    }
    //Used to process if a step has occured or not in real time
    void realTimeProcess(){
        //For red list
        if(redList.get(redList.size()-1).getYPos() < redList.get(redList.size()-2).getYPos() && !steppedOnceRed && redGreenLine){
            realTimeIncreaseRed = true;
        }
        else if(redList.get(redList.size()-1).getYPos() > redList.get(redList.size()-2).getYPos() && realTimeIncreaseRed && !steppedOnceRed && redGreenLine){
            stepCount++;
            stepTimeStamp.add(System.nanoTime());
            updateStepCoordiantes();
            steppedOnceRed =true;
            System.out.println("STEP: " + redList.get(redList.size()-1).getYPos());
            realTimeIncreaseRed = false;
        }
        //for blue list
        if(blueList.get(blueList.size()-1).getYPos() < blueList.get(blueList.size()-2).getYPos() && !steppedOnceBlue && blueGreenLine){
            realTimeIncreaseBlue = true;
        }
        else if(blueList.get(blueList.size()-1).getYPos() > blueList.get(blueList.size()-2).getYPos() && realTimeIncreaseBlue && !steppedOnceBlue && blueGreenLine){
            stepCount++;
            stepTimeStamp.add(System.nanoTime());
            updateStepCoordiantes();
            steppedOnceBlue = true;
            System.out.println("STEP: " + blueList.get(blueList.size()-1).getYPos());
            realTimeIncreaseBlue = false;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stepText.setText(USER_NAME + "'s step length: " + String.format("%.3f", STEP_LENGTH) + "m" + "\n" + "Number of Steps: " + stepCount +"\n" + "Coordinates: " +"\n" + String.format("%.8f", displayLat) + ", " + String.format("%.8f",dispalyLon));
            }
        });
        Log.i("NUMBER OF STEPS", Double.toString(totalNumSteps));
        if(stepCount >= totalNumSteps){
            processScanCoordinates(blueFootTimeStamp, "blue");
            processScanCoordinates(redFootTimeStamp, "red");
            processScanCoordinates(magTimeStamps, "mag");
            processScanCoordinates(gyroTimeStamp, "gyro");
            processScanCoordinates(accelTimeStamp, "accel");
            postProcessData();

        }
    }
    void postProcessData(){
        //Just working with blue marker
        collectData = false;
        blueNumSteps = 0;
        List<double[]> inputListBlue = new ArrayList<>();
        List<double[]> outputListBlue = new ArrayList<>();
        for(int i = 0; i < blueList.size();i++){
            inputListBlue.add(new double[]{blueList.get(i).getYPos(), blueList.get(i).frame});
        }
        //Reset to zero
        while(blueList.size()>0){
            blueList.remove(0);
        }
        //Reset to zero
        for (int i = 0; i < outputListBlue.size(); i++) {
            outputListBlue.remove(0);
        }


        outputListBlue = cmwa(inputListBlue);
        outputListBlue = cmwa(outputListBlue);
        outputListBlue = cmwa(outputListBlue);
        outputListBlue = cmwa(outputListBlue);
        outputListBlue = cmwa(outputListBlue);


        blueNumSteps = findMax(outputListBlue);

        //Reset to zero
        for (int i = 0; i < inputListBlue.size(); i++) {
            inputListBlue.remove(0);
        }

        //Process red steps
        redNumSteps = 0;
        List<double[]> outputListRed = new ArrayList<>();
        List<double[]> inputListRed = new ArrayList<>();

        for(int i = 0; i < redList.size();i++){
            inputListRed.add(new double[]{redList.get(i).getYPos(), redList.get(i).frame});
        }
        //Reset to zero
        while(redList.size()>0){
            redList.remove(0);
        }
        //Reset to zero
        for (int i = 0; i < outputListRed.size(); i++) {
            outputListRed.remove(0);
        }

        outputListRed = cmwa(inputListRed);
        outputListRed = cmwa(outputListRed);
        outputListRed = cmwa(outputListRed);
        outputListRed = cmwa(outputListRed);
        outputListRed = cmwa(outputListRed);

        redNumSteps = findMax(outputListRed);

        //Reset to zero
        for (int i = 0; i < inputListRed.size(); i++) {
            inputListRed.remove(0);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                enableVideo = false;
                System.out.println("RED STEPS:"+ redNumSteps);
                System.out.println("BLUE STEPS:"+ blueNumSteps);
                if(Math.abs(blueNumSteps - redNumSteps) < 2 && Math.abs(stepCount - (blueNumSteps + redNumSteps))<2) {
                    stepText.setText(USER_NAME + "'s step length: " + String.format("%.3f", STEP_LENGTH) + "m" + "\n" + "Number of Steps: " + totalNumSteps + "\n" + "Collection Complete!");
                    saveData();
                }
                else{
                    stepText.setText(USER_NAME + "'s step length: " + String.format("%.3f", STEP_LENGTH) + "m"+"\n" + "Data Collection Error: Steps do not Match");
                }
            }
        });
    }
    void displayToast(){
        Toast.makeText(getBaseContext(), "Walk Such That Step Peaks Cross Line!", Toast.LENGTH_LONG).show();
    }
    void getMotionInfo(){
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
        degToMRatio = degreeDistance/distance;
        double angle = Math.atan(dLat/dLon);
        xCompMotion = Math.cos(angle);
        yCompMotion = Math.sin(angle);
        totalNumSteps = pathDistance/STEP_LENGTH;
        //Rounds number of steps to nearest whole number
        totalNumSteps = Math.round(totalNumSteps);
    }
    void saveData(){
        //TODO Note that when a dot is not found on the screen the coordinate registered is the same coordinate as the previous time it was on the screen
        //TODO is this the proper behaviour?

        List<String> outputData = new ArrayList<>();
        for(int i = 0; i < redFootTimeStamp.size();i++){
            //0th index is x position 1st index is y position
            //Note that the longitude and latitude coordinates tagged for this are the coordinates of the centre of the user where the phone is NOT of the exact coordinates in longitude and latitude of the feet
            System.out.println(redFootTimeStamp.size() + "," + redFrameList.size() + "," + redGlobalCoords.size() + "," + outputRedList.size());
            outputData.add(Long.toString(redFootTimeStamp.get(i)) + "," + redFrameList.get(i) + "," + Double.toString(redGlobalCoords.get(i).getLatitude()) + "|" + Double.toString(redGlobalCoords.get(i).getLongitude())+ ","  + Double.toString(outputRedList.get(i)[0]) + "|" + Double.toString(outputRedList.get(i)[1]));
        }

        writeOutputData(outputData, "GaitFootCoordsRed");
        while(outputData.size()>0){
            outputData.remove(0);
        }
        for(int i = 0; i < blueFootTimeStamp.size();i++){
            //Order is x then y position
            //Note that the longitude and latitude coordinates tagged for this are the coordinates of the centre of the user where the phone is NOT of the exact coordinates in longitude and latitude of the feet
            outputData.add(Long.toString(blueFootTimeStamp.get(i)) + "," + blueFrameList.get(i) + "," + Double.toString(blueGlobalCoords.get(i).getLatitude()) + "|" + Double.toString(blueGlobalCoords.get(i).getLongitude()) +  "," + Double.toString(outputBlueList.get(i)[0]) + "|" + Double.toString(outputBlueList.get(i)[1]));
        }

        writeOutputData(outputData, "GaitFootCoordsBlue");
        while(outputData.size()>0){
            outputData.remove(0);
        }
        for(int i = 0; i < magCoords.size();i++){
            //0th index is x position 1st index is y position
            System.out.println(i);
            System.out.println(magTimeStamps.size());
            System.out.println(magCoords.size());
            System.out.println(magneticList.size());
            outputData.add(Long.toString(magTimeStamps.get(i)) + "," + Double.toString(magCoords.get(i).getLatitude()) + "|" + Double.toString(magCoords.get(i).getLongitude()) + "," + Float.toString(magneticList.get(i)[0]) + "|" + Float.toString(magneticList.get(i)[1])+"|"+Float.toString(magneticList.get(i)[2]) );
        }
        writeOutputData(outputData, "GaitMagData");
        while(outputData.size()>0){
            outputData.remove(0);
        }
        for(int i = 0; i < accelCoords.size();i++){
            //0th index is x position 1st index is y position
            outputData.add(Long.toString(accelTimeStamp.get(i)) + "," + Double.toString(accelCoords.get(i).getLatitude()) + "|" + Double.toString(accelCoords.get(i).getLongitude()) + "," + Float.toString(accelList.get(i)[0]) + "|" + Float.toString(accelList.get(i)[1])+"|"+Float.toString(accelList.get(i)[2]) );
        }
        writeOutputData(outputData, "GaitAccelData");
        while(outputData.size()>0){
            outputData.remove(0);
        }
        for(int i = 0; i < gyroCoords.size();i++){
            //0th index is x position 1st index is y position
            outputData.add(Long.toString(gyroTimeStamp.get(i)) + "," + Double.toString(gyroCoords.get(i).getLatitude()) + "|" + Double.toString(gyroCoords.get(i).getLongitude()) + "," + Float.toString(gyroList.get(i)[0]) + "|" + Float.toString(gyroList.get(i)[1])+"|"+Float.toString(gyroList.get(i)[2]) );
        }
        writeOutputData(outputData, "GaitGyroData");

    }
    void writeOutputData(List<String> outputData, String folderName){
        try {
            //save data
            File sdCard = Environment.getExternalStorageDirectory();
            File directory = new File(sdCard.getAbsolutePath() + "/" + folderName);
            directory.mkdirs();
            String filename = BUILDING_NAME + FLOOR_NUMBER + "(" + startLat + "," + startLong + ")" + "to" + "(" + endLat + "," + endLong + ")"+ ".txt";
            File file = new File(directory, filename);
            PrintWriter out = new PrintWriter(file);
            for (int i = 0; i<outputData.size();i++) {
                out.write(outputData.get(i));
                out.write("\r\n");
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void processScanCoordinates(List<Long> scanTimeStampList, String scanType){
        //Runs after the path is walked along. Rewrites the coordinates in outputData
        //processes the coordinates of each scan based on the list of timestamps for scans and timestamps for steps
        //Make a list to store coordinates of each scan. The index number represents the scan number minus 1
        //TODO some items are lost in this algorithm
        List<LatLng> scanCoordinates = new ArrayList<>();
        System.out.println("LENGTH BEFOREEEE" + scanTimeStampList.size() + scanType);
        for(int i = 0; i < stepTimeStamp.size()-1;i++){
            long upperTimeBound = stepTimeStamp.get((i+1));
            long lowerTimeBound = stepTimeStamp.get(i);
            List<Integer> scanIndexWithinStep = new ArrayList<>();
            //Searches through the list of timestamps and adds the scan numbers that fall within the step to a list
            for(int j = 0; j < scanTimeStampList.size();j++){
                if(scanTimeStampList.get(j) <= upperTimeBound && scanTimeStampList.get(j)> lowerTimeBound){
                    scanIndexWithinStep.add(j);
                }
            }
            if(scanIndexWithinStep.size()>0) {
                //Can loop through them as they will be sequential
                for (int k = scanIndexWithinStep.get(0); k <= scanIndexWithinStep.get(scanIndexWithinStep.size() - 1); k++) {
                    //Run the algorithm for each scan that falls within the time range for each step
                    double tempLat;
                    double tempLong;
                    //Formula from Dr. Zheng's paper
                    // i here represents the number of steps as the index number in adaptiveTimeList is the step number
                    if (xIncreasing) {
                        tempLat = startLat + STEP_LENGTH * (i + (double) (scanTimeStampList.get(k) - lowerTimeBound) / (double) (upperTimeBound - lowerTimeBound)) * yCompMotion * degToMRatio;
                    } else {
                        tempLat = startLat - STEP_LENGTH * (i + (double) (scanTimeStampList.get(k) - lowerTimeBound) / (double) (upperTimeBound - lowerTimeBound)) * yCompMotion * degToMRatio;
                    }
                    if (yIncreasing) {
                        tempLong = startLong + STEP_LENGTH * (i + (double) (scanTimeStampList.get(k) - lowerTimeBound) / (double) (upperTimeBound - lowerTimeBound)) * xCompMotion * degToMRatio;
                    } else {
                        tempLong = startLong - STEP_LENGTH * (i + (double) (scanTimeStampList.get(k) - lowerTimeBound) / (double) (upperTimeBound - lowerTimeBound)) * xCompMotion * degToMRatio;
                    }
                    scanCoordinates.add(new LatLng(tempLat, tempLong));
                }
            }

        }
        System.out.println("LENGTHHHH AFTER" + scanCoordinates.size() + scanType);
        if(scanType == "mag"){
            magCoords = scanCoordinates;
        }
        else if(scanType == "gyro"){
            gyroCoords = scanCoordinates;
        }
        else if(scanType == "accel"){
            accelCoords = scanCoordinates;
        }
        else if(scanType == "blue"){
            blueGlobalCoords = scanCoordinates;
        }
        else if(scanType == "red"){
            redGlobalCoords = scanCoordinates;
        }
    }
    void updateStepCoordiantes(){
        //Tags each step with its coordinates. Post processing can be used to give every scan it's own coordiantes.
        if(xIncreasing) {
            lat = lat + STEP_LENGTH * yCompMotion * degToMRatio;
            displayLat = displayLat + STEP_LENGTH*yCompMotion*degToMRatio;
        }
        else{
            lat = lat - STEP_LENGTH * yCompMotion * degToMRatio;
            displayLat = displayLat - STEP_LENGTH*yCompMotion*degToMRatio;
        }
        if(yIncreasing) {
            lon = lon + STEP_LENGTH * xCompMotion * degToMRatio;
            dispalyLon= dispalyLon + STEP_LENGTH*xCompMotion*degToMRatio;
        }
        else{
            lon = lon - STEP_LENGTH * xCompMotion * degToMRatio;
            dispalyLon= dispalyLon - STEP_LENGTH*xCompMotion*degToMRatio;
        }
    }
    public void getStepProfile() {
        try {
            Toast.makeText(getBaseContext(), "Reading Data", Toast.LENGTH_LONG).show();
            //save data
            File sdCard = Environment.getExternalStorageDirectory();
            File directory = new File(sdCard.getAbsolutePath() + "/DataCollectProfiles");
            directory.mkdirs();
            File file = new File(directory, USER_NAME + ".txt");
            FileReader fr = new FileReader(file);
            boolean readStepTime = false;
            boolean readSensitivity = false;
            int commaCount = 0;
            String stepTime = "";
            String fileInfo = "";
            String sensitivity = "";
            int intNextChar;
            char nextChar;
            //Numbers correspond the ASCII table values
            intNextChar = fr.read();
            while (intNextChar < 58 && intNextChar > 43) {
                if(intNextChar == ',' && !readStepTime){
                    commaCount++;
                    intNextChar = fr.read();
                    readStepTime = true;
                }
                if(intNextChar == ',' && !readSensitivity){
                    commaCount++;
                    intNextChar = fr.read();
                    readSensitivity = true;
                    readStepTime = false;
                }
                if(readStepTime && !readSensitivity){
                    nextChar = (char) intNextChar;
                    stepTime = stepTime + nextChar;
                    intNextChar = fr.read();
                }
                else if (readSensitivity) {
                    System.out.println("here");
                    nextChar = (char) intNextChar;
                    sensitivity = sensitivity + nextChar;
                    intNextChar = fr.read();
                }
                else {
                    nextChar = (char) intNextChar;
                    fileInfo = fileInfo + nextChar;
                    intNextChar = fr.read();
                }
            }
            //Global constants
            STEP_TIME_ORIGINAL = Double.parseDouble(stepTime);
            STEP_LENGTH = Double.parseDouble(fileInfo);

        } catch (Exception e) {
        }

    }

}
