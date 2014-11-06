package me.up.ctd;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.hardware.Camera;
import android.widget.Toast;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.hardware.Camera.PictureCallback;
import android.graphics.BitmapFactory;
import android.hardware.Camera.ShutterCallback;
import android.os.Environment;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import android.provider.MediaStore;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MediaMessage extends Fragment{

    public static MediaMessage media_message;
    private SurfaceView preview = null;
    private SurfaceHolder previewHolder = null;
    private Camera camera = null;
    private boolean inPreview = false;
    private boolean cameraConfigured = false;
    private ImageView switch_camera_button;
    private ImageView take_photo_button;
    private ImageView record_video_button;
    private int camera_int;
    private MediaRecorder recorder;
    private boolean recording = false;
    public String photo_output = null;
    public String video_output = null;
    private String UploadServerUri = MyActivity.base_url+"client/php/upload_media_message.php";
    private String outputFile = null;
    private String FileName;
    private ProgressDialog dialog = null;
    int serverResponseCode = 0;
    public static boolean allow_init = false;
    public static boolean running = false;

    public MediaMessage() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
        media_message = this;
        if (allow_init) {
            running = true;
            switch_camera_button = (ImageView)this.getActivity().findViewById(R.id.switch_camera);
            switch_camera_button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch_camera();
                }
            });

            take_photo_button = (ImageView)this.getActivity().findViewById(R.id.take_photo);
            take_photo_button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    take_picture();
                    take_photo_button.setOnClickListener(null);
                }
            });

            record_video_button = (ImageView)this.getActivity().findViewById(R.id.record_video);
            record_video_button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!recording) {
                        WebAppInterface.wv_handler.kill_switch();
                        recording = true;
                        record_video_button.setBackground(getResources().getDrawable(R.drawable.video_record_stop));
                        record_video();
                    } else {
                        record_video_button.setOnClickListener(null);
                        recording = false;
                        WebAppInterface.wv_handler.stop_tracking_time();
                        releaseMediaRecorder();
                        dialog = ProgressDialog.show(getActivity(), "", "Uploading file...", true);
                        new Thread(new Runnable() {
                            public void run() {
                                uploadFile(Environment.getExternalStorageDirectory().
                                        getAbsolutePath() + "/" + video_output);
                            }
                        }).start();
                    }
                }
            });

            preview = (SurfaceView)this.getActivity().findViewById(R.id.camerapreview);
            previewHolder = preview.getHolder();
            previewHolder.addCallback(surfaceCallback);
        }
    }

    public void toggle_media(String type) {
        if (type.equals("video")) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    record_video_button.setVisibility(View.VISIBLE);
                    take_photo_button.setVisibility(View.INVISIBLE);
                    record_video_button.bringToFront();
                }
            });
        } else {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    record_video_button.setVisibility(View.INVISIBLE);
                    take_photo_button.setVisibility(View.VISIBLE);
                    take_photo_button.bringToFront();
                }
            });
        }
    }

    public void record_video() {
        recorder = new MediaRecorder();
        String path = Environment.getExternalStorageDirectory().toString();
        long timestamp = System.currentTimeMillis();
        video_output = "video_message"+timestamp+".3gp";
        camera.unlock();
        recorder.setCamera(camera);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        recorder.setOutputFile(path + "/" + video_output);
        try {
            recorder.prepare();
            recorder.start();
            Toast.makeText(getActivity().getApplicationContext(), "Recording started", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {}
    }

    private void releaseMediaRecorder(){
        if (recorder != null) {
            // clear recorder configuration
            recorder.reset();
            // release the recorder object
            recorder.release();
            recorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            camera.lock();
        }
    }

    public void take_picture() {
        camera.takePicture(myShutterCallback, myPictureCallback_RAW, myPictureCallback_JPG);
    }

    public void switch_camera() {
        if (camera_int == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            camera_int = Camera.CameraInfo.CAMERA_FACING_BACK;
            switch_camera_button.setBackground(getResources().getDrawable(R.drawable.front));
        } else {
            camera_int = Camera.CameraInfo.CAMERA_FACING_FRONT;
            switch_camera_button.setBackground(getResources().getDrawable(R.drawable.back));
        }
        restart_camera();
    }

    public void restart_camera() {
        try {
            if (inPreview) {
                camera.stopPreview();
            }
            inPreview = false;
            camera.release();
            camera = Camera.open(camera_int);
            camera.setPreviewDisplay(previewHolder);
            inPreview = true;
            startPreview();
        } catch (IOException e) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media_message, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        camera_int = Camera.CameraInfo.CAMERA_FACING_FRONT;
        camera = Camera.open(camera_int);
        startPreview();
    }

    @Override
    public void onPause() {
        if (inPreview) {
            camera.stopPreview();
        }
        camera.release();
        camera = null;
        inPreview = false;
        releaseMediaRecorder();
        super.onPause();
    }

    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                }
                else {
                    int resultArea = result.width*result.height;
                    int newArea = size.width*size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return(result);
    }

    private void initPreview(int width, int height) {
        if (camera!=null && previewHolder.getSurface()!=null) {
            try {
                camera.setPreviewDisplay(previewHolder);
            }
            catch (Throwable t) {
                Log.e("PreviewDemo-surfaceCallback",
                        "Exception in setPreviewDisplay()", t);
                Toast
                        .makeText(getActivity().getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }

            if (!cameraConfigured) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = getBestPreviewSize(width, height,
                        parameters);

                if (size != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setParameters(parameters);
                    cameraConfigured = true;
                }
            }
        }
    }

    PictureCallback myPictureCallback_JPG = new PictureCallback(){
        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            Bitmap bitmapPicture
                    = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);
            String path = Environment.getExternalStorageDirectory().toString();
            OutputStream fOut = null;
            long timestamp = System.currentTimeMillis();
            photo_output = "photo"+timestamp+".jpg";
            File file = new File(path, photo_output);
            try {
                fOut = new FileOutputStream(file);
            } catch (FileNotFoundException e) {}

            (bitmapPicture).compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            try {
                fOut.flush();
                fOut.close();
            } catch (IOException e) {}
            try {
                MediaStore.Images.Media.insertImage(getActivity().getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
                dialog = ProgressDialog.show(getActivity(), "", "Uploading file...", true);
                new Thread(new Runnable() {
                    public void run() {
                        uploadFile(Environment.getExternalStorageDirectory().
                                getAbsolutePath() + "/" + photo_output);
                    }
                }).start();
            } catch (FileNotFoundException e) {}
        }
    };

    PictureCallback myPictureCallback_RAW = new PictureCallback(){
        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub

        }
    };

    ShutterCallback myShutterCallback = new ShutterCallback(){
        @Override
        public void onShutter() {
        }
    };

    private void startPreview() {
        if (cameraConfigured && camera != null) {
            camera.startPreview();
            inPreview = true;
        }
    }

    SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            // no-op -- wait until surfaceChanged()
        }

        public void surfaceChanged(SurfaceHolder holder,
                                   int format, int width,
                                   int height) {
            initPreview(width, height);
            startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // no-op
        }
    };

    @Override
    public void onDetach() {
        super.onDetach();
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
                            send_message();
                            MyActivity.MyActivity_pointer.kill_media();
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
                MyActivity.MyActivity_pointer.kill_media();
            } catch (Exception e) {
                dialog.dismiss();
                e.printStackTrace();
                Log.e("Upload file to server Exception", "Exception : "
                        + e.getMessage(), e);
                MyActivity.MyActivity_pointer.kill_media();
            }
            dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }

    public void send_message() {
        if (photo_output == null) {
            WebAppInterface.wv_handler.send_media_message(video_output);
        } else {
            WebAppInterface.wv_handler.send_media_message(photo_output);
        }
    }

}
