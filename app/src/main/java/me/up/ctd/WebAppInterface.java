package me.up.ctd;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.webkit.JavascriptInterface;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.webkit.WebView;
import android.media.AudioManager;
import android.net.Uri;

import java.io.IOException;

import apprtc.AppRTCDemoActivity;
import android.os.PowerManager;
import android.os.SystemClock;
import android.app.AlertDialog;
import android.content.DialogInterface;


/**
 * Created by addicted on loop7/loop8/14.
 */
public class WebAppInterface extends WebViewClient{
    Context mContext;
    WebView mWebView;
    String room_id;
    MediaPlayer ring;
    public static MediaPlayer ring_handler;
    public static WebAppInterface wv_handler;
    /** Instantiate the interface and set the context */
    public WebAppInterface(Context c, WebView mWebView) {
        mContext = c;
        this.mWebView = mWebView;
        wv_handler = this;
    }

    public void update_battery_info(float state) {
        this.mWebView.loadUrl("javascript:top_panel.battery_update("+state+")");
    }

    public void update_battery_info_charging() {
        this.mWebView.loadUrl("javascript:top_panel.battery_charging()");
    }

    public void update_time(String current) {
        this.mWebView.loadUrl("javascript:top_panel.time_update('"+current+"')");
    }

    public void update_location(double latitude, double longitude) {
        this.mWebView.loadUrl("javascript:top_panel.update_location('"+latitude+"', '"+longitude+"');");
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void wifi_manager() {
        Intent i = new Intent(mContext, WifiTester.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    @JavascriptInterface
    public void stream(String id, String avatar, String my_avatar) {
        AppRTCDemoActivity.room_id = id;
        AppRTCDemoActivity.calling_avatar = avatar;
        AppRTCDemoActivity.my_avatar = my_avatar;
        Intent i = new Intent(mContext, AppRTCDemoActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    @JavascriptInterface
    public void call_ignored() {
        Toast.makeText(mContext, "Call IGNORED", Toast.LENGTH_SHORT).show();
        AppRTCDemoActivity.kill_handler.finish();
    }

    @JavascriptInterface
    public void start_video(String num) {
        Intent i = new Intent(mContext, video.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        video.video_num = num;
        mContext.startActivity(i);
    }

    @JavascriptInterface
    public void pick_image() {
        MyActivity.MyActivity_pointer.pick_image();
    }

    @JavascriptInterface
    public void record_audio() {
        MyActivity.MyActivity_pointer.record_audio();
    }

    @JavascriptInterface
    public void stop_recording() {
        MyActivity.MyActivity_pointer.stop_recording();
    }

    @JavascriptInterface
    public void ring_ring() {
        play_sound(MyActivity.base_url+"audio/ring.wav");
    }

    @JavascriptInterface
    public void play_waiting_sound() {
        play_sound(MyActivity.base_url+"audio/calling.mp3");
    }

    public void play_sound(String url_string) {
        // Get the AudioManager
        AudioManager audioManager =
                (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
// Set the volume of played media to maximum.
        audioManager.setStreamVolume (
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0);
        float count=100*.01f;
        ring = null;
        Uri url = Uri.parse(url_string);
        ring = new MediaPlayer();
        ring.setAudioStreamType(AudioManager.STREAM_MUSIC);
        ring.setVolume(count, count);
        ring.setLooping(true);
        try {
            ring.setDataSource(mContext, url);
            ring.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ring.start();
        ring_handler = ring;
    }

    @JavascriptInterface
    public void stop_ringing() {
        if (ring != null) {
            ring.stop();
            ring.release();
            ring = null;
        }
    }

    public void stop_tracking_time() {
        this.mWebView.loadUrl("javascript:MediaMessage.stop_counting();");
    }

    public void send_audio_message(String file_name) {
        this.mWebView.loadUrl("javascript:AudioMessage.send('"+file_name+"')");
    }

    public void send_media_message(String file_name) {
        this.mWebView.loadUrl("javascript:MediaMessage.send('"+file_name+"')");
    }

    public void track_audio_record_level(int level) {

        this.mWebView.loadUrl("javascript:AudioMessage.track_level('"+level+"')");
    }

    @JavascriptInterface
    public void toggle_media(String type) {
        if (MediaMessage.media_message != null) {
            if (MediaMessage.media_message.running) {
                MediaMessage.media_message.toggle_media(type);
            }
        }
    }

    @JavascriptInterface
    public void media_prompt() {
        MyActivity.MyActivity_pointer.media_message_start();
    }

    @JavascriptInterface
    public void kill_media_fragment() {
        MyActivity.MyActivity_pointer.kill_media();
    }

    @JavascriptInterface
    public void kill_switch() {
        this.mWebView.loadUrl("javascript:MediaMessage.kill_switch()");
    }

    @JavascriptInterface
    public void refresh_webview() {
        MyActivity.MyActivity_pointer.refresh();
    }

    public void call_dropped() {
        this.mWebView.loadUrl("javascript:AndroidCall.dropped()");
    }

    public void init_actions() {
        this.mWebView.loadUrl("javascript:top_panel.init()");
    }

    public void return_pictures(String[] files) {

    }

}
