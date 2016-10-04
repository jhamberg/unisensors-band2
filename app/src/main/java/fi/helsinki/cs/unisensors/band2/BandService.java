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

import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.HeartRateQuality;

import java.util.Locale;

import fi.helsinki.cs.unisensors.band2.io.AppendLogger;

@SuppressWarnings("FieldCanBeLocal")
public class BandService extends Service {
    private final String TAG = this.getClass().getSimpleName();
    private final String DELIM = ";";
    private final int ID = 32478611;
    private NotificationManager mNotificationManager;
    private int skinResponse, heartRate;
    private AppendLogger mGsrLogger, mHrLogger, mRrLogger, mGyroLogger, mAccLogger;
    private float accX, accY, accZ;
    private float gyroaccX, gryoaccY, gryoaccZ, angvelX, angvelY, angvelZ;
    private double rrInterval;
    private boolean gsr = false;
    private boolean hr = false;
    private boolean rr = false;
    private boolean gyro = false;
    private boolean acc = false;

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

    private BandGyroscopeEventListener gyroListener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(BandGyroscopeEvent event) {
            if(event != null){
                String t = System.currentTimeMillis() + "";
                gyroaccX = event.getAccelerationX();
                gryoaccY = event.getAccelerationY();
                gryoaccZ = event.getAccelerationZ();
                angvelX = event.getAngularVelocityX();
                angvelY = event.getAngularVelocityY();
                angvelZ = event.getAngularVelocityZ();
                mGyroLogger.log(t, gyroaccX +"", gryoaccY +"", gryoaccZ +"",
                                angvelX+"", angvelY +"", angvelZ +"");
            }
        }
    };

    private BandAccelerometerEventListener accListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(BandAccelerometerEvent event) {
            if(event != null){
                String t = System.currentTimeMillis() + "";
                accX = event.getAccelerationX();
                accY = event.getAccelerationY();
                accZ = event.getAccelerationZ();
                mAccLogger.log(t, accX+"", accY+"", accZ+"");
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
        if(intent.hasExtra("sensors")){
            boolean[] selection = intent.getBooleanArrayExtra("sensors");
            registerListeners(selection);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void registerListeners(boolean[] selection){
        Context baseContext = getBaseContext();
        gsr = selection[0];
        hr = selection[1];
        rr = selection[2];
        gyro = selection[3];
        acc = selection[4];

        long t = System.currentTimeMillis();
        if(gsr){
            Band.registerGsrListener(baseContext, gsrListener);
            mGsrLogger = new AppendLogger(getBaseContext(), "gsr", t, DELIM);
        } if(hr){
            Band.registerHrListener(baseContext, hrListener);
            mHrLogger = new AppendLogger(getBaseContext(), "hr", t, DELIM);
        } if(rr){
            Band.registerRriListener(baseContext, rriListener);
            mRrLogger = new AppendLogger(getBaseContext(), "rr", t, DELIM);
        } if(gyro){
            Band.registerGyroListener(baseContext, gyroListener);
            mGyroLogger = new AppendLogger(getBaseContext(), "gyro", t, DELIM);
        } if(acc){
            Band.registerAccListener(baseContext, accListener);
            mAccLogger = new AppendLogger(getBaseContext(), "acc", t, DELIM);
        }

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
                (rr ? String.format(Locale.US, "RR: %.2f ", rrInterval) : "");
        mNotificationManager.notify(ID, getPersistentServiceNotification(status));
    }

}
