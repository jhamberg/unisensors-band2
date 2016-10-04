package fi.helsinki.cs.unisensors.band2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    private SharedPreferences preferences;
    private Class service;
    private Intent serviceIntent;
    private Button serviceButton;
    private CheckBox gsrCheckbox, hrCheckbox, rrCheckbox;
    private boolean gsr, hr, rr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        preferences =  PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_main);
        initLayout();

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
                        // Only start if at least one sensor is selected
                        if(value){
                            serviceIntent.putExtra("sensors", selection);
                            startService(serviceIntent);
                            updateButtonState(true);
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        boolean[] selection = getSensorSelection();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("gsr", selection[0]);
        editor.putBoolean("hr", selection[1]);
        editor.putBoolean("rr", selection[2]);
        editor.apply();
    }

    public void initLayout(){
        serviceButton = (Button) findViewById(R.id.serviceButton);
        gsrCheckbox = (CheckBox) findViewById(R.id.gsrBox);
        hrCheckbox = (CheckBox) findViewById(R.id.hrBox);
        rrCheckbox = (CheckBox) findViewById(R.id.rrBox);

        gsrCheckbox.setChecked(preferences.getBoolean("gsr", false));
        hrCheckbox.setChecked(preferences.getBoolean("hr", false));
        rrCheckbox.setChecked(preferences.getBoolean("rr", false));
    }

    public boolean[] getSensorSelection(){
        return new boolean[]{
                gsrCheckbox.isChecked(),
                hrCheckbox.isChecked(),
                rrCheckbox.isChecked()};
    }

    public void updateButtonState(boolean running){
        serviceButton.setText(running ? R.string.stop : R.string.start);
    }

    public boolean isServiceRunning(){
        return Utils.isServiceRunning(getBaseContext(), service);
    }
}
