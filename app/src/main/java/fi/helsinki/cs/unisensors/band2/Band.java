package fi.helsinki.cs.unisensors.band2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.GsrSampleRate;
import com.microsoft.band.sensors.HeartRateConsentListener;

import java.lang.ref.WeakReference;

public class Band {
    public final static String CONSENT = "fi.helsinki.cs.unisensors.band2.CONSENT";
    private static String TAG = Band.class.getSimpleName();
    private static BandClient client;
    private final static String ERR_NOT_CONNECTED = "Band not connected or within reach of bluetooth!";

    public static void registerGsrListener(Context context, BandGsrEventListener listener){
        new GsrSubscriptionTask(context, listener).execute();
    }

    public static void registerHrListener(Context context, BandHeartRateEventListener listener){
        new HrSubscriptionTask(context, listener).execute();
    }

    public static void registerRriListener(Context context, BandRRIntervalEventListener listener){
        new RriSubscriptionTask(context, listener).execute();
    }

    @SuppressWarnings("unchecked")
    public static void requestHrConsent(Context context, TaskCallback callback, WeakReference<Activity> reference){
        new HrConsentTask(context, callback).execute(reference);
    }

    public static void disconnect(){
        if (client != null) {
            try {
                client.getSensorManager().unregisterAllListeners();
                client.disconnect().await();
            } catch (InterruptedException | BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
    }

    private static void logBandException(BandException e){
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
    }

    private static boolean getConnectedBandClient(Context context) throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                Log.d(TAG, "Band isn't paired!");
                return false;
            }
            client = BandClientManager.getInstance().create(context, devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }
        Log.d(TAG, "Connecting to band..");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private static class GsrSubscriptionTask extends AsyncTask<Void, Void, Void> {
        private Context context;
        private BandGsrEventListener listener;

        GsrSubscriptionTask(Context context, BandGsrEventListener listener){
            this.context = context;
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient(context)) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        Log.d(TAG, "Successfully connected!");
                        client.getSensorManager().registerGsrEventListener(listener, GsrSampleRate.MS200);
                    } else {
                        Log.e(TAG, "GSR cannot be measured on Band 1");
                    }
                } else {
                    Log.e(TAG, ERR_NOT_CONNECTED);
                }
            } catch (BandException e) {
                logBandException(e);
            } catch (Exception e) {
                Log.e(TAG, "Generic error when subscribing to GSR: " + e.getMessage());
            }
            return null;
        }
    }

    private static class HrSubscriptionTask extends AsyncTask<Void, Void, Void> {
        private Context context;
        private BandHeartRateEventListener listener;
        private boolean consented;

        HrSubscriptionTask(Context context, BandHeartRateEventListener listener){
            this.context = context;
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient(context)) {
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        consented = true;
                        client.getSensorManager().registerHeartRateEventListener(listener);
                    } else {
                        consented = false;
                        Log.e(TAG, "Heart rate permission missing!");
                    }
                } else {
                    Log.e(TAG, ERR_NOT_CONNECTED);
                }
            } catch (BandException e) {
                logBandException(e);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(!consented){
                Intent consentIntent = new Intent(context, ConsentActivity.class);
                consentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(consentIntent);
            }
            super.onPostExecute(aVoid);
        }
    }

    private static class RriSubscriptionTask extends AsyncTask<Void, Void, Void> {
        private Context context;
        private BandRRIntervalEventListener listener;
        private boolean consented;

        RriSubscriptionTask(Context context, BandRRIntervalEventListener listener){
            this.context = context;
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient(context)) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                            client.getSensorManager().registerRRIntervalEventListener(listener);
                            consented = true;
                        } else {
                           consented = false;
                        }
                    } else {
                        Log.e(TAG, "RR interval cannot be measured on Band 1");
                    }
                } else {
                    Log.e(TAG, ERR_NOT_CONNECTED);
                }
            } catch (BandException e) {
                logBandException(e);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(!consented){
                Intent consentIntent = new Intent(context, ConsentActivity.class);
                consentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(consentIntent);
            }
            super.onPostExecute(aVoid);
        }
    }

    private static class HrConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        private Context context;
        private TaskCallback callback;

        HrConsentTask(Context context, TaskCallback callback){
            this.context = context;
            this.callback = callback;
        }

        @SafeVarargs
        @Override
        protected final Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient(context)) {
                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                                Intent consent = new Intent(Band.CONSENT);
                                consent.putExtra("consent", consentGiven);
                                context.sendBroadcast(consent);
                                callback.signalFinished();
                            }
                        });
                    }
                } else {
                    Log.e(TAG, ERR_NOT_CONNECTED);
                }
            } catch (BandException e) {
                logBandException(e);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }
    }
}
