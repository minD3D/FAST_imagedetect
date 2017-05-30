package com.example.gomdo.imagedetection;

import android.Manifest;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.jik.imagedetection.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String TAG = "OCVSample::Activity";

    private TextView si_tv;
    private CameraBridgeViewBase mOpenCvCameraView;

    private BackgroundSubtractorMOG2 sub;
    private Mat mGray;
    private Mat mRgb;
    private Mat mFGMask;
    private List<MatOfPoint> contours;
    private double lRate = 0.5;
    private SeekBar sb;
    private boolean move = false;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                0);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);


        si_tv = (TextView) findViewById(R.id.sample_text);
        si_tv.setBackgroundColor(Color.rgb(0,0,255));
        si_tv.setText("알림 구현 부분");
        si_tv.setTextColor(Color.rgb(0,255,0));

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.show_camera_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(640, 480);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
/*
         if(move = true)
              si_tv.setBackgroundColor(Color.rgb(255,0,0));
        else if(move = false)
             si_tv.setBackgroundColor(Color.rgb(255,255,255));
*/

    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native String stringFromJNI();

    @Override
    public void onCameraViewStarted(int width, int height) {
       // sub = new BackgroundSubtractorMOG2();

        //creates matrices to hold the different frames
        mRgb = new Mat();
        mFGMask = new Mat();
        mGray = new Mat();
         sub = Video.createBackgroundSubtractorMOG2();

        //arraylist to hold individual contours
        contours = new ArrayList<MatOfPoint>();
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

    contours.clear();
    //gray frame because it requires less resource to process
    mRgb = inputFrame.gray();

    //this function converts the gray frame into the correct RGB format for the BackgroundSubtractorMOG apply function
    Imgproc.cvtColor(mRgb, mGray, Imgproc.COLOR_GRAY2RGB);
    Imgproc.threshold(mRgb,mGray,10,255,Imgproc.THRESH_TOZERO);


    Imgproc.blur(mGray,mGray,new Size(10.0,10.0));




    //apply detects objects moving and produces a foreground mask
    //the lRate updates dynamically dependent upon seekbar changes

    sub.apply(mGray, mFGMask, lRate);

    //  int i = mRgb.cols()+100;
    // int j = mRgb.rows()+100;

    //  mFGMask.col(i);
    //  mFGMask.row(j);
    MatOfKeyPoint points = new MatOfKeyPoint();

    FeatureDetector fast = FeatureDetector.create(FeatureDetector.FAST);
    fast.detect(mFGMask, points);



        int r = mFGMask.rows();
    int c = mFGMask.cols();
    int sum = 0;


    Log.d(TAG, "row : " + r);
    Log.d(TAG, "col : " + c);

    for (int i=0; i<r;){
        for (int j=0; j<c;){
            double[] buff = mFGMask.get(i, j);
            if (buff[0] >= 1)
                sum++;
            j+=5;
        }
        i+=5;
    }

    Log.d(TAG, "sum : " + sum);

        if(sum >1000)
            move = true;
        else
            move = false;
        Log.d(TAG, "move : " + move);

        /*
        if(move = true)
            si_tv.setBackgroundColor(Color.rgb(255,0,0));
        else
            si_tv.setBackgroundColor(Color.rgb(255,255,255));
*/
        Scalar redcolor = new Scalar(255, 0, 0);

    //erode and dilate are used  to remove noise from the foreground mask
    // Imgproc.erode(mFGMask, mFGMask, new Mat());
    // Imgproc.dilate(mFGMask, mFGMask, new Mat());

    //drawing contours around the objects by first called findContours and then calling drawContours
    //RETR_EXTERNAL retrieves only external contours
    //CHAIN_APPROX_NONE detects all pixels for each contour
    //Imgproc.findContours(mFGMask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

    //draws all the contours in red with thickness of 2
    //Imgproc.drawContours(mFGMask, contours, -1, new Scalar(255, 0, 0), 2);
    Features2d.drawKeypoints(mFGMask, points, mFGMask, redcolor, 3);

    return mFGMask;
}
}
