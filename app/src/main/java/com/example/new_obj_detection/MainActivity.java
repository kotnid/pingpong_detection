package com.example.new_obj_detection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.boundingRect;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    JavaCameraView javaCameraView;
    Mat mRGBA , mRGBAT;

    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(MainActivity.this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        if (!checkPermission()){
            requestPermission();
        }

        javaCameraView = (JavaCameraView) findViewById(R.id.opencv_surface_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(MainActivity.this );
        javaCameraView.setMaxFrameSize(1920,1080);
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                200);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(1080,1920 , CvType.CV_8UC4);
        Log.d("main" , "h:"+height);
        Log.d("main" , "w:"+width);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        mRGBAT = mRGBA.t();


        Core.flip(mRGBA.t() , mRGBAT , 1);
        Core.rotate(mRGBA.t() , mRGBAT , Core.ROTATE_90_CLOCKWISE);
        Imgproc.resize(mRGBAT , mRGBAT , mRGBA.size());

        //Mat mGRAY = inputFrame.gray();
        //Imgproc.blur(mGRAY, mGRAY, new Size(7, 7), new Point(2, 2));
        //Imgproc.cvtColor(mRGBA , mGRAY , Imgproc.COLOR_BGR2GRAY);
        //Imgproc.medianBlur(mGRAY,mGRAY , 5);
        //Core.flip(mGRAY , mGRAY , 1);

        Mat circles = new Mat();
        Imgproc.cvtColor(inputFrame.rgba() , mRGBA , Imgproc.COLOR_RGBA2RGB);
        //Imgproc.cvtColor(mRGBA , mRGBA , Imgproc.COLOR_RGB2HSV);

        Core.inRange(mRGBA , new Scalar(165, 48, 0) , new Scalar(255, 208, 50), mRGBAT);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mRGBAT , contours , new Mat() , Imgproc.RETR_LIST , Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i=0 ; i < contours.size() ; i++){
            //Log.d("main" , "data:"+contours);
            Rect box = boundingRect(contours.get(i));
            if ((box.height < 150) || (box.width < 150) || (Math.abs(box.height-box.width) > 100)){
                continue;
            }

            Log.d("main" , "S:"+box.tl());
            Imgproc.rectangle(mRGBA , box.tl() , box.br() , new Scalar(255,255,255));

            Point start = box.tl();
            int cent_h = (int) start.y + (box.height / 2);
            int cent_w = (int) start.x + (box.width / 2);
            Point center =  new Point(cent_w,cent_h);
            Imgproc.circle(mRGBA, center, 10, new Scalar(255, 0, 0, 255), 3);
            //Imgproc.drawContours(mRGBA , contours , i , new Scalar(255,255,255),-1);
        }

        //Imgproc.cvtColor(mRGBAT , mRGBAT , Imgproc.COLOR_BGR2GRAY);
        //Mat temp = new Mat(1080,1920 , CvType.CV_8UC4);
        //Core.bitwise_and(mRGBAT , mRGBA , temp);
        //Imgproc.cvtColor(mRGBA , mRGBA , Imgproc.COLOR_HSV2BGR);
        //Imgproc.HoughCircles(mRGBAT, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 100, 50, 30, 50, 1000);

        Log.d("main", String.valueOf("size: " + circles.cols()) + ", " + String.valueOf(circles.rows()));
        /**
        if (circles.cols() > 0) {
            for (int x=0; x < Math.min(circles.cols(), 5); x++ ) {
                double circleVec[] = circles.get(0, x);

                if (circleVec == null) {
                    break;
                }

                Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                int radius = (int) circleVec[2];


                double[] data = mRGBA.get((int)circleVec[1] , (int)circleVec[0]);
                double R = data[0];
                double G = data[1];
                double B = data[2];

                if (!((Math.abs(R-255) < 40 )&& (Math.abs(G-138) < 40) && (B < 40))){
                    Log.d("main" , "wrong");
                    break;
                }
                Log.d("main" , "R:"+R+" G:"+G+" B:"+B);

                Imgproc.circle(mRGBA, center, 3, new Scalar(255, 255, 255), 5);
                Imgproc.circle(mRGBA, center, radius, new Scalar(255, 255, 255), 2);
            }
        }
        **/
        circles.release();
        //mGRAY.release();

        return mRGBA;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        if (OpenCVLoader.initDebug()){
            Log.d("main" , "Opencv loaded :D");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }else{
            Log.d("main" , "Opencv not loaded :(");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION , this , baseLoaderCallback);
        }
    }
}