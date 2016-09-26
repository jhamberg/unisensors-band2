package fi.helsinki.cs.unisensors.band2;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.lang.ref.WeakReference;


public class ConsentActivity extends AppCompatActivity implements TaskCallback {
    private final String TAG = ConsentActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WeakReference<Activity> reference = new WeakReference<Activity>(this);
        Band.requestHrConsent(getBaseContext(), this, reference);
    }

    @Override
    public void signalFinished() {
        Log.d(TAG, "Finishing consent activity");
        finish();
    }
}
