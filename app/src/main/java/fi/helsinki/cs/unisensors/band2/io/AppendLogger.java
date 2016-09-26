package fi.helsinki.cs.unisensors.band2.io;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

import fi.helsinki.cs.unisensors.band2.R;

public class AppendLogger {
    private final String TAG = this.getClass().getCanonicalName();
    private final String NEWLINE = System.getProperty("line.separator");
    private Context context;
    private FileOutputStream output;
    private String delimiter;

    public AppendLogger(Context context, String fileName, String delimiter) {
        this.context = context;
        this.delimiter = delimiter;
        output = getAppendStream(fileName);
    }

    public void log(String...fields){
        try {
            String line = constructLine(fields);
            if(line != null && !line.isEmpty()){
                output.write(line.getBytes());
            }
        } catch(Throwable th){
            Log.e(TAG, "Failed writing to log!");
            th.printStackTrace();
        }
    }

    public void finish(){
        try {
            output.close();
        } catch(Throwable th){
            Log.e(TAG, "Failed closing the log output stream!");
            th.printStackTrace();
        }
    }

    private String constructLine(String ...fields){
        String line = "";
        int len = fields.length;
        for(int i=0; i<len; i++){
            line += fields[i];
            line += ((i < len-1) ? delimiter : "");
        }
        line += NEWLINE;
        return line;
    }

    @SuppressWarnings("all")
    private FileOutputStream getAppendStream(String fileName){
        try {
            Long timestamp = System.currentTimeMillis();
            String folderName = context.getString(R.string.app_name).replace(" ", "");
            File extPath = Environment.getExternalStorageDirectory();
            File folder = new File(extPath, folderName);
            if(!folder.exists()){
                folder.mkdirs();
            }
            String name = fileName + "-" + timestamp + ".csv";
            File output = new File(folder, name);
            Log.d(TAG, "Opening file " + output.getAbsolutePath() + " for logging");
            return new FileOutputStream(output, true);
        } catch(Throwable th){
            Log.e(TAG, "Failed to get an output stream!");
            th.printStackTrace();
            return null;
        }
    }
}
