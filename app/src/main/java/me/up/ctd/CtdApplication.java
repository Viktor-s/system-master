package me.up.ctd;

import android.app.Application;
import android.app.AlarmManager;
import android.provider.SyncStateContract.Constants;
import android.util.Log;
import android.app.PendingIntent;
import android.content.Intent;

/**
 * Created by addicted on 8/14/14.
 */
public class CtdApplication extends Application {
    private Thread.UncaughtExceptionHandler mOnRuntimeError;
    @Override
    public void onCreate() {
        super.onCreate();
        final PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MyActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mOnRuntimeError = new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable ex) {
                AlarmManager mgr = (AlarmManager) getSystemService(
                        getApplicationContext().ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis(),
                        intent);
                System.exit(2);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(mOnRuntimeError);
    }
}
