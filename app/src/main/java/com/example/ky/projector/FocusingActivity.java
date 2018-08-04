package com.example.ky.projector;

import android.graphics.Bitmap;
import android.icu.text.DecimalFormat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

public class FocusingActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static  String TAG = "FocusingActivity";
    JavaCameraView javaCameraView;
    Mat mRgba, imgGray, imgGraySmall, imgGrayFinal;//, imgCanny;
    TextView focusValue;
    int frameCounter;
    ImageView smallImg;

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
        setContentView(R.layout.activity_focusing);
        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        focusValue = (TextView) findViewById(R.id.focusValue);
        frameCounter = 0;
        smallImg = (ImageView) findViewById(R.id.imageView);
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
        //Log.i(""+imgGray.width(), ""+imgGray.height());
        //Core.flip(imgGray, imgGray2, 1);
        Core.rotate(imgGray, imgGrayFinal, Core.ROTATE_90_CLOCKWISE);
        imgGraySmall = imgGrayFinal.submat(200, 880, 200, 880);
        //Imgproc.Canny(imgGray2, imgCanny, 127, 255);
        if(frameCounter==30) {
            frameCounter = 0;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DecimalFormat df = new DecimalFormat("0.00");
                    focusValue.setText(df.format(varianceOfLaplacian(imgGrayFinal)));

                    smallImg.setImageBitmap(new Bitmap(imgGraySmall));
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
}
