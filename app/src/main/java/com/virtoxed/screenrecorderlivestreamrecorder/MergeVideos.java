package com.virtoxed.screenrecorderlivestreamrecorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.virtoxed.screenrecorderlivestreamrecorder.fragments.EditFragment;
import com.virtoxed.screenrecorderlivestreamrecorder.utils.UriUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import VideoHandle.EpEditor;
import VideoHandle.EpVideo;
import VideoHandle.OnEditorListener;

public class MergeVideos extends AppCompatActivity implements View.OnClickListener{
    private static final int CHOOSE_FILE = 11;
    private TextView tv_add;
    private Button bt_add, bt_merge;
    private List<EpVideo> videoList;
    private ProgressDialog mProgressDialog;
  //  ImageView thumb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_videos);
        Window window = this.getWindow();

// clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

// finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.tabcolor));
        getSupportActionBar().setTitle("Merge Videos");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE },1);
            }

        }
        initView();
    }
    private void initView() {
     //   thumb=findViewById(R.id.thumb);
        tv_add = (TextView) findViewById(R.id.tv_add);
        bt_add = (Button) findViewById(R.id.bt_add);
        bt_merge = (Button) findViewById(R.id.bt_merge);
        videoList = new ArrayList<>();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMax(100);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setTitle("Processing");

        bt_add.setOnClickListener(this);
        bt_merge.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_add:
                chooseFile();
                break;
            case R.id.bt_merge:
                mergeVideo();
                break;
        }
    }

    private void chooseFile() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_PICK);
       // intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode==RESULT_OK && data !=null && data.getData() !=null || requestCode == 1) {
            String videoUrl = UriUtils.getPath(MergeVideos.this, data.getData());
//            Glide.with(this)
//                    .load(videoUrl) // or URI/path
//                    .into(thumb);
            tv_add.setText(tv_add.getText() + videoUrl + "\n");
            videoList.add(new EpVideo(videoUrl));

        }


    }

    private void mergeVideo() {
        if (videoList.size() > 1) {
            mProgressDialog.setProgress(0);
            mProgressDialog.show();
            File direct = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES) + "/Screen Recorder Live Stream Recorder");

            final String  outPath = direct+"/"+"MergedVideo-" +
                    Long.toHexString(System.currentTimeMillis())+".mp4";
            EpEditor.OutputOption outputOption = new EpEditor.OutputOption(outPath);
            outputOption.setWidth(480); //Output video width, default 480
            outputOption.setHeight(800);//Output video height, default 360
            outputOption.frameRate = 30;//Output video frame rate, default 30
            outputOption.bitRate = 10;//Output video bit rate, default 10
            EpEditor.merge(videoList, outputOption, new OnEditorListener() {
                @Override
                public void onSuccess() {

                    mProgressDialog.dismiss();

                    Intent v = new Intent(Intent.ACTION_VIEW);
                    v.setDataAndType(Uri.parse(outPath), "video/mp4");
                    startActivity(v);

                }

                @Override
                public void onFailure() {
                    Toast.makeText(MergeVideos.this, "Edit Failed", Toast.LENGTH_SHORT).show();
                    mProgressDialog.dismiss();
                }

                @Override
                public void onProgress(float v) {
                    mProgressDialog.setProgress((int) (v * 100));
                }

            });
        } else {
            Toast.makeText(this, "Add atleast two videos", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Fragment fragment = new EditFragment();
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.mergelayout, fragment).commit();
                finish();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
