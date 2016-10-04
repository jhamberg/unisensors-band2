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
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.HeartRateQuality;

import java.util.ArrayList;
import java.util.Locale;

import fi.helsinki.cs.unisensors.band2.io.AppendLogger;

public class BandService extends Service {
    private final String TAG = this.getClass().getSimpleName();
    private final String DELIM = ";";
    private final int ID = 32478611;
    private NotificationManager mNotificationManager;
    private AppendLogger mGsrLogger, mHrLogger, mRrLogger;
    private int skinResponse, heartRate, heartRateQuality;
    private double rrInterval;
    private boolean gsr = false;
    private boolean hr = false;
    private boolean rr = false;

    private BroadcastReceiver hrConsentReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            boolean consented = extras.getBoolean("consent");
            if(consented){
                Log.d(TAG, "User allowed monitoring Heart Rate");
                Band.registerHrListener(context, hrListener);
                Band.registerRriListener(context, rriListener);
            } else {
                Log.d(TAG, "User denied monitoring Heart Rate");
            }
        }
    };

    private BandGsrEventListener gsrListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                String t = System.currentTimeMillis()+"";
                skinResponse = event.getResistance();
                mGsrLogger.log(t, skinResponse+"");
                updateStatus();
            }
        }
    };
    private BandHeartRateEventListener hrListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                String t = System.currentTimeMillis()+"";
                int quality = 0;
                HeartRateQuality hrQuality = event.getQuality();
                if(hrQuality == HeartRateQuality.LOCKED){
                    quality = 1;
                }
                heartRate = event.getHeartRate();
                mHrLogger.log(t, heartRate+"", quality+"");
                updateStatus();
            }
        }
    };

    private BandRRIntervalEventListener rriListener = new BandRRIntervalEventListener() {
        @Override
        public void onBandRRIntervalChanged(final BandRRIntervalEvent event) {
            if (event != null) {
                String t = System.currentTimeMillis()+ "";
                rrInterval = event.getInterval();
                mRrLogger.log(t, rrInterval+"");
                updateStatus();
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
        Band.disconnect();
        unregisterReceiver(hrConsentReceiver);
        mNotificationManager.cancel(ID);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(ID, getPersistentServiceNotification("Initializing.."));
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Context baseContext = getBaseContext();
        if(intent.hasExtra("sensors")){
            boolean[] selection = intent.getBooleanArrayExtra("sensors");
            gsr = selection[0];
            hr = selection[1];
            rr = selection[2];
            Log.d(TAG, "Gsr: " + gsr + " Hr: " + hr + " Rr: " + rr);
            long t = System.currentTimeMillis();
            if(gsr){
                Band.registerGsrListener(baseContext, gsrListener);
                mGsrLogger = new AppendLogger(getBaseContext(), "gsr", t, DELIM);
            }
            if(hr){
                Band.registerHrListener(baseContext, hrListener);
                mHrLogger = new AppendLogger(getBaseContext(), "hr", t, DELIM);
            }
            if(rr){
                Band.registerRriListener(baseContext, rriListener);
                mRrLogger = new AppendLogger(getBaseContext(), "rr", t, DELIM);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public Notification getPersistentServiceNotification(String status){
        Context appContext = getApplicationContext();
        Intent notificationIntent = new Intent(appContext, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent activityIntent = PendingIntent.getActivity(appContext, 0, notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext)
                .setSmallIcon(R.drawable.ic_watch_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(status)
                .setOngoing(true)
                .setContentIntent(activityIntent);
        return builder.build();
    }

    private void updateStatus(){
        String status =
                (gsr ? "GSR: " + skinResponse + " k\u2126 ": "") +
                (hr ? "HR: " + heartRate + " ": "") +
                (rr ? String.format(Locale.US, "RR: %.2f", rrInterval) : "");
        mNotificationManager.notify(ID, getPersistentServiceNotification(status));
    }

}
