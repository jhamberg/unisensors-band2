package fi.helsinki.cs.unisensors.band2;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.Layout;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

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


        BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> set = a.getBondedDevices();
        BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener(){
            public void onServiceConnected(int profile, BluetoothProfile proxy)
            {
                if (profile == BluetoothProfile.HEADSET)
                {
                    BluetoothHeadset headset = (BluetoothHeadset) proxy;
                    Log.d(TAG, "Found headset");
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    // secret parameters that when added provide audio url in the result
                    intent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR");
                    intent.putExtra("android.speech.extra.GET_AUDIO", true);

                    startActivityForResult(intent, 42);
                }
            }
            public void onServiceDisconnected(int profile)
            {
            }
        };

        IntentFilter newintent = new IntentFilter();
        newintent.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        getBaseContext().registerReceiver(mSCOHeadsetAudioState, newintent);

        // a.getProfileProxy(MainActivity.this, listener, BluetoothProfile.HEADSET);
        AudioManager localAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        localAudioManager.setMode(0);
        localAudioManager.setBluetoothScoOn(true);
        localAudioManager.startBluetoothSco();
        localAudioManager.setMode(AudioManager.MODE_IN_CALL);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // the resulting text is in the getExtras:
        Bundle bundle = data.getExtras();
        ArrayList<String> matches = bundle.getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
        // the recording url is in getData:
        Uri audioUri = data.getData();
        ContentResolver contentResolver = getContentResolver();
        try {
            InputStream filestream = contentResolver.openInputStream(audioUri);
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(filestream));
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // TODO: read audio file from inputstream
    }

    private final BroadcastReceiver mSCOHeadsetAudioState = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
               Log.d(TAG, "Device is ready for recording");
            } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                Log.d(TAG, "Device not ready for recording");
            }
        }
    };

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
        editor.putBoolean("ambient", selection[6]);
        editor.putBoolean("uv", selection[7]);
        editor.putBoolean("skin", selection[8]);
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
        view.ambientBox.setChecked(preferences.getBoolean("ambient", false));
        view.uvBox.setChecked(preferences.getBoolean("uv", false));
        view.skinBox.setChecked(preferences.getBoolean("skin", false));
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
                view.baroBox.isChecked(),
                view.ambientBox.isChecked(),
                view.uvBox.isChecked(),
                view.skinBox.isChecked()
        };
    }


    public void updateButtonState(boolean running){
        view.serviceButton.setText(running ? R.string.stop : R.string.start);
    }

    public boolean isServiceRunning(){
        return Utils.isServiceRunning(getBaseContext(), service);
    }
}
