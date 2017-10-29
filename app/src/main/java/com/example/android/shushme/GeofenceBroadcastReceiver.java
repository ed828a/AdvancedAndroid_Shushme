package com.example.android.shushme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * Created by Edward on 10/29/2017.
 */

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = GeofenceBroadcastReceiver.class.getSimpleName();
    private static final int NOTIFICATION_REQUEST_CODE = 0;
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(LOG_TAG, "OnReceive called");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        // get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            setRingerMode(context, AudioManager.RINGER_MODE_SILENT);
        }
        switch (geofenceTransition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                setRingerMode(context, AudioManager.RINGER_MODE_SILENT);
                sendNotification(context, geofenceTransition);
                break;

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                setRingerMode(context, AudioManager.RINGER_MODE_NORMAL);
                sendNotification(context, geofenceTransition);
                break;

            default:
                Log.e(LOG_TAG, String.format("Unknown transition: %d", geofenceTransition));
        }

    }

    private void setRingerMode(Context context, int mode) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // check for DND permissions for API 24+
        if (Build.VERSION.SDK_INT < 24 ||
                (Build.VERSION.SDK_INT >= 24 &&
                        !notificationManager.isNotificationPolicyAccessGranted())) {
            AudioManager audioManager =
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setRingerMode(mode);
        }
    }

    private void sendNotification(Context context, int transitionType){
        // create an explicit content Intent that starts the main activity.
        Intent notificationIntent = new Intent(context, MainActivity.class);
        // construct  a task stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // add the Main Activity to the task stack as the parent
        stackBuilder.addParentStack(MainActivity.class);
        // push the content Intent onto the stack
        stackBuilder.addNextIntent(notificationIntent);

        // get a PendingIntent containing the entire back stack
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(NOTIFICATION_REQUEST_CODE,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        // get a notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER){
            builder.setSmallIcon(R.drawable.ic_volume_off_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume_off_white_24dp))
                    .setContentTitle(context.getString(R.string.silent_mode_activated));
        } else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT){
            builder.setSmallIcon(R.drawable.ic_volume_up_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume_up_white_24dp))
                    .setContentTitle(context.getString(R.string.back_to_normal));
        }
        // continue building the notification
        builder.setContentText(context.getString(R.string.touch_to_relaunch));
        builder.setContentIntent(notificationPendingIntent);
        // dismiss notification once the user touches it
        builder.setAutoCancel(true);

        Notification notification = builder.build();

        // get a Notification Manager instance
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // issue the notification
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
