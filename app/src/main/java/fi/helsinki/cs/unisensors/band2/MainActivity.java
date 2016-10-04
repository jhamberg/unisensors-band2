package fi.helsinki.cs.unisensors.band2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private Class service;
    private Intent serviceIntent;
    private Button serviceButton;
    private CheckBox gsrCheckbox, hrCheckbox, rrCheckbox;
    private boolean gsr, hr, rr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceButton = (Button) findViewById(R.id.serviceButton);
        gsrCheckbox = (CheckBox) findViewById(R.id.gsrBox);
        hrCheckbox = (CheckBox) findViewById(R.id.hrBox);
        rrCheckbox = (CheckBox) findViewById(R.id.rrBox);

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
                    updateButtonState(false);
                } else {
                    boolean[] selection = getSensorSelection();
                    for(boolean value : selection){
                        if(value){
                            serviceIntent.putExtra("sensors", selection);
                            startService(serviceIntent);
                            updateButtonState(true);
                        }
                    }
                }
            }
        });
    }

    public boolean[] getSensorSelection(){
        gsr = gsrCheckbox.isChecked();
        hr = hrCheckbox.isChecked();
        rr = rrCheckbox.isChecked();
        return new boolean[]{gsr, hr, rr};
    }

    public void updateButtonState(boolean running){
        serviceButton.setText(running ? R.string.stop : R.string.start);
    }

    public boolean isServiceRunning(){
        return Utils.isServiceRunning(getBaseContext(), service);
    }
}
