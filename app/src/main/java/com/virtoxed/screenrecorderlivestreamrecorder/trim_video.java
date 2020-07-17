package com.virtoxed.screenrecorderlivestreamrecorder;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.virtoxed.screenrecorderlivestreamrecorder.fragments.EditFragment;

import java.io.File;

import life.knowledge4.videotrimmer.K4LVideoTrimmer;
import life.knowledge4.videotrimmer.interfaces.OnTrimVideoListener;

public class trim_video extends AppCompatActivity {


    static String path;
    K4LVideoTrimmer videoTrimmer;
    ProgressBar progressBar;

    Activity activity;

    public static void addpath( String  p ){
        path = p;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trim_video);
        Window window = this.getWindow();

// clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

// finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.tabcolor));
        getSupportActionBar().setTitle("Trim Video");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        videoTrimmer  = ((K4LVideoTrimmer) findViewById(R.id.timeLine));
        progressBar=findViewById(R.id.progressbar);
        progressBar.setVisibility(View.INVISIBLE);
        videoTrimmer.setMaxDuration(14400);


        activity = this ;

        File direct = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES) + "/Screen Recorder Live Stream Recorder");

        if (!direct.exists()) {
            direct.mkdirs();
        }


        final String output = direct+"/"+"TrimmedVideo-" +
                Long.toHexString(System.currentTimeMillis())+".mp4";
        videoTrimmer.setDestinationPath(output);


        if (videoTrimmer != null) {
            videoTrimmer.setVideoURI(Uri.parse(path));
        }

        videoTrimmer.setOnTrimVideoListener(new OnTrimVideoListener() {
            @Override
            public void getResult(Uri uri) {
progressBar.setVisibility(View.VISIBLE);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(trim_video.this ,"Trimmed Video Saved in Gallery Successfully. ", Toast.LENGTH_SHORT).show();

                    }
                });


                System.out.println( "file = " + uri.getPath() + " = " + uri.toString()  );


                File f = new File( uri.getPath() );
                File to = new File( output );

                f.renameTo(to);

                finish();


            }

            @Override
            public void cancelAction() {
                Fragment fragment = new EditFragment();
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.trimlayout, fragment).commit();
                finish();

            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Fragment fragment = new EditFragment();
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.trimlayout, fragment).commit();
                finish();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
