package com.virtoxed.screenrecorderlivestreamrecorder.fragments;


import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;


import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.GridView;

import android.widget.TextView;


import com.virtoxed.screenrecorderlivestreamrecorder.R;
import com.virtoxed.screenrecorderlivestreamrecorder.Spacecraft;
import com.virtoxed.screenrecorderlivestreamrecorder.adapters.ImageAdapter;


import java.io.File;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImageFragment extends Fragment {
    GridView lv;
    TextView tv;
    ImageAdapter mAdapter;

    public ImageFragment() {
        // Required empty public constructor
    }
    public static ImageFragment getInstance()    {
        return new ImageFragment();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View rootView= inflater.inflate(R.layout.fragment_image, container, false);
        lv=rootView.findViewById(R.id.list_images);
        tv=rootView.findViewById(R.id.tvEmpty);
        mAdapter=new ImageAdapter(getContext(),getData());
        lv.setAdapter(mAdapter);


        return rootView;

    }

    private ArrayList<Spacecraft> getData()
    {
        ArrayList<Spacecraft> spacecrafts=new ArrayList<>();
        File targetfolder= new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES)+ "/Screen Recorder Live Stream Recorder");

        Spacecraft s;
        if(!targetfolder.exists())
        {
            targetfolder.mkdirs();
            File[] files= targetfolder.listFiles();
            if(files!=null) {
                tv.setVisibility(View.INVISIBLE);
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    s = new Spacecraft();
                    s.setName(file.getName());
                    s.setUri(Uri.fromFile(file));
                    s.setLocalPath(file);

                    spacecrafts.add(s);

                }
            }
            else
            {
                tv.setVisibility(View.VISIBLE);
            }
        }
        else if(targetfolder.exists()) {
            File[] files = targetfolder.listFiles();
            if(files!=null) {
                tv.setVisibility(View.INVISIBLE);
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    s = new Spacecraft();
                    s.setName(file.getName());
                    s.setUri(Uri.fromFile(file));
                    s.setLocalPath(file);

                    spacecrafts.add(s);

                }
            }
            else {
                tv.setVisibility(View.VISIBLE);
            }
        }
        return spacecrafts;
    }


}
