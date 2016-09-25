package fi.helsinki.cs.unisensors.band2;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.GsrSampleRate;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    private BandClient client = null;
    private BandGsrEventListener gsrListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                Log.d(TAG, "Resistance: " + event.getResistance() + " kOhms");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new GsrSubscriptionTask().execute();
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
