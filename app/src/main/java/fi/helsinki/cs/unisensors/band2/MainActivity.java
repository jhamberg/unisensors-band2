package fi.helsinki.cs.unisensors.band2;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    private HashMap<String, Boolean> choices;

    private Class service;
    private Intent serviceIntent;
    private Button serviceButton;
    private CheckBox gsr, hr, rr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceButton = (Button) findViewById(R.id.serviceButton);
        gsr = (CheckBox) findViewById(R.id.gsrBox);
        hr = (CheckBox) findViewById(R.id.hrBox);
        rr = (CheckBox) findViewById(R.id.rrBox);

        service = BandService.class;
        serviceIntent = new Intent(MainActivity.this, service);
        boolean running = isServiceRunning();
        updateButtonState(running);

        serviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean running = isServiceRunning();
                if(running){
                    stopService(serviceIntent);
                } else {
                    updateChoices();
                    startService(serviceIntent);
                }
                updateButtonState(!running);
            }
        });
    }

    public void updateChoices(){

    }

    public void updateButtonState(boolean running){
        serviceButton.setText(running ? R.string.stop : R.string.start);
    }

    public boolean isServiceRunning(){
        return Utils.isServiceRunning(getBaseContext(), service);
    }
}
