package fi.helsinki.cs.unisensors.band2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    private Class service;
    private Intent serviceIntent;
    private Button serviceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceButton = (Button) findViewById(R.id.serviceButton);

        service = BandService.class;
        serviceIntent = new Intent(MainActivity.this, service);
        updateButtonState();

        serviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean running = isServiceRunning();
                if(running){
                    stopService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                updateButtonState(!running);
            }
        });
    }

    public void updateButtonState(){
        updateButtonState(isServiceRunning());
    }

    public void updateButtonState(boolean running){
        serviceButton.setText(running ? R.string.stop : R.string.start);
    }

    public boolean isServiceRunning(){
        return Utils.isServiceRunning(getBaseContext(), service);
    }
}
