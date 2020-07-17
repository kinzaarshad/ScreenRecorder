package com.virtoxed.screenrecorderlivestreamrecorder;

import android.net.Uri;

import androidx.room.Entity;

import java.io.File;

@Entity(tableName = "images")
public class Spacecraft {
    private String name;
    private Uri uri;
    private File mLocalPath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public File getLocalPath() {
        return mLocalPath;
    }

    public void setLocalPath(File mLocalPath) {
        this.mLocalPath = mLocalPath;
    }


}
