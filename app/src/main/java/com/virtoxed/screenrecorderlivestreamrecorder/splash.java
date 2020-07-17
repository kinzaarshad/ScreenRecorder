package com.virtoxed.screenrecorderlivestreamrecorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils;

import java.util.Timer;
import java.util.TimerTask;

public class splash extends AppCompatActivity {
    private ProgressBar mProgress;
    int counter=0;
    private static boolean sp=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Window window = this.getWindow();

// clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

// finally change the color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.tabcolor));
        }
if(sp) {
    sp=false;
    doWork();
}
else {
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
}
