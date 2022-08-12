package com.virtoxed.screenrecorderlivestreamrecorder.services;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.ToneGenerator;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;


import com.bumptech.glide.Glide;
import com.virtoxed.screenrecorderlivestreamrecorder.BuildConfig;
import com.virtoxed.screenrecorderlivestreamrecorder.controllers.settings.CameraSetting;
import com.virtoxed.screenrecorderlivestreamrecorder.controllers.settings.SettingManager;
import com.virtoxed.screenrecorderlivestreamrecorder.MainActivity;

import com.virtoxed.screenrecorderlivestreamrecorder.services.recording.RecordingService;
import com.virtoxed.screenrecorderlivestreamrecorder.services.recording.RecordingService.RecordingBinder;

import com.virtoxed.screenrecorderlivestreamrecorder.services.screenshot.ImageTransmogrifier;
import com.virtoxed.screenrecorderlivestreamrecorder.services.streaming.StreamingService;
import com.virtoxed.screenrecorderlivestreamrecorder.services.streaming.StreamingService.StreamingBinder;

import com.virtoxed.screenrecorderlivestreamrecorder.R;
import com.virtoxed.screenrecorderlivestreamrecorder.splashVariable;
import com.virtoxed.screenrecorderlivestreamrecorder.utils.CameraPreview;
import com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils;
import com.virtoxed.screenrecorderlivestreamrecorder.helper.StreamProfile;
import com.virtoxed.screenrecorderlivestreamrecorder.utils.NotificationHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.serenegiant.utils.UIThreadHelper.runOnUiThread;
import static com.virtoxed.screenrecorderlivestreamrecorder.utils.NotificationHelper.CHANNEL_ID;


public class ControllerService extends Service {
    private static final String TAG = ControllerService.class.getSimpleName();

    private final boolean DEBUG = MyUtils.DEBUG;
    public static BaseService mService;
    private Boolean mRecordingServiceBound = false;
    private View mViewRoot;
    private View mCameraLayout;
    private View mImgPopupWindowView,mVideoPopupWindowView;
    private WindowManager mWindowManager;

    WindowManager.LayoutParams paramViewRoot;

    WindowManager.LayoutParams paramCam;

    WindowManager.LayoutParams paramCountdown;

    WindowManager.LayoutParams paramImgPopup;

    WindowManager.LayoutParams paramVideoPopup;

    private Intent mScreenCaptureIntent = null;

    public static ImageView mImgClose, mImgRec, mImgStart, mImgStop, mImgPause, mImgCapture, mImgLive, mImgSetting;
    private Boolean mRecordingStarted = false;
    private Boolean mRecordingPaused = false;
    private Camera mCamera;
    private LinearLayout cameraPreview;
    private CameraPreview mPreview;
    private int mScreenWidth, mScreenHeight;
    private TextView mTvCountdown;
    private View mCountdownLayout;
    private int mCameraWidth = 160, mCameraHeight = 120;
    private StreamProfile mStreamProfile;
    private int mMode;


    private static final int PERMISSIONS_REQUEST_STORAGE = 110;
    private static final int REQUEST_MEDIA_PROJECTION = 111;

    private static final String STATE_RESULT_CODE = "RESULT_CODE";
    private static final String STATE_RESULT_DATA = "RESULT_DATA";

    private int mResultCode;
    private MediaProjectionManager mMediaProjectionManager;
    private Intent mResultData;
    private Intent shotIntent;

    //for screenshot
    private static final int NOTIFY_ID=9906;
    public static final String EXTRA_RESULT_CODE="resultCode";
    public static final String EXTRA_RESULT_INTENT="resultIntent";
    static final String ACTION_RECORD=
            BuildConfig.APPLICATION_ID+".RECORD";
    static final String ACTION_SHUTDOWN=
            BuildConfig.APPLICATION_ID+".SHUTDOWN";
    static final int VIRT_DISPLAY_FLAGS=
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY |
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private MediaProjection projection;
    private VirtualDisplay vdisplay;
    final private HandlerThread handlerThread=
            new HandlerThread(getClass().getSimpleName(),
                    android.os.Process.THREAD_PRIORITY_BACKGROUND);
    private Handler handler;
    private MediaProjectionManager mgr;
    private WindowManager wmgr;
    private ImageTransmogrifier it;
    private int resultCode;
    private Intent resultData;
    final private ToneGenerator beeper=
            new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

    //for popup windows
    private ImageView delimgpopup,cancelimgpopup,shareimgpopup,popupimg;
    private ImageView delvideopopup,cancelvideopopup,sharevideopopup,popupvideo;

    //for removing chathead
    private ImageView remove_image_view;
    private Point szWindow = new Point();
    private View removeFloatingWidgetView;
    private int x_init_cord, y_init_cord, x_init_margin, y_init_margin;
    private boolean isLeft = true;
    private int initialX;         //x_init_margin
    private int initialY;         //y_init_margin
    private float initialTouchX;       //x_init_cord
    private float initialTouchY;       //y_init_cord
    int Xdiff;
    int Ydiff;

    //for notification
    public static NotificationManager manager;
    public static RemoteViews contentView;
    NotificationCompat.Builder notificationBuilder;
    public static Notification notification;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null)
            return START_NOT_STICKY;

        String action = intent.getAction();
        if(action!=null) {
            handleIncomeAction(intent);
            if(DEBUG) Log.i(TAG, "return START_REDELIVER_INTENT" + action);

            return START_NOT_STICKY;
        }
        if (intent.getAction()==null) {
            resultCode=intent.getIntExtra(EXTRA_RESULT_CODE, 1337);
            resultData=intent.getParcelableExtra(EXTRA_RESULT_INTENT);
        }
        else if (ACTION_RECORD.equals(intent.getAction())) {
            if (resultData!=null) {
                startCapture();
            }
            else {
                Intent ui=
                        new Intent(this, MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(ui);
            }
        }
        else if (ACTION_SHUTDOWN.equals(intent.getAction())) {
            beeper.startTone(ToneGenerator.TONE_PROP_NACK);
            stopForeground(true);
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIncomeAction(Intent intent) {
        String action = intent.getAction();
        if(TextUtils.isEmpty(action))
            return;

        switch (action){
            case MyUtils.ACTION_INIT_CONTROLLER:
                mMode = intent.getIntExtra(MyUtils.KEY_CONTROLlER_MODE, MyUtils.MODE_RECORDING);
                mScreenCaptureIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if(mMode == MyUtils.MODE_STREAMING)
                    mStreamProfile = (StreamProfile) intent.getSerializableExtra(MyUtils.STREAM_PROFILE);
                boolean isCamera = intent.getBooleanExtra(MyUtils.KEY_CAMERA_AVAILABLE, false);

                if(isCamera && mCamera ==null) {
                    if(DEBUG) Log.i(TAG, "onStartCommand: before initCameraView");
                    initCameraView();
                }
                if(mScreenCaptureIntent == null){
                    Log.i(TAG, "mScreenCaptureIntent is NULL");
                    stopService();
                }
                else if(!mRecordingServiceBound){
                    if(DEBUG) Log.i(TAG, "before run bindStreamService()"+action);
                    bindStreamingService();
                }
                break;

            case MyUtils.ACTION_UPDATE_SETTING:
                handleUpdateSetting(intent);
                break;

            case MyUtils.ACTION_NOTIFY_FROM_STREAM_SERVICE:

                break;
            case MyUtils.ACTION_UPDATE_STREAM_PROFILE:
                if(mMode == MyUtils.MODE_STREAMING && mService!=null && mRecordingServiceBound) {
                    mStreamProfile = (StreamProfile) intent.getSerializableExtra(MyUtils.STREAM_PROFILE);
                    ((StreamingService)mService).updateStreamProfile(mStreamProfile);
                }
                else{
                    Log.e(TAG, "handleIncomeAction: ", new Exception("Update stream profile error") );
                }
                break;

        }
    }

    private void handleUpdateSetting(Intent intent) {
        int key = intent.getIntExtra(MyUtils.ACTION_UPDATE_SETTING, -1);
        switch (key){
            case R.string.setting_camera_size:
                updateCameraSize();
                break;
            case R.string.setting_camera_position:
                updateCameraPosition();
                break;
            case R.string.setting_camera_mode:
                updateCameraMode();
                break;
        }
    }

    private void updateCameraMode() {
        CameraSetting profile = SettingManager.getCameraProfile(getApplicationContext());
        if(profile.getMode().equals(CameraSetting.CAMERA_MODE_OFF))
            toggleView(mCameraLayout, View.GONE);
        else{
            if(mCameraLayout!=null){
                mWindowManager.removeViewImmediate(mCameraLayout);
                releaseCamera();
                initCameraView();
            }
        }
    }

    private void updateCameraPosition() {
        if(DEBUG)
            Log.i(TAG, "updateCameraPosition: ");
        CameraSetting profile = SettingManager.getCameraProfile(getApplicationContext());
        paramCam.gravity = profile.getParamGravity();
        paramCam.x = 0;
        paramCam.y = 0;
        mWindowManager.updateViewLayout(mCameraLayout, paramCam);
    }

    private void updateCameraSize() {
        CameraSetting profile = SettingManager.getCameraProfile(getApplicationContext());
        calculateCameraSize(profile);
        onConfigurationChanged(getResources().getConfiguration());
    }

    public ControllerService() {

    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new IllegalStateException("Binding not supported. Go away.");
//        return null;
    }

    BroadcastReceiver rec=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mRecordingServiceBound){

                toggleView(mCountdownLayout, View.VISIBLE);

                int countdown = (SettingManager.getCountdown(getApplication())+1) * 1000;

                new CountDownTimer(countdown, 1000) {

                    public void onTick(long millisUntilFinished) {
                        mTvCountdown.setText(""+(millisUntilFinished / 1000));
                    }

                    public void onFinish() {
                        toggleView(mCountdownLayout, View.GONE);
                        contentView.setViewVisibility(R.id.recordL,View.GONE);
                        contentView.setViewVisibility(R.id.stopL,View.VISIBLE);
                        manager.notify(2, notification);
                        mRecordingStarted = true;
                        mService.startPerformService();
                        if(splashVariable.sp.contains("third")) {
                            MainActivity.mImgRec.setVisibility(View.INVISIBLE);
                            MainActivity.mImgStop.setVisibility(View.VISIBLE);
                        }
                        mImgStart.setVisibility(View.INVISIBLE);
                        mImgStop.setVisibility(View.VISIBLE);



                    }
                }.start();

            }
            else{
                mRecordingStarted = false;
                MyUtils.toast(getApplicationContext(), "Recording Service connection has not been established", Toast.LENGTH_LONG);
                Log.e(TAG, "Recording Service connection has not been established");
                stopService();
            }


        }
    };
    BroadcastReceiver stp=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mRecordingServiceBound){
                //Todo: stop and save recording
                contentView.setViewVisibility(R.id.stopL,View.GONE);
                contentView.setViewVisibility(R.id.recordL,View.VISIBLE);

                manager.notify(2, notification);
                mRecordingStarted = false;

                mService.stopPerformService();
                if(splashVariable.sp.contains("third")) {
                    MainActivity.mImgStop.setVisibility(View.INVISIBLE);
                    MainActivity.mImgRec.setVisibility(View.VISIBLE);
                }
                mImgStop.setVisibility(View.INVISIBLE);
                mImgStart.setVisibility(View.VISIBLE);



                if(mMode==MyUtils.MODE_RECORDING){
                    ((RecordingService)mService).insertVideoToGallery();
                    ((RecordingService)mService).saveVideoToDatabase();
                    mWindowManager.addView(mVideoPopupWindowView, paramVideoPopup);
                    Glide.with(ControllerService.this).load(RecordingService.outputFile).into(popupvideo);
                    popupvideo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //   mVideoPopupWindowView.setVisibility(View.GONE);
                            Intent vi = new Intent(Intent.ACTION_VIEW);
                            vi.setDataAndType(Uri.parse(RecordingService.outputFile), "video/mp4");
                            vi.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            ControllerService.this.startActivity(vi);
                            mWindowManager.removeView(mVideoPopupWindowView);
                        }
                    });
                    cancelvideopopup.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //   mVideoPopupWindowView.setVisibility(View.GONE);
                            mWindowManager.removeView(mVideoPopupWindowView);
                        }
                    });
                    delvideopopup.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {



                            File out=new File(RecordingService.outputFile);
                            if(out.exists()){
                                out.delete();
                            }
                            // mVideoPopupWindowView.setVisibility(View.GONE);
                            mWindowManager.removeView(mVideoPopupWindowView);



                        }
                    });
                    sharevideopopup.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //    mVideoPopupWindowView.setVisibility(View.GONE);
                            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                            sharingIntent.setType("video/*");
                            sharingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            sharingIntent.putExtra(Intent.EXTRA_STREAM, RecordingService.outputFile);
                            Intent startIntent = Intent.createChooser(sharingIntent, "Share Video");
                            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            ControllerService.this.startActivity(startIntent);
                            mWindowManager.removeView(mVideoPopupWindowView);

                        }
                    });

                    return;
                }

            }
            else{
                mRecordingStarted = true;
                MyUtils.toast(getApplicationContext(), "Recording Service connection has not been established", Toast.LENGTH_LONG);
            }
        }
    };
    BroadcastReceiver screensht=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timer buttonTimer = new Timer();
            buttonTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startCapture();
                        }
                    });
                }
            }, 1000);

        }
    };

    BroadcastReceiver ext=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopForeground(true);
            splashVariable.sp="first";
            stopSelf();


        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        mgr=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        wmgr=(WindowManager)getSystemService(WINDOW_SERVICE);

        registerReceiver(rec,new IntentFilter("rec"));
        registerReceiver(stp,new IntentFilter("stp"));
        registerReceiver(screensht,new IntentFilter("ss"));
        registerReceiver(ext,new IntentFilter("ext"));



        if(DEBUG) Log.i(TAG, "StreamingControllerService: onCreate");

        updateScreenSize();
        if(paramViewRoot==null) {
            initParam();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                up();

            }
            else{
                low();
            }
        }



    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void up(){

        NotificationChannel chan = new NotificationChannel(CHANNEL_ID, NotificationHelper.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);
        contentView = new RemoteViews(getPackageName(), R.layout.notification_items);
        another();


        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.appicon)
                .setContent(contentView)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                //.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                //.setChannelId(NotificationManager.EXTRA_NOTIFICATION_CHANNEL_ID)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        startForeground(2, notification);

        if(mViewRoot == null)
            initializeViews();

    }
    public void low(){

        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        contentView = new RemoteViews(getPackageName(), R.layout.notification_items);


        another();
        notificationBuilder = new NotificationCompat.Builder(this);
        notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.appicon)
                .setContent(contentView)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                //.setChannelId(NotificationManager.EXTRA_NOTIFICATION_CHANNEL_ID)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        startForeground(2, notification);
        if(mViewRoot == null)
            initializeViews();

    }
    public void another(){
        Intent intent = new Intent(this, MyBroadcastReciever.class);
        Intent intent5 = new Intent(this, MyBroadcastReciever.class);
        Intent intent1 = new Intent(this, MyBroadcastReciever.class);
        Intent intent2 = new Intent(this, MyBroadcastReciever.class);
        Intent intent3 = new Intent(this, MyBroadcastReciever.class);
        Intent intent4 = new Intent(this, MyBroadcastReciever.class);

        intent.setAction(MyBroadcastReciever.rec_start);
        intent5.setAction(MyBroadcastReciever.rec_stop);
        intent1.setAction(MyBroadcastReciever.screenshot);
        intent2.setAction(MyBroadcastReciever.settingg);
        intent3.setAction(MyBroadcastReciever.home);
        intent4.setAction(MyBroadcastReciever.exit);
        //intent.putExtra("title", "something");
        //intent.putExtra("text", strtext);
        PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        PendingIntent pIntent5 = PendingIntent.getBroadcast(this, 0, intent5, 0);
        PendingIntent pIntent1 = PendingIntent.getBroadcast(this, 0, intent1, 0);
        PendingIntent pIntent2 = PendingIntent.getBroadcast(this, 0, intent2, 0);
        PendingIntent pIntent3 = PendingIntent.getBroadcast(this, 0, intent3, 0);
        PendingIntent pIntent4 = PendingIntent.getBroadcast(this, 0, intent4, 0);
        //PendingIntent pIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        contentView.setViewVisibility(R.id.stopL,View.GONE);
        contentView.setViewVisibility(R.id.recordL,View.VISIBLE);
        contentView.setImageViewResource(R.id.rec_not_id,R.drawable.record_noti);
        contentView.setImageViewResource(R.id.stop_not_id,R.drawable.stop_noti);
        contentView.setImageViewResource(R.id.ss_id,R.drawable.ss_noti);
        contentView.setImageViewResource(R.id.settings_not_id,R.drawable.settings_noti);
        contentView.setImageViewResource(R.id.home_not_id,R.drawable.home_noti);
        contentView.setImageViewResource(R.id.exit_not_id,R.drawable.exit_noti);

        contentView.setOnClickPendingIntent(R.id.rec_not_id, pIntent);
        contentView.setOnClickPendingIntent(R.id.stop_not_id, pIntent5);
        contentView.setOnClickPendingIntent(R.id.ss_id, pIntent1);
        contentView.setOnClickPendingIntent(R.id.settings_not_id, pIntent2);
        contentView.setOnClickPendingIntent(R.id.home_not_id, pIntent3);
        contentView.setOnClickPendingIntent(R.id.exit_not_id, pIntent4);
    }
    public static class MyBroadcastReciever extends BroadcastReceiver {
        public static final String rec_start = "OK";
        public static final String rec_stop = "OKA";
        public static final String screenshot = "OKB";
        public static final String settingg = "OKC";
        public static final String home = "OKD";
        public static final String exit = "OEK";

        private static final String TAG = "receiver";
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceive(Context context, Intent intent) {
            if(rec_start.equals(intent.getAction())) {

                Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(closeIntent);
                Intent i=new Intent("rec");
                context.sendBroadcast(i);


            }
            else if(rec_stop.equals(intent.getAction())) {

                Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(closeIntent);
                Intent i=new Intent("stp");
                context.sendBroadcast(i);


            }
            else if(screenshot.equals(intent.getAction())) {

                Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(closeIntent);
                Intent i=new Intent("ss");
                context.sendBroadcast(i);

            }
            else if(settingg.equals(intent.getAction())) {

                Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(closeIntent);
                if(!MainActivity.active) {
                    Intent i = new Intent(context, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.setAction(MyUtils.ACTION_OPEN_SETTING_ACTIVITY);
                    context.startActivity(i);
                }

            }
            else if(home.equals(intent.getAction())) {
                Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(closeIntent);
                if(!MainActivity.active) {
                    Intent i = new Intent(context, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.setAction(MyUtils.ACTION_OPEN_LIVE_ACTIVITY);
                    context.startActivity(i);
                }



            }
            else if(exit.equals(intent.getAction())) {

                Intent i=new Intent("ext");
                context.sendBroadcast(i);


            }
        }
    }
    private void getWindowManagerDefaultDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)

            mWindowManager.getDefaultDisplay().getSize(szWindow);
        else {
            int w = mWindowManager.getDefaultDisplay().getWidth();
            int h = mWindowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }
    }
    /*  Add Remove View to Window Manager  */
    private View addRemoveView(LayoutInflater inflater) {
        //Inflate the removing view layout we created
        removeFloatingWidgetView = inflater.inflate(R.layout.remove_floating_widget_layout, null);
        int flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            flags = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        //Add the view to the window.
        WindowManager.LayoutParams paramRemove = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                flags,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        paramRemove.gravity = Gravity.TOP | Gravity.LEFT;

        //Initially the Removing widget view is not visible, so set visibility to GONE
        removeFloatingWidgetView.setVisibility(View.GONE);
        remove_image_view = (ImageView) removeFloatingWidgetView.findViewById(R.id.remove_img);

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(removeFloatingWidgetView, paramRemove);
        return remove_image_view;
    }
    /*  Add Floating Widget View to Window Manager  */
    private void addFloatingWidgetView(LayoutInflater inflater) {
        //Inflate the floating view layout we created
        mViewRoot = inflater.inflate(R.layout.layout_recording_again, null);
        int flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            flags = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        //Add the view to the window.
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                flags,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.LEFT;

        //Initially view will be added to top-left corner, you change x-y coordinates according to your need
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager.addView(mViewRoot, params);


    }
    /*  Reset position of Floating Widget view on dragging  */
    private void resetPosition(int x_cord_now) {
        if (x_cord_now <= szWindow.x / 2) {
            isLeft = true;
            moveToLeft(x_cord_now);
        } else {
            isLeft = false;
            moveToRight(x_cord_now);
        }

    }


    /*  Method to move the Floating widget view to Left  */
    private void moveToLeft(final int current_x_cord) {
        final int x = szWindow.x - current_x_cord;

        new CountDownTimer(500, 5) {
            //get params of Floating Widget view
            WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) mViewRoot.getLayoutParams();

            public void onTick(long t) {
                long step = (500 - t) / 5;

                mParams.x = 0 - (int) (current_x_cord * current_x_cord * step);

                //If you want bounce effect uncomment below line and comment above line
                // mParams.x = 0 - (int) (double) bounceValue(step, x);


                //Update window manager for Floating Widget
                mWindowManager.updateViewLayout(mViewRoot, mParams);
            }

            public void onFinish() {
                mParams.x = 0;

                //Update window manager for Floating Widget
                mWindowManager.updateViewLayout(mViewRoot, mParams);
            }
        }.start();
    }

    /*  Method to move the Floating widget view to Right  */
    private void moveToRight(final int current_x_cord) {

        new CountDownTimer(500, 5) {
            //get params of Floating Widget view
            WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) mViewRoot.getLayoutParams();

            public void onTick(long t) {
                long step = (500 - t) / 5;

                mParams.x = (int) (szWindow.x + (current_x_cord * current_x_cord * step) - mViewRoot.getWidth());

                //If you want bounce effect uncomment below line and comment above line
                //  mParams.x = szWindow.x + (int) (double) bounceValue(step, x_cord_now) - mFloatingWidgetView.getWidth();

                //Update window manager for Floating Widget
                mWindowManager.updateViewLayout(mViewRoot, mParams);
            }

            public void onFinish() {
                mParams.x = szWindow.x - mViewRoot.getWidth();

                //Update window manager for Floating Widget
                mWindowManager.updateViewLayout(mViewRoot, mParams);
            }
        }.start();
    }

    /*  Get Bounce value if you want to make bounce effect to your Floating Widget */
    private double bounceValue(long step, long scale) {
        double value = scale * Math.exp(-0.055 * step) * Math.cos(0.08 * step);
        return value;
    }
    private void onFloatingWidgetLongClick() {
        //Get remove Floating view params
        WindowManager.LayoutParams removeParams = (WindowManager.LayoutParams) removeFloatingWidgetView.getLayoutParams();

        //get x and y coordinates of remove view
        int x_cord = (szWindow.x - removeFloatingWidgetView.getWidth()) / 2;
        int y_cord = szWindow.y - (removeFloatingWidgetView.getHeight() + getStatusBarHeight());


        removeParams.x = x_cord;
        removeParams.y = y_cord;

        //Update Remove view params
        mWindowManager.updateViewLayout(removeFloatingWidgetView, removeParams);
    }
    private int getStatusBarHeight() {
        return (int) Math.ceil(25 * getApplicationContext().getResources().getDisplayMetrics().density);
    }
    public WindowManager getWindowManager() {
        return(wmgr);
    }

    public Handler getHandler() {
        return(handler);
    }
    File file;
    public void processImage(final byte[] png) {
        new Thread() {
            @Override
            public void run() {
                String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                File myDir = new File(root +File.separator+"Screen Recorder Live Stream Recorder");
                if(!myDir.exists()) {
                    myDir.mkdirs();
                }
                Random generator = new Random();
                int n = 10000;
                n = generator.nextInt(n);
                String fname = "Screenshot-"+ n +".png";
                file = new File (myDir, fname);
                if (file.exists ()) file.delete ();
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(png);
                    out.flush();
                    out.close();
                    myDir.getAbsolutePath();



                    MediaScannerConnection.scanFile(ControllerService.this,
                            new String[] {myDir.getAbsolutePath()},
                            new String[] {"image/png"},
                            null);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        beeper.startTone(ToneGenerator.TONE_PROP_ACK);
        stopCapture();

        Bitmap bmp = BitmapFactory.decodeByteArray(png, 0, png.length);
        try {
            mImgPopupWindowView=LayoutInflater.from(this).inflate(R.layout.image_popup_window,null);
            mWindowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
            mWindowManager.addView(mImgPopupWindowView,paramImgPopup);

            popupimg=mImgPopupWindowView.findViewById(R.id.popup_img);
            delimgpopup=mImgPopupWindowView.findViewById(R.id.delete_popup_img);
            cancelimgpopup=mImgPopupWindowView.findViewById(R.id.close_img_popup_window);
            shareimgpopup=mImgPopupWindowView.findViewById(R.id.share_popup_img);

            popupimg.setImageBitmap(bmp);
            popupimg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent vi = new Intent(Intent.ACTION_VIEW);
                    vi.setDataAndType(getImageUri(ControllerService.this,bmp), "image/*");
                    vi.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ControllerService.this.startActivity(vi);
                    mImgPopupWindowView.setVisibility(View.GONE);
                }
            });
            cancelimgpopup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mImgPopupWindowView.setVisibility(View.GONE);
                }
            });

            delimgpopup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {




                    file.delete();
                    mImgPopupWindowView.setVisibility(View.GONE);





                }
            });
            shareimgpopup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mImgPopupWindowView.setVisibility(View.GONE);
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    sharingIntent.setType("image/*");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM, getImageUri(ControllerService.this,bmp));

                    Intent startIntent = Intent.createChooser(sharingIntent, "Share Image");
                    startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ControllerService.this.startActivity(startIntent);


                }
            });

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
    private void stopCapture() {
        if (projection!=null) {
            projection.stop();
            vdisplay.release();
            projection=null;

        }
    }


    public void startCapture() {
        projection=mgr.getMediaProjection(resultCode, resultData);
        it=new ImageTransmogrifier(this);

        MediaProjection.Callback cb=new MediaProjection.Callback() {
            @Override
            public void onStop() {
                vdisplay.release();
            }
        };

        vdisplay=projection.createVirtualDisplay("andshooter",
                it.getWidth(), it.getHeight(),
                getResources().getDisplayMetrics().densityDpi,
                VIRT_DISPLAY_FLAGS, it.getSurface(), null, handler);
        projection.registerCallback(cb, handler);

    }
    private void initParam() {
        if(DEBUG) Log.i(TAG, "initParam: ");
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        paramViewRoot = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        paramCam = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        paramCountdown = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        paramImgPopup = new WindowManager.LayoutParams(
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

    }

    private void updateScreenSize() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
    }

    private void initCameraView() {
        if(DEBUG) Log.i(TAG, "StreamingControllerService: initializeCamera()");
        CameraSetting cameraProfile = SettingManager.getCameraProfile(getApplication());

        mCameraLayout = LayoutInflater.from(this).inflate(R.layout.layout_camera_view, null);

        if(cameraProfile.getMode().equals(CameraSetting.CAMERA_MODE_BACK))
            mCamera =  Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        else
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);

        cameraPreview = mCameraLayout.findViewById(R.id.camera_preview);

        calculateCameraSize(cameraProfile);

        onConfigurationChanged(getResources().getConfiguration());

        paramCam.gravity = cameraProfile.getParamGravity();
        paramCam.x = 0;
        paramCam.y = 0;

        mPreview = new CameraPreview(this, mCamera);

        cameraPreview.addView(mPreview);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mCameraLayout, paramCam);
        mCamera.startPreview();

        //re-inflate controller
        mWindowManager.removeViewImmediate(mViewRoot);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mViewRoot, paramViewRoot);

        if(cameraProfile.getMode().equals(CameraSetting.CAMERA_MODE_OFF))
            toggleView(cameraPreview, View.GONE);

        mCameraLayout.findViewById(R.id.root_camera_container).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        //remember the initial position.
                        initialX = paramCam.x;
                        initialY = paramCam.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(event.getRawX() < mScreenWidth/2) {
                            paramCam.x = 0;
                        }
                        else {
                            paramCam.x = mScreenWidth;
                        }
                        paramCam.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mCameraLayout, paramCam);


                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Xdiff < 20 && Ydiff < 20) {
//                            if (isViewCollapsed()) {
//                                //When user clicks on the image view of the collapsed layout,
//                                //visibility of the collapsed layout will be changed to "View.GONE"
//                                //and expanded view will become visible.
//                                toggleNavigationButton(View.VISIBLE);
//                            }
//                            else {
//                                toggleNavigationButton(View.GONE);
//                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        paramCam.x = initialX + (int) (event.getRawX() - initialTouchX);
                        paramCam.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mCameraLayout, paramCam);
                        return true;
                }

                return false;
            }
        });
    }

    private void calculateCameraSize(CameraSetting cameraProfile) {
        int factor;
        switch (cameraProfile.getSize()){
            case CameraSetting.SIZE_BIG:
                factor = 3;
                break;
            case CameraSetting.SIZE_MEDIUM:
                factor = 4;
                break;
            default: //small
                factor = 5;
                break;
        }
        if(mScreenWidth > mScreenHeight) {//landscape
            mCameraWidth = mScreenWidth / factor;
//            mCameraHeight = mScreenHeight / factor;
            mCameraHeight = mCameraWidth*3/4;
        }
        else{
            mCameraWidth = mScreenHeight/factor;
//            mCameraHeight = mScreenWidth/factor;
            mCameraHeight = mCameraWidth*3/4;
        }
        if(DEBUG) Log.i(TAG, "calculateCameraSize: "+mScreenWidth+"x"+mScreenHeight);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
        getWindowManagerDefaultDisplay();

        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mViewRoot.getLayoutParams();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {


            if (layoutParams.y + (mViewRoot.getHeight() + getStatusBarHeight()) > szWindow.y) {
                layoutParams.y = szWindow.y - (mViewRoot.getHeight() + getStatusBarHeight());
                mWindowManager.updateViewLayout(mViewRoot, layoutParams);
            }

            if (layoutParams.x != 0 && layoutParams.x < szWindow.x) {
                resetPosition(szWindow.x);
            }

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            if (layoutParams.x > szWindow.x) {
                resetPosition(szWindow.x);
            }

        }
        if(DEBUG) Log.i(TAG, "onConfigurationChanged: DETECTED" + newConfig.orientation);
        updateScreenSize();

        if(paramViewRoot!=null){
            paramViewRoot.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            paramViewRoot.x = 0;
            paramViewRoot.y = 0;
        }

        if(cameraPreview!=null) {
            int width = mCameraWidth, height = mCameraHeight;

            ViewGroup.LayoutParams params = cameraPreview.getLayoutParams();

            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                params.height = width;
                params.width = height;

            } else {
                params.height = height;
                params.width = width;
            }

            cameraPreview.setLayoutParams(params);
        }
    }

    private void initializeViews() {
        if(DEBUG) Log.i(TAG, "StreamingControllerService: initializeViews()");

        mViewRoot = LayoutInflater.from(this).inflate(R.layout.layout_recording_again, null);

        mVideoPopupWindowView = LayoutInflater.from(this).inflate(R.layout.video_popup_window, null);

        View mViewCountdown = LayoutInflater.from(this).inflate(R.layout.layout_countdown, null);



//        paramViewRoot.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
//        paramViewRoot.x = 0;
//        paramViewRoot.y = 0;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        getWindowManagerDefaultDisplay();
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        addRemoveView(inflater);
        addFloatingWidgetView(inflater);
        mWindowManager.addView(mViewCountdown, paramCountdown);

    //    mWindowManager.addView(mViewRoot, paramViewRoot);

        mCountdownLayout = mViewCountdown.findViewById(R.id.countdown_container);
        mTvCountdown = mViewCountdown.findViewById(R.id.tvCountDown);

        toggleView(mCountdownLayout, View.GONE);
        // toggleView(mTransparentBackgroundLayout, View.GONE);

        mImgRec = mViewRoot.findViewById(R.id.imgRec);
        mImgCapture = mViewRoot.findViewById(R.id.imgCapture);
        mImgClose = mViewRoot.findViewById(R.id.imgClose);
        mImgLive = mViewRoot.findViewById(R.id.imgLive);
        mImgPause = mViewRoot.findViewById(R.id.imgPause);
        mImgStart = mViewRoot.findViewById(R.id.imgStart);
        mImgSetting = mViewRoot.findViewById(R.id.imgSetting);
        mImgStop = mViewRoot.findViewById(R.id.imgStop);

        popupvideo=mVideoPopupWindowView.findViewById(R.id.popup_video);
        cancelvideopopup=mVideoPopupWindowView.findViewById(R.id.close_video_popup_window);
        delvideopopup=mVideoPopupWindowView.findViewById(R.id.delete_popup_video);
        sharevideopopup=mVideoPopupWindowView.findViewById(R.id.share_popup_video);




        toggleView(mImgStop, View.INVISIBLE);
        toggleNavigationButton(View.GONE);


        //For live Stream Camera
        mImgCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationButton(View.GONE);
                if(mCameraLayout.getVisibility() == View.GONE){
                    toggleView(mCameraLayout, View.VISIBLE);
                }
                else{
                    toggleView(mCameraLayout, View.GONE);
                }
            }
        });

        //for screenshot
        mImgPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationButton(View.GONE);
                toggleView(mViewRoot, View.GONE);
                Timer buttonTimer0 = new Timer();
                buttonTimer0.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startCapture();
                            }
                        });
                    }
                }, 1000);

                Timer buttonTimer = new Timer();
                buttonTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                toggleView(mViewRoot, View.VISIBLE);
                            }
                        });
                    }
                }, 500);
            }
        });

      //for open settings activity
        mImgSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                toggleNavigationButton(View.GONE);
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(MyUtils.ACTION_OPEN_SETTING_ACTIVITY);
                startActivity(intent);

            }
        });

        //for start recording
        mImgStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationButton(View.GONE);

                if(mRecordingServiceBound){

                    toggleView(mCountdownLayout, View.VISIBLE);

                    int countdown = (SettingManager.getCountdown(getApplication())+1) * 1000;

                    new CountDownTimer(countdown, 1000) {

                        public void onTick(long millisUntilFinished) {
                            toggleView(mViewRoot, View.GONE);
                            mTvCountdown.setText(""+(millisUntilFinished / 1000));
                        }

                        public void onFinish() {
                            toggleView(mCountdownLayout, View.GONE);

                            toggleView(mViewRoot, View.VISIBLE);
                            mRecordingStarted = true;
                            mService.startPerformService();
                            if(splashVariable.sp.contains("third")) {
                                MainActivity.mImgRec.setVisibility(View.INVISIBLE);
                                MainActivity.mImgStop.setVisibility(View.VISIBLE);
                            }
                            contentView.setViewVisibility(R.id.recordL,View.GONE);
                            contentView.setViewVisibility(R.id.stopL,View.VISIBLE);
                            manager.notify(2, notification);

                        }
                    }.start();

                }
                else{
                    mRecordingStarted = false;
                    MyUtils.toast(getApplicationContext(), "Recording Service connection has not been established", Toast.LENGTH_LONG);
                    Log.e(TAG, "Recording Service connection has not been established");
                    stopService();
                }
            }
        });

        //for stopping recording
        mImgStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationButton(View.GONE);

                if(mRecordingServiceBound){
                    //Todo: stop and save recording
                    mRecordingStarted = false;

                    mService.stopPerformService();
                    if(splashVariable.sp.contains("third")) {
                        MainActivity.mImgStop.setVisibility(View.INVISIBLE);
                        MainActivity.mImgRec.setVisibility(View.VISIBLE);
                    }
                    contentView.setViewVisibility(R.id.stopL,View.GONE);
                    contentView.setViewVisibility(R.id.recordL,View.VISIBLE);
                    manager.notify(2, notification);

                    if(mMode==MyUtils.MODE_RECORDING){
                        ((RecordingService)mService).insertVideoToGallery();
                        ((RecordingService)mService).saveVideoToDatabase();

                        mWindowManager.addView(mVideoPopupWindowView, paramVideoPopup);
                        Glide.with(ControllerService.this).load(RecordingService.outputFile).into(popupvideo);
                        popupvideo.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //   mVideoPopupWindowView.setVisibility(View.GONE);
                                Intent vi = new Intent(Intent.ACTION_VIEW);
                                vi.setDataAndType(Uri.parse(RecordingService.outputFile), "video/mp4");
                                vi.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ControllerService.this.startActivity(vi);
                                mWindowManager.removeView(mVideoPopupWindowView);
                            }
                        });
                        cancelvideopopup.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //    mVideoPopupWindowView.setVisibility(View.GONE);
                                mWindowManager.removeView(mVideoPopupWindowView);
                            }
                        });
                        delvideopopup.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {



                                File out=new File(RecordingService.outputFile);
                                if(out.exists()){
                                    out.delete();
                                }
                                //    mVideoPopupWindowView.setVisibility(View.GONE);
                                mWindowManager.removeView(mVideoPopupWindowView);


                            }
                        });
                        sharevideopopup.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //    mVideoPopupWindowView.setVisibility(View.GONE);
                                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                sharingIntent.setType("video/*");
                                sharingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                sharingIntent.putExtra(Intent.EXTRA_STREAM, RecordingService.outputFile);
                                Intent startIntent = Intent.createChooser(sharingIntent, "Share Video");
                                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ControllerService.this.startActivity(startIntent);
                                mWindowManager.removeView(mVideoPopupWindowView);

                            }
                        });

                        return;
                    }

                }
                else{
                    mRecordingStarted = true;
                    MyUtils.toast(getApplicationContext(), "Recording Service connection has not been established", Toast.LENGTH_LONG);
                }
            }
        });

        mImgLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationButton(View.GONE);
                if(!MainActivity.active) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction(MyUtils.ACTION_OPEN_VIDEO_MANAGER_ACTIVITY);
                    startActivity(intent);
                }
            }
        });

        //FOR CLOSING SERVICE
        mImgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mRecordingStarted){
                    mImgStop.performClick();
                }
                splashVariable.sp="first";
//                toggleView(mViewRoot,View.GONE);
//                toggleView(mCameraLayout,View.GONE);

                stopService();

                if(!DEBUG)
                    if(!MainActivity.active) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (mMode == MyUtils.MODE_RECORDING) {
                            intent.setAction(MyUtils.ACTION_OPEN_VIDEO_MANAGER_ACTIVITY);
                        } else {
                            intent.setAction(MyUtils.ACTION_OPEN_LIVE_ACTIVITY);
                        }
                        startActivity(intent);
                    }
            }
        });

        mViewRoot.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            long time_start = 0, time_end = 0;

            boolean isLongClick = false;//variable to judge if user click long press
            boolean inBounded = false;//variable to judge if floating view is bounded to remove view
            int remove_img_width = 0, remove_img_height = 0;

            Handler handler_longClick = new Handler();
            Runnable runnable_longClick = new Runnable() {
                @Override
                public void run() {
                    //On Floating Widget Long Click

                    //Set isLongClick as true
                    isLongClick = true;

                    //Set remove widget view visibility to VISIBLE
                    removeFloatingWidgetView.setVisibility(View.VISIBLE);

                    onFloatingWidgetLongClick();
                }
            };


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //Get Floating widget view params
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mViewRoot.getLayoutParams();

                //get the touch location coordinates
                int x_cord = (int) event.getRawX();
                int y_cord = (int) event.getRawY();

                int x_cord_Destination, y_cord_Destination;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                         time_start = System.currentTimeMillis();

                        handler_longClick.postDelayed(runnable_longClick, 600);

                        remove_img_width = remove_image_view.getLayoutParams().width;
                        remove_img_height = remove_image_view.getLayoutParams().height;

                        x_init_cord = x_cord;
                        y_init_cord = y_cord;

                        //remember the initial position.
                        x_init_margin = layoutParams.x;
                        y_init_margin = layoutParams.y;




//                        //remember the initial position.
//                        initialX = paramViewRoot.x;       //x_init_margin
//                        initialY = paramViewRoot.y;       //y_init_margin
//
//                        //get the touch location
//                        initialTouchX = event.getRawX();   //x_init_cord
//                        initialTouchY = event.getRawY();   //y_init_cord
                        return true;
                    case MotionEvent.ACTION_UP:
                        isLongClick = false;
                        removeFloatingWidgetView.setVisibility(View.GONE);
                        remove_image_view.getLayoutParams().height = remove_img_height;
                        remove_image_view.getLayoutParams().width = remove_img_width;
                        handler_longClick.removeCallbacks(runnable_longClick);

                        //If user drag and drop the floating widget view into remove view then stop the service
                        if (inBounded) {
//                            splashVariable.sp="first";
//                            stopSelf();
                           toggleView(mViewRoot,View.GONE);

                            inBounded = false;
                            break;
                        }


                        //Get the difference between initial coordinate and current coordinate
                        int x_diff = x_cord - x_init_cord;
                        int y_diff = y_cord - y_init_cord;

                        //The check for x_diff <5 && y_diff< 5 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Math.abs(x_diff) < 5 && Math.abs(y_diff) < 5) {
                            time_end = System.currentTimeMillis();

                            //Also check the difference between start time and end time should be less than 300ms
                            if ((time_end - time_start) < 300)
                                if (isViewCollapsed()) {
                                    //When user clicks on the image view of the collapsed layout,
                                    //visibility of the collapsed layout will be changed to "View.GONE"
                                    //and expanded view will become visible.
                                    toggleNavigationButton(View.VISIBLE);
                                }
                                else {
                                    toggleNavigationButton(View.GONE);
                                }

                        }

                        y_cord_Destination = y_init_margin + y_diff;

                        int barHeight = getStatusBarHeight();
                        if (y_cord_Destination < 0) {
                            y_cord_Destination = 0;
                        } else if (y_cord_Destination + (mViewRoot.getHeight() + barHeight) > szWindow.y) {
                            y_cord_Destination = szWindow.y - (mViewRoot.getHeight() + barHeight);
                        }

                        layoutParams.y = y_cord_Destination;

                        inBounded = false;

                        //reset position if user drags the floating view
                        resetPosition(x_cord);











//                        if(event.getRawX() < mScreenWidth/2) {
//                            paramViewRoot.x = 0;
//                        }
//                        else {
//                            paramViewRoot.x = mScreenWidth;
//                        }
//                        paramViewRoot.y = initialY + (int) (event.getRawY() - initialTouchY);
//
//                        //Update the layout with new X & Y coordinate
//                        mWindowManager.updateViewLayout(mViewRoot, paramViewRoot);
//
//
//                        Xdiff = (int) (event.getRawX() - initialTouchX);
//                        Ydiff = (int) (event.getRawY() - initialTouchY);
//
//                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
//                        //So that is click event.
//                        if (Xdiff < 20 && Ydiff < 20) {
//                            //           time_end = System.currentTimeMillis();
//                            if (isViewCollapsed()) {
//                                //When user clicks on the image view of the collapsed layout,
//                                //visibility of the collapsed layout will be changed to "View.GONE"
//                                //and expanded view will become visible.
//                                toggleNavigationButton(View.VISIBLE);
//                            }
//                            else {
//                                toggleNavigationButton(View.GONE);
//                            }
//                        }






                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int x_diff_move = x_cord - x_init_cord;
                        int y_diff_move = y_cord - y_init_cord;

                        x_cord_Destination = x_init_margin + x_diff_move;
                        y_cord_Destination = y_init_margin + y_diff_move;

                        //If user long click the floating view, update remove view
                        if (isLongClick) {
                            int x_bound_left = szWindow.x / 2 - (int) (remove_img_width * 1.5);
                            int x_bound_right = szWindow.x / 2 + (int) (remove_img_width * 1.5);
                            int y_bound_top = szWindow.y - (int) (remove_img_height * 1.5);

                            //If Floating view comes under Remove View update Window Manager
                            if ((x_cord >= x_bound_left && x_cord <= x_bound_right) && y_cord >= y_bound_top) {
                                inBounded = true;

                                int x_cord_remove = (int) ((szWindow.x - (remove_img_height * 1.5)) / 2);
                                int y_cord_remove = (int) (szWindow.y - ((remove_img_width * 1.5) + getStatusBarHeight()));

                                if (remove_image_view.getLayoutParams().height == remove_img_height) {
                                    remove_image_view.getLayoutParams().height = (int) (remove_img_height * 1.5);
                                    remove_image_view.getLayoutParams().width = (int) (remove_img_width * 1.5);

                                    WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeFloatingWidgetView.getLayoutParams();
                                    param_remove.x = x_cord_remove;
                                    param_remove.y = y_cord_remove;

                                    mWindowManager.updateViewLayout(removeFloatingWidgetView, param_remove);
                                }

                                layoutParams.x = x_cord_remove + (Math.abs(removeFloatingWidgetView.getWidth() - mViewRoot.getWidth())) / 2;
                                layoutParams.y = y_cord_remove + (Math.abs(removeFloatingWidgetView.getHeight() - mViewRoot.getHeight())) / 2;

                                //Update the layout with new X & Y coordinate
                                mWindowManager.updateViewLayout(mViewRoot, layoutParams);
                                break;
                            } else {
                                //If Floating window gets out of the Remove view update Remove view again
                                inBounded = false;
                                remove_image_view.getLayoutParams().height = remove_img_height;
                                remove_image_view.getLayoutParams().width = remove_img_width;
                                if (isViewCollapsed()) {
                                    //When user clicks on the image view of the collapsed layout,
                                    //visibility of the collapsed layout will be changed to "View.GONE"
                                    //and expanded view will become visible.
                                    toggleNavigationButton(View.VISIBLE);
                                }
                                else {
                                    toggleNavigationButton(View.GONE);
                                }
                            }

                        }


                        layoutParams.x = x_cord_Destination;
                        layoutParams.y = y_cord_Destination;

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mViewRoot, layoutParams);

//                        //Calculate the X and Y coordinates of the view.
//                        paramViewRoot.x = initialX + (int) (event.getRawX() - initialTouchX);
//                        paramViewRoot.y = initialY + (int) (event.getRawY() - initialTouchY);
//
//                        //Update the layout with new X & Y coordinate
//                        mWindowManager.updateViewLayout(mViewRoot, paramViewRoot);
                        return true;
                }

                return false;
            }
        });
        mViewRoot.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus)
                    toggleNavigationButton(View.GONE);

            }
        });

    }


    private void stopService() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true);
                stopSelf();
            } else {
                stopSelf();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private void toggleView(View view, int visible) {
        view.setVisibility(visible);
    }

    private void bindStreamingService() {
        if(DEBUG)
            Log.i(TAG, "Controller: bindService()");

        Intent service;

        if(mMode == MyUtils.MODE_STREAMING) {
            if(mStreamProfile == null)
                throw new RuntimeException("Streaming proflie is null");

            service = new Intent(getApplicationContext(), StreamingService.class);
            Bundle bundle = new Bundle();

            bundle.putSerializable(MyUtils.STREAM_PROFILE, mStreamProfile);

            service.putExtras(bundle);

        }
        else {
            service = new Intent(getApplicationContext(), RecordingService.class);
        }

        service.putExtra(Intent.EXTRA_INTENT, mScreenCaptureIntent);

        bindService(service, mStreamingServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection mStreamingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IBinder binder;
            if(mMode == MyUtils.MODE_STREAMING) {
                binder = (StreamingBinder) service;
                mService = ((StreamingBinder) binder).getService();
            }
            else{
                binder = (RecordingBinder)service;
                mService = ((RecordingBinder) binder).getService();
            }
            mRecordingServiceBound = true;


        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRecordingServiceBound = false;

        }
    };

    private boolean isViewCollapsed() {
        return mViewRoot == null || mViewRoot.findViewById(R.id.imgSetting).getVisibility() == View.GONE;
    }

    void toggleNavigationButton(int viewMode){
        //Todo: make animation here

        mImgStart.setVisibility(viewMode);
        mImgSetting.setVisibility(viewMode);
        mImgPause.setVisibility(viewMode);
        mImgCapture.setVisibility(viewMode);
        mImgLive.setVisibility(viewMode);
        mImgClose.setVisibility(viewMode);
        mImgStop.setVisibility(viewMode);


        if(viewMode == View.GONE){
            mViewRoot.setPadding(32,32, 32, 32);
        }else{
            if(mRecordingStarted){
                mImgStart.setVisibility(View.INVISIBLE);

            }
            else{
                mImgStop.setVisibility(View.INVISIBLE);

            }


            mViewRoot.setPadding(32,48, 32, 48);
        }
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mViewRoot!=null){
            mWindowManager.removeViewImmediate(mViewRoot);
        }
        if(mImgPopupWindowView!=null)
        {
            mWindowManager.removeView(mImgPopupWindowView);
        }
        if (removeFloatingWidgetView != null) {
            mWindowManager.removeView(removeFloatingWidgetView);
        }
//        if(mVideoPopupWindowView!=null)
//        {
//            mWindowManager.removeView(mVideoPopupWindowView);
//        }

        if(mCameraLayout!=null){
            mWindowManager.removeView(mCameraLayout);
            releaseCamera();
        }

        if(mService !=null && mRecordingServiceBound) {
            unbindService(mStreamingServiceConnection);
            mService.stopSelf();
            mRecordingServiceBound = false;
        }
        if (null != shotIntent) {
            stopService(shotIntent);
        }
        stopCapture();
        if(rec!=null) {
            unregisterReceiver(rec);
        }
        if(stp!=null) {
            unregisterReceiver(stp);
        }
        if(screensht!=null) {
            unregisterReceiver(screensht);
        }
        if(ext!=null) {
            unregisterReceiver(ext);
        }

    }


}
