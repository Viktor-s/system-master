package me.up.ctd;

import java.io.IOException;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.support.v4.app.FragmentActivity;
import android.widget.RelativeLayout;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.util.Log;
import java.util.Random;
import android.os.Handler;
import java.util.Timer;
import java.util.TimerTask;

public class RecordAudio extends Fragment {

    private MediaRecorder myRecorder;
    private MediaPlayer myPlayer;
    private String outputFile = null;
    private RelativeLayout llLayout = null;
    private FragmentActivity faActivity = null;
    private ProgressDialog dialog = null;
    private long timestamp;
    private String UploadServerUri = MyActivity.base_url+"/client/php/upload_media_message.php";
    int serverResponseCode = 0;
    public static RecordAudio record_audio;
    public static boolean running = false;
    private String FileName;
    public static boolean allow_record = false;
    private Timer timer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                      Bundle savedInstanceState) {
        faActivity  = (FragmentActivity)    super.getActivity();
        llLayout    = (RelativeLayout)    inflater.inflate(R.layout.activity_record_audio, container, false);
        return llLayout;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (allow_record) {
            running = true;
            record_audio = this;
            // store it to sd card
            timestamp = System.currentTimeMillis();
            FileName = "audio_message"+timestamp+".3gpp";
            outputFile = Environment.getExternalStorageDirectory().
                    getAbsolutePath() + "/" + FileName;

            myRecorder = new MediaRecorder();
            myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            myRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            myRecorder.setOutputFile(outputFile);
            start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stop();
    }

    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int len = 7;
        char tempChar;
        for (int i = 0; i < len; i++){
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    public void start(){
        try {
            myRecorder.prepare();
            myRecorder.start();
            start_logging();
        } catch (IllegalStateException e) {
            // start:it is called before prepare()
            // prepare: it is called after start() or before setOutputFormat()
            e.printStackTrace();
        } catch (IOException e) {
            // prepare() fails
            e.printStackTrace();
        }
    }

    public void start_logging() {
        timer = new Timer();
        timer.scheduleAtFixedRate( new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        try{
                            WebAppInterface.wv_handler.track_audio_record_level(myRecorder.getMaxAmplitude());
                        }
                        catch (Exception e) {
                            // TODO: handle exception
                        }
                    }
                });
            }
        }, 0, 100);
    }

    public void stop_logging() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void stop(){
        try {
            if (myRecorder != null) {
                myRecorder.stop();
                myRecorder.release();
                myRecorder  = null;
            }
            stop_logging();
            dialog = ProgressDialog.show(this.getActivity(), "", "Uploading file...", true);
            new Thread(new Runnable() {
                public void run() {
                    uploadFile(outputFile);
                }
            }).start();
        } catch (IllegalStateException e) {
            //  it is called before start()
            e.printStackTrace();
        } catch (RuntimeException e) {
            // no valid audio/video data has been received
            e.printStackTrace();
        }
    }

    public int uploadFile(String sourceFileUri) {

        Log.i("File", sourceFileUri);

        final String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {
            dialog.dismiss();
            Log.e("uploadFile", "Source File not exist :"
                    +outputFile);
            return 0;
        }
        else
        {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(UploadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + fileName + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);
                if(serverResponseCode == 200){
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            File to_delete = new File(fileName);
                            to_delete.delete();
                            WebAppInterface.wv_handler.send_audio_message(FileName);
                            MyActivity.MyActivity_pointer.destroy_audio_record();
                        }
                    });
                }
                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {
                dialog.dismiss();
                ex.printStackTrace();
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
                MyActivity.MyActivity_pointer.destroy_audio_record();
            } catch (Exception e) {
                dialog.dismiss();
                e.printStackTrace();
                Log.e("Upload file to server Exception", "Exception : "
                        + e.getMessage(), e);
                MyActivity.MyActivity_pointer.destroy_audio_record();
            }
            dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }
}
