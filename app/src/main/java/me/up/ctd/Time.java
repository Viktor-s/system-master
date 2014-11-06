package me.up.ctd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by addicted on loop7/17/14.
 */
public class Time extends BroadcastReceiver {

    WebView mWebView;

    Time (WebView w_v) {
        this.mWebView = w_v;
    }

    public void update(Context context) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String current = sdf.format(new Date());
        new WebAppInterface(context, this.mWebView).update_time(current);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        update(context);
    }

}
