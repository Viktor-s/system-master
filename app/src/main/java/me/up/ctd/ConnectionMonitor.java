package me.up.ctd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
/**
 * Created by addicted on loop7/loop6/14.
 */
public class ConnectionMonitor extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION))
            return;
        boolean noConnectivity = intent.getBooleanExtra(
                ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        NetworkInfo aNetworkInfo = (NetworkInfo) intent
                .getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
        if (!noConnectivity) {
            if ((aNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                    || (aNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)) {
                // start your service stuff here
            }
        } else {
            if ((aNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                    || (aNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)) {
                // stop your service stuff here
            }
        }
    }
}