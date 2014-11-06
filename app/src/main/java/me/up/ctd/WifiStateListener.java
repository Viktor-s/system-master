package me.up.ctd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.app.Activity;

/**
 * Created by addicted on loop7/loop10/14.
 */
public class WifiStateListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo networkInfo =
                    intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if(networkInfo.isConnected()) {
                // Wifi is connected
                context.unregisterReceiver(this);
                /*Toast.makeText(context, "Wifi is connected: " + String.valueOf(networkInfo), Toast.LENGTH_SHORT).show();
                Intent i = c.getPackageManager()
                        .getLaunchIntentForPackage(c.getPackageName() );

                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK );
                c.startActivity(i);*/
                WifiManager refresher = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                refresher.startScan();
            }
        } else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            NetworkInfo networkInfo =
                    intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                    ! networkInfo.isConnected()) {
                // Wifi is disconnected
                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show();

            }
        }
    }

}
