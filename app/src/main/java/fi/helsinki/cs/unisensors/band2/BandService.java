package fi.helsinki.cs.unisensors.band2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.GsrSampleRate;

public class BandService extends Service {
    private final String TAG = this.getClass().getSimpleName();
    private final int ID = 42;
    private NotificationManager mNotificationManager;
    private BandClient client = null;
    private BandGsrEventListener gsrListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                updateNotification("GSR: " + event.getResistance() + "kOhm");
            }
        }
    };

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
                .setContentTitle("UniSensors: Band2 Monitor")
                .setContentText(status)
                .setOngoing(true)
                .setContentIntent(activityIntent);
        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(ID, getPersistentServiceNotification("Initializing.."));
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        new GsrSubscriptionTask().execute();
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateNotification(String status){
        mNotificationManager.notify(ID, getPersistentServiceNotification(status));
    }


    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                Log.d(TAG, "Band isn't paired!");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            Log.d(TAG, "Already connected to band!");
            return true;
        }
        Log.d(TAG, "Connecting to band..");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private class GsrSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        Log.d(TAG, "Successfully connected!");
                        client.getSensorManager().registerGsrEventListener(gsrListener, GsrSampleRate.MS200);
                    } else {
                        Log.e(TAG, "GSR cannot be measured on Band 1");
                    }
                } else {
                    Log.e(TAG, "Band not connected or within reach of bluetooth!");
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Unsupported SDK version, please update.";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Incorrect permissions or missing Microsoft Band application.";
                        break;
                    default:
                        exceptionMessage = "Unknown band error: " + e.getMessage();
                        break;
                }
                Log.e(TAG, exceptionMessage);

            } catch (Exception e) {
                Log.e(TAG, "Generic error when subscribing to GSR: " + e.getMessage());
            }
            return null;
        }
    }
}
