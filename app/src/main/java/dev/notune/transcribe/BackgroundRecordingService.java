package dev.notune.transcribe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * Keeps the microphone open while the voice keyboard records in the
 * background ("Record in background"). Android silences the mic for apps
 * that aren't in the foreground, so without this, everything said after
 * switching to another app is lost. Holding a microphone-type foreground
 * service (with its notification) for the duration of the recording keeps
 * the capture running; the recording itself stays in the keyboard.
 *
 * Must be started while the keyboard is on screen (Android doesn't allow
 * starting a microphone foreground service from the background), so the
 * keyboard starts it together with the recording.
 */
public class BackgroundRecordingService extends Service {
    private static final String TAG = "BgRecordingService";
    private static final String CHANNEL_ID = "background_recording";
    private static final int NOTIFICATION_ID = 2;

    /** Starts the service; returns false if Android refused. */
    static boolean start(Context context) {
        try {
            context.startForegroundService(new Intent(context, BackgroundRecordingService.class));
            return true;
        } catch (RuntimeException e) {
            // e.g. ForegroundServiceStartNotAllowedException: recording still
            // works while the keyboard stays on screen.
            Log.w(TAG, "Could not start background recording service", e);
            return false;
        }
    }

    static void stop(Context context) {
        context.stopService(new Intent(context, BackgroundRecordingService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            // The microphone type exists from Android 11, which is also when
            // background mic capture started to need it.
            if (Build.VERSION.SDK_INT >= 30) {
                startForeground(NOTIFICATION_ID, createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "startForeground refused", e);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private Notification createNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    getString(R.string.bg_recording_channel), NotificationManager.IMPORTANCE_LOW));
        }
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.bg_recording_title))
                .setContentText(getString(R.string.bg_recording_text))
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
