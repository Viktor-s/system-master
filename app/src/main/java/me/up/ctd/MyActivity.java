package me.up.ctd;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.os.Build;
import android.view.WindowManager;
import android.view.Window;
import android.util.Log;
import android.webkit.WebSettings;
import android.content.Context;
import android.content.IntentFilter;
import java.util.List;
import java.util.Map;
import android.webkit.WebChromeClient;
import java.util.HashMap;
import android.view.View;
import android.widget.FrameLayout;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.database.Cursor;
import android.widget.ImageView;
import java.lang.reflect.Method;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.location.Location;
import android.location.Criteria;
import android.location.LocationListener;
import android.provider.Settings;
import android.net.NetworkInfo;


public class MyActivity extends FragmentActivity {

    public WebView mWebview;
    static boolean active = false;
    public boolean web_loaded = false;
    public static boolean no_connection = false;
    public static MyActivity MyActivity_pointer;
    private final ViewGroup.LayoutParams hudLayout =
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private String selectedImagePath;
    private ImageView img;
    private RecordAudio record_fragment;
    private MediaMessage media_message_fragment;
    private CtdFileManager ctdFileManager;
    private int new_id = 0;
    private FragmentTransaction ft;
    private FragmentManager fm;
    public static String base_url = "http://justup.me/";
    double[] gps;

    double lat;
    double lon;


    public List<BroadcastReceiver> receivers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyActivity_pointer = this;
        int currentOrientation = getResources().getConfiguration().orientation;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        this.kill_bar();
        //this.show_bar();
        //this.clear_cash();
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        mWebview  = new WebView(this);
        mWebview.getSettings().setLoadsImagesAutomatically(true);
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.getSettings().setDatabaseEnabled(true);
        mWebview.getSettings().setDomStorageEnabled(true);
        mWebview.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        mWebview.getSettings().setSupportZoom(false);
        mWebview.getSettings().setUserAgentString(mWebview.getSettings().getUserAgentString() + " (XY ClientApp)");
        mWebview.getSettings().setAllowFileAccess(true);
        mWebview.getSettings().setSavePassword(false);
        mWebview.getSettings().setSupportMultipleWindows(false);
        mWebview.getSettings().setAppCacheEnabled(false);
        mWebview.getSettings().setAppCachePath("");
        mWebview.getSettings().setAppCacheMaxSize(5 * 1024 * 1024);
        mWebview.setPadding(0, 0, 0, 0);
        mWebview.getSettings().setLoadWithOverviewMode(true);
        mWebview.getSettings().setUseWideViewPort(true);
        mWebview.getSettings().setPluginState(WebSettings.PluginState.ON);
        mWebview.addJavascriptInterface(new WebAppInterface(this, mWebview), "Android");
        final Activity activity = this;
        mWebview.setWebChromeClient(new WebChromeClient());
        mWebview.clearCache(true);
        mWebview.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                //Toast.makeText(activity, description, Toast.LENGTH_SHORT).show();
                //reg_listener(getApplicationContext());
                no_connection = true;
                Intent i = new Intent(getApplicationContext(), WifiTester.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(i);
            }
            public void onPageFinished(WebView view, String url) {
                web_loaded = true;
                setContentView(mWebview);
                mWebview.loadUrl("javascript:top_panel.init()");
                init_top_panel();
                get_location();
                update_location();
            }
        });

        Map<String, String> noCacheHeaders = new HashMap<String, String>(2);
        noCacheHeaders.put("Pragma", "no-cache");
        noCacheHeaders.put("Cache-Control", "no-cache");
        setContentView(R.layout.activity_my);
        //setContentView(mWebview);

    }

    public void update_location() {
        if (gps[0] != 0 && gps[1] != 0) {
            WebAppInterface.wv_handler.update_location(gps[0], gps[1]);
        }
    }

    public void clear_cash() {
        PackageManager  pm = getPackageManager();
        Method[] methods = pm.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().equals("freeStorage")) {
                // Found the method I want to use
                try {
                    long desiredFreeStorage = 8 * 1024 * 1024 * 1024;
                    m.invoke(pm, desiredFreeStorage , null);
                } catch (Exception e) {
                    // Method invocation failed. Could be a permission problem
                }
                break;
            }
        }
    }

    public void check_for_updates() {
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "pm enable me.up.ctd.uptabupdate"});
            proc.waitFor();
        } catch (Exception e) {}
        UpdateChecker updateChecker = new UpdateChecker(getApplicationContext());
        updateChecker.execute("");
        try {
            Boolean update_needed = updateChecker.get();
            if (update_needed) {
                Intent i = getApplicationContext().getPackageManager().getLaunchIntentForPackage("me.up.ctd.uptabupdate");
                getApplicationContext().startActivity(i);
                try {
                    Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c", "pm disable me.up.ctd" });
                } catch (Exception e) {}
            }
        } catch (Exception e) {

        }
    }

    public void refresh() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MediaMessage.media_message.allow_init && MediaMessage.media_message.running) {
                    MediaMessage.media_message.running = false;
                    MediaMessage.media_message.allow_init = false;
                    fm.popBackStack();
                }
                if (RecordAudio.running) {
                    RecordAudio.running = false;
                    fm.popBackStack();
                }
                if (CtdFileManager.ctd_file_manager.running) {
                    CtdFileManager.ctd_file_manager.running = false;
                    fm.popBackStack();
                }
                MyActivity_pointer.recreate();
            }
        });
    }

    public void init_top_panel() {
        IntentFilter battery_filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        battery_filter.addAction(Intent.ACTION_POWER_CONNECTED);
        battery_filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        getApplicationContext().registerReceiver(new Battery(mWebview), battery_filter);
        IntentFilter time_filter = new IntentFilter();
        time_filter.addAction(Intent.ACTION_TIME_TICK);
        time_filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        time_filter.addAction(Intent.ACTION_TIME_CHANGED);
        getApplicationContext().registerReceiver(new Time(mWebview), time_filter);
        new Time(mWebview).update(getApplicationContext());
        new WebAppInterface(getApplicationContext(), mWebview).init_actions();
    }


    public void reg_listener(Context c) {
        BroadcastReceiver wifi_listener = new WifiStateListener();
        IntentFilter wifi_intents = new IntentFilter();
        wifi_intents.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        wifi_intents.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(wifi_listener, wifi_intents);
    }

    public static void is_reg(BroadcastReceiver receiver, Context c){
        List<BroadcastReceiver> rec = new MyActivity().receivers;
        boolean registered = rec.contains(receiver);
        if (registered) {
            rec.remove(receiver);
            c.unregisterReceiver(receiver);
        } else {
            rec.add(receiver);
        }
    }

    private void show_bar() {
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(new String[] { "su", "-c", "am startservice -n com.android.systemui/.SystemUIService" });
        } catch (Exception e) {
            Log.w("Main","Failed to kill task bar (1).");
            e.printStackTrace();
        }
        try {
            proc.waitFor();
        } catch (Exception e) {
            Log.w("Main","Failed to kill task bar (2).");
            e.printStackTrace();
        }
    }

    private void kill_bar() {
        Process proc = null;
        String ProcID = "79"; //HONEYCOMB AND OLDER
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            ProcID = "42"; //ICS AND NEWER
        }
        try {
            proc = Runtime.getRuntime().exec(new String[] { "su", "-c", "service call activity "+ProcID+" s16 com.android.systemui" });
        } catch (Exception e) {
            Log.w("Main","Failed to kill task bar (loop1).");
            e.printStackTrace();
        }
        try {
            proc.waitFor();
        } catch (Exception e) {
            Log.w("Main","Failed to kill task bar (loop2).");
            e.printStackTrace();
        }
    }

    private void get_location() {
        // Get the location manager
        /*LocationManager mlocManager=null;
        LocationListener mlocListener;
        mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mlocListener = new MyLocationListener();
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
        if (mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (MyLocationListener.longitude != 0 && MyLocationListener.latitude != 0) {
                Toast.makeText(getApplicationContext(), "Latitude: " + MyLocationListener.latitude + ", Longitude: " + MyLocationListener.longitude, Toast.LENGTH_SHORT).show();
            }
        }*/
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);

        /* Loop over the array backwards, and if you get an accurate location, then break out the loop*/
        Location l = null;

        for (int i=providers.size()-1; i>=0; i--) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) break;
        }

        gps = new double[2];
        if (l != null) {
            gps[0] = l.getLatitude();
            gps[1] = l.getLongitude();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
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

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (!mWifi.isConnected()) {
            no_connection = true;
            Intent i = new Intent(getApplicationContext(), WifiTester.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplicationContext().startActivity(i);

        } else {
            if (!web_loaded) {
                Map<String, String> noCacheHeaders = new HashMap<String, String>(2);
                mWebview.loadUrl(base_url, noCacheHeaders);
            }
            check_for_updates();
            get_location();
            if (!no_connection) {
                update_location();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    public void pick_image() {
        ctdFileManager = new CtdFileManager();
        final String[] files = ctdFileManager.read_dir(Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera");
        if (!CtdFileManager.ctd_file_manager.running) {
            CtdFileManager.ctd_file_manager.allow_init = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fm = getFragmentManager();
                    ft = fm.beginTransaction();
                    FrameLayout new_frame = new FrameLayout(getApplicationContext());
                    addContentView(new_frame, hudLayout);
                    new_id = View.generateViewId();
                    new_frame.setId(new_id);
                    ctdFileManager.setArguments(getIntent().getExtras());
                    ft.add(new_id, ctdFileManager).addToBackStack("file_manager").commit();
                    ctdFileManager.append_gallery(files);
                }
            });
        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public void record_audio() {
        RecordAudio.allow_record = true;
        if (!RecordAudio.record_audio.running) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fm = getFragmentManager();
                    ft = fm.beginTransaction();
                    record_fragment = new RecordAudio();
                    FrameLayout new_frame = new FrameLayout(getApplicationContext());
                    addContentView(new_frame, hudLayout);
                    new_id = View.generateViewId();
                    new_frame.setId(new_id);
                    record_fragment.setArguments(getIntent().getExtras());
                    ft.add(new_id, record_fragment).addToBackStack("record_audio").commit();
                }
            });
        }
    }

    public void media_message_start() {
        MediaMessage.media_message.allow_init = true;
        if (!MediaMessage.media_message.running) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fm = getFragmentManager();
                    ft = fm.beginTransaction();
                    ft.setCustomAnimations(R.animator.slide_in, R.animator.slide_out);
                    media_message_fragment = new MediaMessage();
                    FrameLayout new_frame = new FrameLayout(getApplicationContext());
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.gravity = Gravity.TOP | Gravity.RIGHT;
                    lp.setMargins(0, 647, 385, 0);
                    addContentView(new_frame, hudLayout);
                    new_frame.setLayoutParams(lp);
                    new_id = View.generateViewId();
                    new_frame.setId(new_id);
                    media_message_fragment.setArguments(getIntent().getExtras());
                    ft.add(new_id, media_message_fragment).addToBackStack("media_message").commit();
                }
            });
        }
    }

    public void kill_media() {
        MediaMessage.media_message.allow_init = false;
        if (MediaMessage.media_message.running) {
            MediaMessage.media_message.running = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fm.popBackStack();
                    ft = null;
                }
            });
        }
    }

    public void stop_recording() {
        RecordAudio.allow_record = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RecordAudio.record_audio.onPause();
            }
        });
    }

    public void destroy_audio_record() {
        if (RecordAudio.running) {
            RecordAudio.running = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fm.popBackStack();
                    ft = null;
                }
            });
        }
    }
}

