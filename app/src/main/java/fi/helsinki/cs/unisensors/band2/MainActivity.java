package fi.helsinki.cs.unisensors.band2;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;

import java.util.HashMap;

import fi.helsinki.cs.unisensors.band2.databinding.ActivityMainBinding;
import fi.helsinki.cs.unisensors.band2.databinding.InputDialogBinding;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    private ActivityMainBinding view;
    private SharedPreferences preferences;
    private Class service;
    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        preferences =  PreferenceManager.getDefaultSharedPreferences(this);
        view = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initLayout();

        service = BandService.class;
        serviceIntent = new Intent(MainActivity.this, service);
        boolean running = isServiceRunning();
        updateButtonState(running);

        view.serviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean running = isServiceRunning();
                if(running){
                    stopService(serviceIntent);
                    updateButtonState(false);
                } else {
                    showSessionNameDialog();
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
        editor.putBoolean("gyro", selection[3]);
        editor.putBoolean("acc", selection[4]);
        editor.putBoolean("baro", selection[5]);
        editor.apply();
    }

    public void startService(String session){
        boolean[] selection = getSensorSelection();
        for(boolean value : selection){
            // Only start if at least one sensor is selected
            if(value){
                serviceIntent.putExtra("sensors", selection);
                serviceIntent.putExtra("session", session);
                startService(serviceIntent);
                updateButtonState(true);
                break;
            }
        }
    }

    public void initLayout(){
        view.gsrBox.setChecked(preferences.getBoolean("gsr", false));
        view.hrBox.setChecked(preferences.getBoolean("hr", false));
        view.rrBox.setChecked(preferences.getBoolean("rr", false));
        view.gyroBox.setChecked(preferences.getBoolean("gyro", false));
        view.accBox.setChecked(preferences.getBoolean("acc", false));
        view.baroBox.setChecked(preferences.getBoolean("baro", false));
    }

    public void showSessionNameDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Session name");
        LayoutInflater inflater = LayoutInflater.from(getBaseContext());
        final InputDialogBinding dialogView = DataBindingUtil.inflate(inflater,
                R.layout.input_dialog, null, false);
        builder.setView(dialogView
                .getRoot());
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String session = dialogView.filenameInput.getText().toString();
                startService(session);
                dialog.cancel();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public boolean[] getSensorSelection(){
        return new boolean[]{
                view.gsrBox.isChecked(),
                view.hrBox.isChecked(),
                view.rrBox.isChecked(),
                view.gyroBox.isChecked(),
                view.accBox.isChecked(),
                view.baroBox.isChecked()};
    }


    public void updateButtonState(boolean running){
        view.serviceButton.setText(running ? R.string.stop : R.string.start);
    }

    public boolean isServiceRunning(){
        return Utils.isServiceRunning(getBaseContext(), service);
    }
}
