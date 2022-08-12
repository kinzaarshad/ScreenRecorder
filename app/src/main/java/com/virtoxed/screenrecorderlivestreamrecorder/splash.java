package com.virtoxed.screenrecorderlivestreamrecorder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.virtoxed.screenrecorderlivestreamrecorder.helper.StreamProfile;
import com.virtoxed.screenrecorderlivestreamrecorder.services.ControllerService;
import com.virtoxed.screenrecorderlivestreamrecorder.services.streaming.StreamingService;
import com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils;

import java.util.Timer;
import java.util.TimerTask;

import static com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils.isMyServiceRunning;

public class splash extends AppCompatActivity {

    //private static String sp="first";

    private ProgressBar mProgress;
    int counter=0;
    //private static boolean sp=true;


    public static boolean active = false;
    private static final int PERMISSION_REQUEST_CODE = 3004;
    private static final int PERMISSION_DRAW_OVER_WINDOW = 3005;
    private static final int PERMISSION_RECORD_DISPLAY = 3006;
    private static String[] mPermission = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public int mMode = MyUtils.MODE_RECORDING;
    private Intent mScreenCaptureIntent = null;
    private int mScreenCaptureResultCode = MyUtils.RESULT_CODE_FAILED;
    private StreamProfile mStreamProfile;
    ControllerService cs;
    WindowManager.LayoutParams paramCountdown;
    private View mCountdownLayout;
    WindowManager.LayoutParams paramVideoPopup;

    private ImageView delvideopopup,cancelvideopopup,sharevideopopup,popupvideo;
    private View mVideoPopupWindowView;
    private static final int REQUEST_SCREENSHOT=59706;
    private MediaProjectionManager mgr;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_splash);
        Window window = this.getWindow();
        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.tabcolor));
        }
        if(splashVariable.sp.contains("first")) {
            splashVariable.sp="second";
            initViews();
            cs=new ControllerService();
//            if(isMyServiceRunning(getApplicationContext(), StreamingService.class)) {
//                return;
//            }
//            if(isMyServiceRunning(getApplicationContext(), ControllerService.class)){
//                return;
//            }
//            mMode = MyUtils.MODE_RECORDING;
            shouldStartControllerService();
            mgr=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mgr.createScreenCaptureIntent(),
                    REQUEST_SCREENSHOT);

        } else if(splashVariable.sp.contains("second")) {
            splashVariable.sp="third";
            setContentView(R.layout.activity_splash);

            doWork();


        }

        else {
            setContentView(R.layout.activity_splash);
            //doWork();
            Intent intent = new Intent(splash.this, MainActivity.class);
            intent.setAction(MyUtils.ACTION_OPEN_VIDEO_MANAGER_ACTIVITY);
            startActivity(intent);
            finish();
        }
    }
    private void doWork() {
        mProgress =findViewById(R.id.progressbar);
        mProgress.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
        final Timer t=new Timer();
        TimerTask tt=new TimerTask() {
            @Override
            public void run() {
                counter++;
                mProgress.setProgress(counter);
                if(counter==100)
                {
                    t.cancel();
                    Intent intent = new Intent(splash.this, MainActivity.class);
                    intent.setAction(MyUtils.ACTION_OPEN_VIDEO_MANAGER_ACTIVITY);
                    startActivity(intent);
                    finish();
                }
            }
        };
        t.schedule(tt,0,25);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MyUtils.KEY_CONTROLlER_MODE, mMode);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState!=null){
            mMode = savedInstanceState.getInt(MyUtils.KEY_CONTROLlER_MODE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
    }



    private void requestScreenCaptureIntent() {
        if(mScreenCaptureIntent == null){
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), PERMISSION_RECORD_DISPLAY);
        }
    }



    private void initViews() {


        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        paramCountdown = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        paramVideoPopup = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        mVideoPopupWindowView = LayoutInflater.from(this).inflate(R.layout.video_popup_window, null);
        popupvideo = mVideoPopupWindowView.findViewById(R.id.popup_video);
        cancelvideopopup = mVideoPopupWindowView.findViewById(R.id.close_video_popup_window);
        delvideopopup = mVideoPopupWindowView.findViewById(R.id.delete_popup_video);
        sharevideopopup = mVideoPopupWindowView.findViewById(R.id.share_popup_video);

        View mViewCountdown = LayoutInflater.from(this).inflate(R.layout.layout_countdown, null);

        mCountdownLayout = mViewCountdown.findViewById(R.id.countdown_container);

        mCountdownLayout.setVisibility(View.GONE);

    }
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // PERMISSION DRAW OVER
            if(!Settings.canDrawOverlays(this)){
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_DRAW_OVER_WINDOW);
            }
            ActivityCompat.requestPermissions(this, mPermission, PERMISSION_REQUEST_CODE);

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    int granted = PackageManager.PERMISSION_GRANTED;
                    for(int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != granted) {
                            //MyUtils.showSnackBarNotification(mImgRec,"Please grant all permissions to record screen.", Snackbar.LENGTH_LONG);
                            return;
                        }
                    }
                    shouldStartControllerService();
                }
                break;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void shouldStartControllerService() {
        if(!hasCaptureIntent())
            requestScreenCaptureIntent();

        if(hasPermission()) {
            startControllerService();
        }
        else{
            requestPermissions();
            if(!hasCaptureIntent())
                requestScreenCaptureIntent();

        }
    }

    private boolean hasCaptureIntent() {
        return mScreenCaptureIntent != null;// || mScreenCaptureResultCode == MyUtils.RESULT_CODE_FAILED;
    }
    public static ImageView mImgRec,mImgStop;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCREENSHOT) {
            if (resultCode == RESULT_OK) {
                Intent i =
                        new Intent(this, ControllerService.class)
                                .putExtra(ControllerService.EXTRA_RESULT_CODE, resultCode)
                                .putExtra(ControllerService.EXTRA_RESULT_INTENT, data);

                startService(i);
            }
        }

        if (requestCode == PERMISSION_DRAW_OVER_WINDOW) {

            //Check if the permission is granted or not.
            if (resultCode != RESULT_OK) { //Permission is not available
              //  MyUtils.showSnackBarNotification(mImgRec, "Draw over other app permission not available.", Snackbar.LENGTH_SHORT);
            }
        }
        else if( requestCode == PERMISSION_RECORD_DISPLAY) {
            if(resultCode != RESULT_OK){
             //   MyUtils.showSnackBarNotification(mImgRec, "Recording display permission not available.",Snackbar.LENGTH_SHORT);
                mScreenCaptureIntent = null;
            }
            else{
                mScreenCaptureIntent = data;
                mScreenCaptureIntent.putExtra(MyUtils.SCREEN_CAPTURE_INTENT_RESULT_CODE, resultCode);
                mScreenCaptureResultCode = resultCode;

                shouldStartControllerService();
            }
        }
        else{
            super.onActivityResult(requestCode, resultCode, data);
        }


    }

    private void startControllerService() {
        Intent controller = new Intent(splash.this, ControllerService.class);

        controller.setAction(MyUtils.ACTION_INIT_CONTROLLER);

        controller.putExtra(MyUtils.KEY_CAMERA_AVAILABLE, checkCameraHardware(this));

        controller.putExtra(MyUtils.KEY_CONTROLlER_MODE, mMode);

        controller.putExtra(Intent.EXTRA_INTENT, mScreenCaptureIntent);

        if(mMode == MyUtils.MODE_STREAMING) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(MyUtils.STREAM_PROFILE, mStreamProfile);
            controller.putExtras(bundle);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(controller);
        }
        else{
            startService(controller);

        }
        finish();

    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean hasPermission(){
        int granted = PackageManager.PERMISSION_GRANTED;

        return ContextCompat.checkSelfPermission(this, mPermission[0]) == granted
                && ContextCompat.checkSelfPermission(this, mPermission[1]) == granted
                && ContextCompat.checkSelfPermission(this, mPermission[2]) == granted
                && Settings.canDrawOverlays(this)
                && mScreenCaptureIntent != null
                && mScreenCaptureResultCode != MyUtils.RESULT_CODE_FAILED;
    }

    public void setStreamProfile(StreamProfile streamProfile) {
        this.mStreamProfile = streamProfile;

    }

    public void notifyUpdateStreamProfile() {
        if(mMode == MyUtils.MODE_STREAMING){
            Intent controller = new Intent(splash.this, ControllerService.class);

            controller.setAction(MyUtils.ACTION_UPDATE_STREAM_PROFILE);
            Bundle bundle = new Bundle();
            bundle.putSerializable(MyUtils.STREAM_PROFILE, mStreamProfile);
            controller.putExtras(bundle);
            startService(controller);
        }
    }
}
