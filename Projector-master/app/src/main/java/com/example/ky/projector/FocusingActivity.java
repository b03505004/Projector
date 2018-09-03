package com.example.ky.projector;

import android.content.Intent;
import android.graphics.Bitmap;
import android.icu.text.DecimalFormat;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class FocusingActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static  String TAG = "FocusingActivity";
    private static Socket socket;
    private static BufferedReader bufferedReader;
    private static PrintWriter printWriter;
    private static String ip = "192.168.4.1";
    private static int stepCounter = 0;
    private static int bestStep = 0;
    private static String doneAck = "$D%";


    JavaCameraView javaCameraView;
    Mat mRgba, imgGray, imgGraySmall, imgGrayFinal;//, imgCanny;
    TextView focusValueTV;
    int frameCounter;
    ImageView smallImg;
    Bitmap bitmap;


    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };
    static{
        if (OpenCVLoader.initDebug()){
            Log.i(TAG, "OpenCV loaded successfully");
        }
        else{
            Log.i(TAG, "OpenCV not loaded");
        }
    }
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_focusing);
        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        focusValueTV = (TextView) findViewById(R.id.focusValue);
        frameCounter = 0;
        smallImg = (ImageView) findViewById(R.id.imageView);
        focusTask startFocusing = new focusTask();
        startFocusing.execute();
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(javaCameraView!=null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(javaCameraView!=null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (OpenCVLoader.initDebug()){
            Log.i(TAG, "OpenCV loaded successfully");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            Log.i(TAG, "OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallBack);
        }

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        imgGray = new Mat(height-400, width-400, CvType.CV_8UC1);
        imgGraySmall = new Mat(height-400, width-400, CvType.CV_8UC1);
        imgGrayFinal = new Mat(height, width, CvType.CV_8UC1);
        //imgCanny = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Imgproc.cvtColor(mRgba, imgGray, Imgproc.COLOR_RGB2GRAY);
        Core.rotate(imgGray, imgGrayFinal, Core.ROTATE_90_CLOCKWISE);
        imgGraySmall = imgGrayFinal.submat(200, 880, 300, 980);
        //Imgproc.Canny(imgGray2, imgCanny, 127, 255);
        if(frameCounter==30) {
            frameCounter = 0;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    double fv = varianceOfLaplacian(imgGrayFinal);
                    DecimalFormat df = new DecimalFormat("0.00");
                    focusValueTV.setText(df.format(fv));
                    //Log.i(TAG, "Focus val: "+fv);
                    bitmap = Bitmap.createBitmap(imgGraySmall.cols(), imgGraySmall.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(imgGraySmall, bitmap);

                    smallImg.setImageBitmap(bitmap);
                }
            });
        }
        frameCounter+=1;

        return imgGrayFinal;
    }

    public double varianceOfLaplacian(Mat img){
        Mat temp = new Mat(img.height(), img.width(), CvType.CV_8UC1);
        Imgproc.Laplacian(img, temp, CvType.CV_64F);
        MatOfDouble median = new MatOfDouble();
        MatOfDouble std= new MatOfDouble();
        Core.meanStdDev(temp, median , std);

        return Math.pow(std.get(0,0)[0],2);
    }

    class focusTask extends AsyncTask<Void,Void,Void> {
        private String instruction = "";
        private String GoUP = "U";
        private String GoDOWN = "D";
        private String ToTOP = "T";
        private String ToBOTTOM = "B";
        private String FINISHED = "F";
        private double focusValue = 0.0;
        private double bestFocusValue = 0.0;
        private boolean toContinue = true;
        private boolean toFineTune = true;
        private boolean toLimit = true;
        private int state = 0; // 0 = normal situation, 1 = best is bottom, 2 = best is top
        private int continuousBetter = 0;
        private int continuousWorse = 0;
        private int score = 0;

        protected boolean calculateFocusValue(){
            /*Log.i(TAG, "BEFORE: " + System.currentTimeMillis());
            for(long i =0 ; i<100000000; i++){
                long a = i;
            }
            Log.i(TAG, "AFTER : " + System.currentTimeMillis());*/

            imgGraySmall = imgGrayFinal.submat(200, 880, 200, 880);
            focusValue = varianceOfLaplacian(imgGraySmall);
            Log.i(TAG, "focus value: " + focusValue + ", step: "+stepCounter);
            if (focusValue >= bestFocusValue) {
                bestFocusValue = focusValue;
                bestStep = stepCounter;
                return true;
            }
            return false;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                socket = new Socket(ip, 66);
                socket.setSoTimeout(1200000);
                Log.i(TAG, "Socket connected: "+socket.isConnected());
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                printWriter = new PrintWriter(socket.getOutputStream());
                while(toContinue){
                    //i = bufferedReader.read();
                    instruction = bufferedReader.readLine();
                    Log.i(TAG, "Socket read: "+instruction);
                    if (instruction != null) {
                        // instruction: N = start, T = top, B = bottom, K = k
                        // send: U = up, D = down, F = finish
                        if (instruction.equals("N")) {

                            if(!calculateFocusValue()){
                                continuousWorse += 1;
                            }
                            stepCounter += 1;
                            //printWriter.write(doneAck);
                            //printWriter.flush();
                            if(continuousWorse>3){
                                toContinue = false;
                                calculateFocusValue();

                                if (bestStep == stepCounter){
                                    state = 2;
                                } else if (bestStep == 0){
                                    state = 1;
                                }
                                String formattedBestStep = String.format("%02d", stepCounter - bestStep);
                                printWriter.write("$"+formattedBestStep+"%");
                                printWriter.flush();
                                Log.i(TAG, "Continuous worse!");
                                Log.i(TAG, "Best step: "+"$"+formattedBestStep+"%");
                            } else{
                                printWriter.write("$"+GoUP+"%");
                                printWriter.flush();
                                Log.i(TAG, "Sent: " + GoUP);
                            }
                        } else if (instruction.equals("T")) {
                            toContinue = false;
                            calculateFocusValue();

                            if (bestStep == stepCounter){
                                state = 2;
                            } else if (bestStep == 0){
                                state = 1;
                            }
                            String formattedBestStep = String.format("%02d", stepCounter - bestStep);
                            printWriter.write("$"+formattedBestStep+"%");
                            printWriter.flush();
                            Log.i(TAG, "Best step: "+"$"+formattedBestStep+"%");
                            //Intent i = new Intent(FocusingActivity.this, DoneActivity.class);
                            //startActivity(i);
                        }
                    }
                }
                // instruction: K = k
                // send: U = up, D = down, F = finish, T = to TOP, B = to BOTTOM
                // 0 = normal situation, 1 = best is bottom, 2 = best is top
                ///*

                instruction = bufferedReader.readLine();
                Log.i(TAG, "Socket read: "+instruction);
                if (instruction != null) {
                    if(instruction.equals("K")) {
                        boolean justStarted = true;
                        Log.i(TAG, "FINETUNE");
                        while (toFineTune) {
                            if (state == 1) {
                                printWriter.write("$" + GoUP + "%");
                                printWriter.flush();
                                Log.i(TAG, "Sent: " + GoUP);
                                instruction = bufferedReader.readLine();
                                Log.i(TAG, "Socket read: " + instruction);
                                if (instruction != null) {
                                    if (instruction.equals("K")) {
                                        if (calculateFocusValue()) { // moving up is better
                                            if (justStarted) {
                                                justStarted = false;
                                            }
                                        } else { // moving down is better
                                            if (justStarted) {
                                                toFineTune = false;
                                                printWriter.write("$" + ToBOTTOM + "%");
                                                printWriter.flush();
                                                Log.i(TAG, "Sent: " + ToBOTTOM);
                                            } else {
                                                toFineTune = false;
                                                printWriter.write("$" + GoDOWN + "%");
                                                printWriter.flush();
                                                Log.i(TAG, "Sent: " + GoDOWN);
                                                instruction = bufferedReader.readLine();
                                                Log.i(TAG, "Socket read: " + instruction);
                                            }
                                        }
                                    }
                                }
                            } else if (state == 2) {
                                printWriter.write("$" + GoDOWN + "%");
                                printWriter.flush();
                                Log.i(TAG, "Sent: " + GoDOWN);
                                instruction = bufferedReader.readLine();
                                Log.i(TAG, "Socket read: " + instruction);
                                if (instruction != null) {
                                    if (instruction.equals("K")) {
                                        if (calculateFocusValue()) { // moving down is better
                                            if (justStarted) {
                                                justStarted = false;
                                            }
                                        } else { // moving up is better
                                            if (justStarted) {
                                                toFineTune = false;
                                                printWriter.write("$" + ToTOP + "%");
                                                printWriter.flush();
                                                Log.i(TAG, "Sent: " + ToTOP);
                                            } else {
                                                toFineTune = false;
                                                printWriter.write("$" + GoUP + "%");
                                                printWriter.flush();
                                                Log.i(TAG, "Sent: " + GoUP);
                                                instruction = bufferedReader.readLine();
                                                Log.i(TAG, "Socket read: " + instruction);
                                            }
                                        }
                                    }
                                }
                            } else if (state == 0) {
                                justStarted = false;
                                printWriter.write("$" + GoUP + "%");
                                printWriter.flush();
                                Log.i(TAG, "Sent: " + GoUP);
                                instruction = bufferedReader.readLine();
                                Log.i(TAG, "Socket read: " + instruction);
                                if (instruction != null) {
                                    if (instruction.equals("K")) {
                                        if (calculateFocusValue()) { // moving up is better

                                        } else { // moving down is better
                                            state = 1;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "ERROR");

                }
                //*/
                printWriter.write("$"+FINISHED+"%");
                printWriter.flush();
                Log.i(TAG, "Finish code: "+"$"+FINISHED+"%");
                socket.close();
                printWriter.close();
                Intent i = new Intent(FocusingActivity.this, DoneActivity.class);
                startActivity(i);
            }catch (IOException ioe){
                //Log.i("SOCKET", ""+socket.isConnected());
                Log.e(TAG, ""+ioe);
            }
            return null;
        }
    }
}
