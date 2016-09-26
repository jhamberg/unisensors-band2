package fi.helsinki.cs.unisensors.band2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;

public class BandService extends Service {
    private final String TAG = this.getClass().getSimpleName();
    private final int ID = 42;
    private NotificationManager mNotificationManager;
    private int skinResponse, heartRate;
    private BroadcastReceiver hrConsentReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            boolean consented = extras.getBoolean("consent");
            if(consented){
                Log.d(TAG, "User allowed monitoring Heart Rate");
                Band.registerHrListener(context, hrListener);
            } else {
                Log.d(TAG, "User denied monitoring Heart Rate");
            }
        }
    };

    private BandGsrEventListener gsrListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                skinResponse = event.getResistance();
                updateNotification();
            }
        }
    };
    private BandHeartRateEventListener hrListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                heartRate = event.getHeartRate();
                updateNotification();
            }
        }
    };

    @Override
    public void onCreate() {
        registerReceiver(hrConsentReceiver, new IntentFilter(Band.CONSENT));
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(hrConsentReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public Notification getPersistentServiceNotification(String status){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent activityIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_battery_charging_full_white_24dp)
                .setContentTitle("UniSensors: Band2")
                .setContentText(status)
                .setOngoing(true)
                .setContentIntent(activityIntent);
        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(ID, getPersistentServiceNotification("Initializing.."));
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Context baseContext = getBaseContext();
        Band.registerGsrListener(baseContext, gsrListener);
        Band.registerHrListener(baseContext, hrListener);
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateNotification(){
        String status = "GSR: " + skinResponse + " k\u2126 HR: " + heartRate;
        mNotificationManager.notify(ID, getPersistentServiceNotification(status));
    }

}
