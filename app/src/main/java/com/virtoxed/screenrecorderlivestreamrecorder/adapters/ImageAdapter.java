package com.virtoxed.screenrecorderlivestreamrecorder.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.virtoxed.screenrecorderlivestreamrecorder.R;
import com.virtoxed.screenrecorderlivestreamrecorder.Spacecraft;

import java.io.File;
import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {
    Context c;
    ArrayList<Spacecraft> spacecrafts;

    public ImageAdapter(Context c, ArrayList<Spacecraft> spacecrafts) {
        this.c = c;
        this.spacecrafts = spacecrafts;
    }

    @Override
    public int getCount() {
        return spacecrafts.size();
    }


    @Override
    public Object getItem(int position) {
        return spacecrafts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        if(view==null)
        {
            view= LayoutInflater.from(c).inflate(R.layout.image_list_item,parent,false);

        }
        final Spacecraft s=(Spacecraft) this.getItem(position);
        ImageView img=view.findViewById(R.id.images);

        Glide.with(c).load(s.getUri()).into(img);
        String uri= String.valueOf(s.getUri());
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());

                Intent vi = new Intent(Intent.ACTION_VIEW);
                vi.setDataAndType(Uri.parse(uri), "image/*");
                c.startActivity(vi);

            }
        });

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new AlertDialog.Builder(c).setTitle("Delete").setMessage("You Sure! You want to delete this.").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int id) {

                        File file= spacecrafts.remove(position).getLocalPath();
                        file.delete();

                        notifyDataSetChanged();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.dismiss();
                    }
                }).create().show();

                return true;
            }
        });
        return view;
    }
}
