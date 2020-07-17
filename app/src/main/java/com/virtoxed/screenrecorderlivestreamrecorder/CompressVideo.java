package com.virtoxed.screenrecorderlivestreamrecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.virtoxed.screenrecorderlivestreamrecorder.fragments.EditFragment;

import java.io.File;

public class CompressVideo extends AppCompatActivity {

    int f = 0;

    static ImageView add;
    static TextView per , view , filename,selecttext;


    static public void updatepro(int n){

        per.setText(n+" %");

    }


     public static void showfile(){


        per.setVisibility(View.GONE);
         selecttext.setVisibility(View.VISIBLE);
        view.setVisibility(View.VISIBLE);





    }



    @Override
    protected void onResume() {
        super.onResume();

        String m = SharedPrefUtil.getInstance(CompressVideo.this).getString("1");

        if(m!=null){

            if(m.equalsIgnoreCase("0")){

                per.setVisibility(View.VISIBLE);

            }
            else{

                per.setVisibility(View.GONE);
                view.setVisibility(View.GONE);

            }

        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compress_video);
        Window window = this.getWindow();

// clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

// add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

// finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.tabcolor));
        getSupportActionBar().setTitle("Compress Video");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        add = (ImageView) findViewById(R.id.add_video);
        per = (TextView) findViewById(R.id.per);
        view = (TextView) findViewById(R.id.view_file);
        filename = (TextView) findViewById(R.id.filename);
        selecttext = (TextView) findViewById(R.id.selecttext);

        per.setVisibility(View.INVISIBLE);
        view.setVisibility(View.INVISIBLE);


        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                per.setVisibility(View.INVISIBLE);
view.setVisibility(View.INVISIBLE);
                selecttext.setVisibility(View.INVISIBLE);
                if (ContextCompat.checkSelfPermission(CompressVideo.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(CompressVideo.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE },1);
                    }

                }
                else {


                    String s = SharedPrefUtil.getInstance(CompressVideo.this).getString("1");
                    if (s.isEmpty()) {
                        SharedPrefUtil.getInstance(CompressVideo.this).putString("1", "0");
                        SharedPrefUtil.getInstance(CompressVideo.this).putString("stop", "0");

                        Intent pickIntent = new Intent(Intent.ACTION_PICK);
                        pickIntent.setType("video/*");
                        startActivityForResult(pickIntent, 1);

                    } else {

                        if (s.equalsIgnoreCase("1")) {
                            SharedPrefUtil.getInstance(CompressVideo.this).putString("1", "0");
                            SharedPrefUtil.getInstance(CompressVideo.this).putString("stop", "0");

                            Intent pickIntent = new Intent(Intent.ACTION_PICK);
                            pickIntent.setType("video/*");
                            startActivityForResult(pickIntent, 1);


                        } else {
                            // Stop

                            SharedPrefUtil.getInstance(CompressVideo.this).putString("stop", "1");
                           // filename.setVisibility(View.INVISIBLE);
                            per.setVisibility(View.INVISIBLE);
                            view.setVisibility(View.INVISIBLE);
                            SharedPrefUtil.getInstance(CompressVideo.this).putString("1", "1");



                        }


                    }

                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ( resultCode==RESULT_OK && data !=null && data.getData() !=null || requestCode == 1) {

            String state = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(state)) {
                // Can read and write the media
                Toast.makeText(CompressVideo.this, "Sorry some problem to phone storage ", Toast.LENGTH_SHORT).show();
                return;
            }

            File direct = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES) + "/Screen Recorder Live Stream Recorder");

            String  local_address = direct+"/"+"CompressedVideo-" +
                    Long.toHexString(System.currentTimeMillis())+".mp4";
            File ff = new File(local_address);

            if(ff.exists()){
                ff.delete();
            }

            VideoCompressAsyncTask videocompress = new VideoCompressAsyncTask(this);
            videocompress.execute(  getVideopath(data.getData(), CompressVideo.this),  local_address  );

            per.setVisibility(View.VISIBLE);
            per.setText("0%");



        }
        else {

            SharedPrefUtil.getInstance(CompressVideo.this).putString("stop","1");

            per.setVisibility(View.INVISIBLE);
            view.setVisibility(View.INVISIBLE);
            SharedPrefUtil.getInstance(CompressVideo.this).putString("1","1");

        }

    }

    public  String getVideopath(Uri uri , Context cnt ) {
        String[] filePathColumn = {MediaStore.Video.Media.DATA};
        Cursor cursor = cnt.getContentResolver().query(uri, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        return picturePath;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
        Fragment fragment = new EditFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.compresslayout, fragment).commit();
        finish();

        return true;
    }
        return super.onOptionsItemSelected(item);
    }
}
