package com.virtoxed.screenrecorderlivestreamrecorder;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class WritetoExternalStorage {



    public static String getVideopath(Uri uri , Context cnt ) {


        String[] filePathColumn = {MediaStore.Video.Media.DATA};
        Cursor cursor = cnt.getContentResolver().query(uri, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();

        return picturePath;
    }


}
