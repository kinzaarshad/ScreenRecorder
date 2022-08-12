package com.virtoxed.screenrecorderlivestreamrecorder;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.virtoxed.screenrecorderlivestreamrecorder.adapters.ViewPaperAdapter;
import com.virtoxed.screenrecorderlivestreamrecorder.controllers.settings.SettingManager;
import com.virtoxed.screenrecorderlivestreamrecorder.fragments.EditFragment;
import com.virtoxed.screenrecorderlivestreamrecorder.fragments.ImageFragment;
import com.virtoxed.screenrecorderlivestreamrecorder.fragments.SettingFragment;
import com.virtoxed.screenrecorderlivestreamrecorder.fragments.VideoManagerFragment;
import com.virtoxed.screenrecorderlivestreamrecorder.services.BaseService;
import com.virtoxed.screenrecorderlivestreamrecorder.services.ControllerService;
import com.virtoxed.screenrecorderlivestreamrecorder.services.recording.RecordingService;
import com.virtoxed.screenrecorderlivestreamrecorder.services.streaming.StreamingService;
import com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.virtoxed.screenrecorderlivestreamrecorder.helper.StreamProfile;

import java.io.File;
import java.util.Objects;

import static com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils.isMyServiceRunning;

public class MainActivity extends AppCompatActivity implements NavigationView.OnCreateContextMenuListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    DrawerLayout mDrawer;
    NavigationView navigationView;
    public static boolean active = false;
    private static final boolean DEBUG = MyUtils.DEBUG;
    private static final int PERMISSION_REQUEST_CODE = 3004;
    private static final int PERMISSION_DRAW_OVER_WINDOW = 3005;
    private static final int PERMISSION_RECORD_DISPLAY = 3006;
    private static String[] mPermission = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public int mMode = MyUtils.MODE_RECORDING;

    public static ImageView mImgRec,mImgStop;

    private Intent mScreenCaptureIntent = null;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private ViewPaperAdapter mAdapter;

    private int [] tabIcons = {
            R.drawable.ic_video_24,
            R.drawable.ic_photo,
//            R.drawable.ic_live,
            R.drawable.ic_edit,
            R.drawable.ic_setting
    };

    private int mScreenCaptureResultCode = MyUtils.RESULT_CODE_FAILED;

    private StreamProfile mStreamProfile;
    ControllerService cs;
    WindowManager.LayoutParams paramCountdown;
    private TextView mTvCountdown;
    private View mCountdownLayout;
    private WindowManager mWindowManager;
    WindowManager.LayoutParams paramVideoPopup;
    private int count=0;

    private ImageView delvideopopup,cancelvideopopup,sharevideopopup,popupvideo;
    private View mVideoPopupWindowView;
    private static final int REQUEST_SCREENSHOT=59706;
    private MediaProjectionManager mgr;
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Window window = this.getWindow();

// clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

// finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.tabcolor));
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_hamburger);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        getSupportActionBar().setTitle("Videos");

        mDrawer= findViewById(R.id.drawer_layout);
        navigationView=findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                int id = menuItem.getItemId();

                if (id == R.id.nav_videos) {
                    mTabLayout.getTabAt(0).select();

                } else if (id == R.id.nav_images) {
                    mTabLayout.getTabAt(1).select();
                }
                else if (id == R.id.nav_edit) {
                    mTabLayout.getTabAt(2).select();

                }
                else if (id == R.id.nav_settings) {
                    mTabLayout.getTabAt(3).select();
                }


                menuItem.setChecked(true);
                mDrawer.closeDrawers();
                return true;

            }
        });

        initViews();


        Intent intent = getIntent();
        if(intent!=null) {
            handleIncomingRequest(intent);
        }


        cs=new ControllerService();
        if(isMyServiceRunning(getApplicationContext(), StreamingService.class))
        {

            return;
        }
        if(isMyServiceRunning(getApplicationContext(), ControllerService.class)){



            return;
        }
        mMode = MyUtils.MODE_RECORDING;

        //shouldStartControllerService();
        mgr=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);

        startActivityForResult(mgr.createScreenCaptureIntent(),
                REQUEST_SCREENSHOT);

    }

    private void handleIncomingRequest(Intent intent) {
        if(intent != null) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case MyUtils.ACTION_OPEN_SETTING_ACTIVITY:
                    mTabLayout.getTabAt(3).select();

                    break;
                case MyUtils.ACTION_OPEN_EDIT_ACTIVITY:
                    mTabLayout.getTabAt(2).select();

                    break;

                case MyUtils.ACTION_OPEN_IMAGE_ACTIVITY:
                    mTabLayout.getTabAt(1).select();

                    break;
                case MyUtils.ACTION_OPEN_VIDEO_MANAGER_ACTIVITY:
                    mTabLayout.getTabAt(0).select();

                    break;
                case MyUtils.ACTION_START_CAPTURE_NOW:
                    mImgRec.performClick();
                    break;
            }
        }
    }

    private void requestScreenCaptureIntent() {
        if(mScreenCaptureIntent == null){
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), PERMISSION_RECORD_DISPLAY);
        }
    }



    private void initViews() {
        mViewPager = findViewById(R.id.viewpaper);
        setupViewPaper();

        mTabLayout = findViewById(R.id.tabLayout);
        mTabLayout.setupWithViewPager(mViewPager);

        setupTabIcon();

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
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
        popupvideo=mVideoPopupWindowView.findViewById(R.id.popup_video);
        cancelvideopopup=mVideoPopupWindowView.findViewById(R.id.close_video_popup_window);
        delvideopopup=mVideoPopupWindowView.findViewById(R.id.delete_popup_video);
        sharevideopopup=mVideoPopupWindowView.findViewById(R.id.share_popup_video);

        View mViewCountdown = LayoutInflater.from(this).inflate(R.layout.layout_countdown, null);

        mCountdownLayout = mViewCountdown.findViewById(R.id.countdown_container);
        mTvCountdown = mViewCountdown.findViewById(R.id.tvCountDown);
        mCountdownLayout.setVisibility(View.GONE);

        mImgRec =  findViewById(R.id.fab_rec);
        mImgStop =  findViewById(R.id.fab_rec_stop);

        mImgRec.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                if(count==0) {
                    mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                    mWindowManager.addView(mViewCountdown, paramCountdown);
                    count++;
                }
                mCountdownLayout.setVisibility(View.VISIBLE);
                int countdown = (SettingManager.getCountdown(getApplication())+1) * 1000;

                new CountDownTimer(countdown, 1000) {

                    public void onTick(long millisUntilFinished) {

                        mTvCountdown.setText(""+(millisUntilFinished / 1000));
                    }

                    public void onFinish() {
                        cs.mService.startPerformService();
                        mCountdownLayout.setVisibility(View.GONE);
                        mImgRec.setVisibility(View.INVISIBLE);
                        mImgStop.setVisibility(View.VISIBLE);
                        ControllerService.mImgStart.setVisibility(View.INVISIBLE);
                        ControllerService.mImgStop.setVisibility(View.VISIBLE);
                        ControllerService.contentView.setViewVisibility(R.id.recordL,View.GONE);
                        ControllerService.contentView.setViewVisibility(R.id.stopL,View.VISIBLE);
                        ControllerService.manager.notify(2, ControllerService.notification);


                    }
                }.start();

            }
        });

        mImgStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    cs.mService.stopPerformService();
                mImgStop.setVisibility(View.INVISIBLE);
                mImgRec.setVisibility(View.VISIBLE);
                ControllerService.mImgStop.setVisibility(View.INVISIBLE);
                ControllerService.mImgStart.setVisibility(View.VISIBLE);
                ControllerService.contentView.setViewVisibility(R.id.stopL,View.GONE);
                ControllerService.contentView.setViewVisibility(R.id.recordL,View.VISIBLE);
                ControllerService.manager.notify(2, ControllerService.notification);
                if(mMode==MyUtils.MODE_RECORDING){
                    ((RecordingService)cs.mService).insertVideoToGallery();
                    ((RecordingService)cs.mService).saveVideoToDatabase();
                    mWindowManager.addView(mVideoPopupWindowView, paramVideoPopup);
                    Glide.with(MainActivity.this).load(RecordingService.outputFile).into(popupvideo);
                    popupvideo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mVideoPopupWindowView.setVisibility(View.GONE);
                            Intent vi = new Intent(Intent.ACTION_VIEW);
                            vi.setDataAndType(Uri.parse(RecordingService.outputFile), "video/mp4");
                            vi.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(vi);
                            mWindowManager.removeView(mVideoPopupWindowView);
                        }
                    });
                    cancelvideopopup.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mVideoPopupWindowView.setVisibility(View.GONE);
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
                                    mVideoPopupWindowView.setVisibility(View.GONE);
                            mWindowManager.removeView(mVideoPopupWindowView);

                        }
                    });
                    sharevideopopup.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mVideoPopupWindowView.setVisibility(View.GONE);
                            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                            sharingIntent.setType("video/*");
                            sharingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            sharingIntent.putExtra(Intent.EXTRA_STREAM, RecordingService.outputFile);
                            Intent startIntent = Intent.createChooser(sharingIntent, "Share Video");
                            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(startIntent);
                            mWindowManager.removeView(mVideoPopupWindowView);

                        }
                    });
                    Intent intent = getIntent();
                    overridePendingTransition(0, 0);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(intent);
                    return;
                }


            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if(position ==0)
                {
                    getSupportActionBar().setTitle("Videos");


                }
                else if(position == 1){
                    getSupportActionBar().setTitle("Images");
                }
                else if(position ==2)
                {
                    getSupportActionBar().setTitle("Edit");
                }
                else if(position == 3){
                    getSupportActionBar().setTitle("Settings");
                }

                else{

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

    }

    private void setupTabIcon() {
        mTabLayout.getTabAt(0).setIcon(tabIcons[0]);
        mTabLayout.getTabAt(1).setIcon(tabIcons[1]);
        mTabLayout.getTabAt(2).setIcon(tabIcons[2]);
        mTabLayout.getTabAt(3).setIcon(tabIcons[3]);
        mTabLayout.getTabAt(0).select();
    }

    private void setupViewPaper() {
        mAdapter = new ViewPaperAdapter(getSupportFragmentManager());
        mAdapter.addFragment(new VideoManagerFragment(), "Video");
        mAdapter.addFragment(new ImageFragment(), "Image");

        mAdapter.addFragment(new EditFragment(), "Edit");
        mAdapter.addFragment(new SettingFragment(), "Setting");
        mViewPager.setAdapter(mAdapter);
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
                            MyUtils.showSnackBarNotification(mImgRec,"Please grant all permissions to record screen.", Snackbar.LENGTH_LONG);
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
            //startControllerService();
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode==REQUEST_SCREENSHOT) {
            if (resultCode==RESULT_OK) {
                Intent i=
                        new Intent(this, ControllerService.class)
                                .putExtra(ControllerService.EXTRA_RESULT_CODE, resultCode)
                                .putExtra(ControllerService.EXTRA_RESULT_INTENT, data);

                startService(i);
            }
        }
        if (requestCode == PERMISSION_DRAW_OVER_WINDOW) {

            //Check if the permission is granted or not.
            if (resultCode != RESULT_OK) { //Permission is not available
                MyUtils.showSnackBarNotification(mImgRec, "Draw over other app permission not available.",Snackbar.LENGTH_SHORT);
            }
        }
        else if( requestCode == PERMISSION_RECORD_DISPLAY) {
            if(resultCode != RESULT_OK){
                MyUtils.showSnackBarNotification(mImgRec, "Recording display permission not available.",Snackbar.LENGTH_SHORT);
                mScreenCaptureIntent = null;
            }
            else{
                mScreenCaptureIntent = data;
                mScreenCaptureIntent.putExtra(MyUtils.SCREEN_CAPTURE_INTENT_RESULT_CODE, resultCode);
                mScreenCaptureResultCode = resultCode;

                //shouldStartControllerService();
            }
        }
        else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startControllerService() {
        Intent controller = new Intent(MainActivity.this, ControllerService.class);

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

//        if(mMode==MyUtils.MODE_RECORDING)
           // finish();
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
            Intent controller = new Intent(MainActivity.this, ControllerService.class);

            controller.setAction(MyUtils.ACTION_UPDATE_STREAM_PROFILE);
            Bundle bundle = new Bundle();
            bundle.putSerializable(MyUtils.STREAM_PROFILE, mStreamProfile);
            controller.putExtras(bundle);
            //startService(controller);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

}

