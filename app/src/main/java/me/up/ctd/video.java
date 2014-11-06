package me.up.ctd;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import me.up.ctd.R;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;
import android.net.Uri;
import android.provider.MediaStore.Video;
import android.view.Display;
import android.graphics.Point;
import android.view.MotionEvent;
import android.os.Handler;
import android.util.Log;

public class video extends Activity {

    public static String video_num = "up";
    private final ViewGroup.LayoutParams hudLayout =
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    public video video_pointer;
    public ImageView close;
    private boolean touch_active = false;
    private Handler mHandler = new Handler();
    public MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        video_pointer = this;
        setContentView(R.layout.activity_video);
        final VideoView videoView = (VideoView)findViewById(R.id.videoView);
        String uriPath = Environment.getExternalStorageDirectory().
                getAbsolutePath() + "/upme_" + video_num;
        Log.d("video file", uriPath);
        Uri uri = Uri.parse(uriPath);
        videoView.setVideoURI(uri);
        mediaController = new
                MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        create_close_button();
        videoView.start();
        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!touch_active) {
                    touch_active = true;
                    toggleNav();
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            touch_active = false;
                        }
                    }, 200);
                }
                return true;
            }
        });
    }

    public void create_close_button() {
        close = new ImageView(this);
        addContentView(close, hudLayout);
        close.bringToFront();
        FrameLayout.LayoutParams align_close = new FrameLayout.LayoutParams(110,
                110, Gravity.LEFT | Gravity.TOP);
        close.setBackgroundResource(R.drawable.close);
        align_close.setMargins(0, 100, 0, 0);
        close.setLayoutParams(align_close);
        close.setVisibility(View.INVISIBLE);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDestroy();
            }
        });
    }

    private void toggleNav() {
        if (close.getVisibility() == View.VISIBLE) {
            mediaController.show(1);
            close.setVisibility(View.INVISIBLE);
        } else {
            close.setVisibility(View.VISIBLE);
            close.bringToFront();
            mediaController.show(0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.video, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
