package com.virtoxed.screenrecorderlivestreamrecorder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import static android.content.Context.NOTIFICATION_SERVICE;

public class MyBroadcastReceiver extends BroadcastReceiver {
    public static final String something = "OK";
    static final String EXTRA_NOTIFICATION_ID = "notification-id";

    private static final String TAG = "receiver";
    @Override
    public void onReceive(Context context, Intent intent) {


            Toast.makeText(context, "Record Clicked", Toast.LENGTH_SHORT).show();
            NotificationManager notificationmanager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            notificationmanager.cancelAll();


    }
}
