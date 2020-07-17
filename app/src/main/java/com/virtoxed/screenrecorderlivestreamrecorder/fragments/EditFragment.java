package com.virtoxed.screenrecorderlivestreamrecorder.fragments;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.virtoxed.screenrecorderlivestreamrecorder.CompressVideo;
import com.virtoxed.screenrecorderlivestreamrecorder.CroppedImage;
import com.virtoxed.screenrecorderlivestreamrecorder.MergeVideos;
import com.virtoxed.screenrecorderlivestreamrecorder.R;
import com.virtoxed.screenrecorderlivestreamrecorder.WritetoExternalStorage;
import com.virtoxed.screenrecorderlivestreamrecorder.trim_video;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 */
public class EditFragment extends Fragment {
Uri mImageUri;
ImageView cropimg,trimvid,compressvid,mergevid;
    public EditFragment() {
        // Required empty public constructor
    }
    public static EditFragment getInstance()    {
        return new EditFragment();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View rootView= inflater.inflate(R.layout.fragment_edit, container, false);
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE },1);
            }

        }
        cropimg=rootView.findViewById(R.id.cropimg);
        cropimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChooseFile(v);
            }
        });

        trimvid=rootView.findViewById(R.id.trimvid);
        trimvid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK);
                pickIntent.setType("video/*");
                startActivityForResult(pickIntent, 1);
            }
        });

        compressvid=rootView.findViewById(R.id.compressvid);
        compressvid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getContext(), CompressVideo.class);
                startActivity(intent);
            }
        });

        mergevid=rootView.findViewById(R.id.mergevid);
        mergevid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getContext(), MergeVideos.class);
                startActivity(intent);
            }
        });
        return rootView;
    }

    public void onChooseFile(View view)
    {
        CropImage.activity().start(getContext(),this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result=CropImage.getActivityResult(data);
            if(resultCode==RESULT_OK)
            {
              String img= String.valueOf(mImageUri=result.getUri());
                Intent intent=new Intent(getContext(), CroppedImage.class);
                intent.putExtra("img",img);
                startActivity(intent);


            }
            else if(resultCode==CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
            {
                Exception e=result.getError();
                Toast.makeText(getContext(), "Possible Error is :"+e, Toast.LENGTH_SHORT).show();
            }

        }
        if ( resultCode==RESULT_OK && data != null && data.getData() !=null && requestCode == 1 ) {


            Uri uri =  data.getData();
            String videopath = WritetoExternalStorage.getVideopath( uri  , getContext());

            trim_video.addpath( videopath );

            Intent i = new Intent( getContext() , trim_video.class );

            startActivity( i );



        }

        else if (resultCode == RESULT_CANCELED) {

            Toast.makeText(getContext() ,"Video Selected Canceled ", Toast.LENGTH_SHORT).show();

        }
    }
}
