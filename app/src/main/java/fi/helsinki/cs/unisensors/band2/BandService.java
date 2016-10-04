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

import java.util.ArrayList;
import java.util.Locale;

import fi.helsinki.cs.unisensors.band2.io.AppendLogger;

public class BandService extends Service {
    private final String TAG = this.getClass().getSimpleName();
    private final int ID = 32478611;
    private NotificationManager mNotificationManager;
    private AppendLogger mAppendLogger;
    private int skinResponse, heartRate;
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
                skinResponse = event.getResistance();
                registerEvent();
            }
        }
    };
    private BandHeartRateEventListener hrListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                heartRate = event.getHeartRate();
                registerEvent();
            }
        }
    };

    private BandRRIntervalEventListener rriListener = new BandRRIntervalEventListener() {
        @Override
        public void onBandRRIntervalChanged(final BandRRIntervalEvent event) {
            if (event != null) {
                rrInterval = event.getInterval();
                registerEvent();
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
        mAppendLogger.finish();
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
            if(gsr) Band.registerGsrListener(baseContext, gsrListener);
            if(hr)  Band.registerHrListener(baseContext, hrListener);
            if(rr)  Band.registerRriListener(baseContext, rriListener);
        }
        String filename = constructFileName();
        mAppendLogger = new AppendLogger(getBaseContext(), filename, ";");
        return super.onStartCommand(intent, flags, startId);
    }

    public Notification getPersistentServiceNotification(String status){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent activityIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_watch_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(status)
                .setOngoing(true)
                .setContentIntent(activityIntent);
        return builder.build();
    }

    private void registerEvent(){
        ArrayList<String> toLog = new ArrayList<>();
        toLog.add(System.currentTimeMillis() + "");
        if(gsr) toLog.add(skinResponse + "");
        if(hr) toLog.add(heartRate + "");
        if(rr) toLog.add(rrInterval + "");
        mAppendLogger.log(toLog.toArray(new String[toLog.size()]));
        updateStatus();
    }

    private String constructFileName(){
        String filename = "";
        if(gsr) filename += "gsr-";
        if(hr) filename += "hr-";
        if(rr) filename += "rr-";

        // Remove last hyphen
        return filename.substring(0,filename.length()-1);
    }


    private void updateStatus(){
        String status =
                (gsr ? "GSR: " + skinResponse + " k\u2126 ": "") +
                (hr ? "HR: " + heartRate + " ": "") +
                (rr ? String.format(Locale.US, "RR: %.2f", rrInterval) : "");
        mNotificationManager.notify(ID, getPersistentServiceNotification(status));
    }

}
