package me.up.ctd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;
import android.widget.Toast;
import android.os.BatteryManager;

import java.lang.reflect.Constructor;

/**
 * Created by addicted on loop7/17/14.
 */
public class Battery extends BroadcastReceiver {
    float prev = 100;
    WebView w_v;

    Battery (WebView w_v) {
        this.w_v = w_v;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean is_charging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        if (!is_charging) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float battery_pct = level / (float)scale;
            if (battery_pct != this.prev) {
                this.prev = battery_pct;
                String test = this.prev + "";
                new WebAppInterface(context, this.w_v).update_battery_info(this.prev);
            }
        } else {
            new WebAppInterface(context, this.w_v).update_battery_info_charging();
        }
    }

}
