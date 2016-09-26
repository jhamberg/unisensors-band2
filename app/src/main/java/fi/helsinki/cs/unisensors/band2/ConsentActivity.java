package fi.helsinki.cs.unisensors.band2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.lang.ref.WeakReference;

public class ConsentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WeakReference<Activity> reference = new WeakReference<Activity>(this);
        Band.requestHrConsent(getBaseContext(), reference);
    }
}
