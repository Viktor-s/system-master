package me.up.ctd;

import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.graphics.Typeface;
import android.widget.Toast;
import android.net.wifi.WifiConfiguration;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.view.LayoutInflater;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.util.Log;

import org.w3c.dom.Text;

public class WifiTester extends Activity implements View.OnClickListener{
    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    StringBuilder sb = new StringBuilder();
    LinearLayout wifi_list;
    Button back_button;
    Typeface face;
    String current_bssid;
    private boolean first_init = true;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        first_init = true;
        setContentView(R.layout.activity_wifi_tester);
        wifi_list = (LinearLayout) findViewById(R.id.wifi_list);
        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mainWifi.setWifiEnabled(true);
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWifi.startScan();
        face = Typeface.createFromAsset(getAssets(),
                "fonts/PFBeauSansPro-Bold.ttf");
        TextView wifi_title = (TextView) findViewById(R.id.wifi_title);
        wifi_title.setText(MyActivity.no_connection ? "ДЛЯ ДАЛЬНЕЙШЕЙ РАБОТЫ ПОДКЛЮЧИТЕСЬ К СЕТИ WIFI" : "ИЗМЕНИТЬ WI-FI СЕТЬ \n\n");
        //mainText.setTypeface(face);
        wifi_title.setTypeface(face);
        back_button = (Button) findViewById(R.id.back_button);
        back_button.setTypeface(face);
        back_button.setOnClickListener(this);
    }

    public static String getCurrentSsid(Context context) {
        String ssid = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !connectionInfo.getSSID().isEmpty()) {
                ssid = connectionInfo.getBSSID();
            }
        }
        return ssid;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Refresh");
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        mainWifi.startScan();
        return super.onMenuItemSelected(featureId, item);
    }

    protected void onPause() {
        unregisterReceiver(receiverWifi);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    class WifiReceiver extends BroadcastReceiver implements View.OnClickListener {
        Context w_context;
        Integer security_type;
        String selected_ssid;
        WifiConfiguration conf = new WifiConfiguration();

        public void onReceive(Context c, Intent intent) {
            w_context = c;
            this.refresh_list();
        }

        public void refresh_list() {
            current_bssid = WifiTester.getCurrentSsid(w_context);
            String loop_ssid;
            wifiList = mainWifi.getScanResults();
            wifi_list.removeAllViews();
            final TextView[] myTextViews = new TextView[wifiList.size()];
            for(int i = 0; i < wifiList.size(); i++){
                loop_ssid = (wifiList.get(i)).SSID;
                final LinearLayout wifi_item = (LinearLayout)getLayoutInflater().inflate(R.layout.dummy, null);
                final TextView wifi_name = (TextView) wifi_item.findViewById(R.id.wifi_name);
                final ImageView connected = (ImageView) wifi_item.findViewById(R.id.connected);
                final ImageView security = (ImageView) wifi_item.findViewById(R.id.network_protected);
                final ImageView wifi_level = (ImageView) wifi_item.findViewById(R.id.wifi_strength);
                int level = wifiList.get(i).level;
                if (wifiList.get(i).capabilities.contains("WEP") || wifiList.get(i).capabilities.contains("WPA")) {
                    security.setBackground(getResources().getDrawable(R.drawable.wifi_lock));
                } else {
                    security.setBackground(null);
                }
                level = Math.abs(level);
                if (level > 45 && level < 55) {
                    wifi_level.setBackground(getResources().getDrawable(R.drawable.wifi_4));
                } else if (level > 35 && level < 65) {
                    wifi_level.setBackground(getResources().getDrawable(R.drawable.wifi_3));
                } else if (level > 20 && level < 80) {
                    wifi_level.setBackground(getResources().getDrawable(R.drawable.wifi_2));
                } else if (level > 10 && level < 90) {
                    wifi_level.setBackground(getResources().getDrawable(R.drawable.wifi_1));
                } else {
                    wifi_level.setBackground(getResources().getDrawable(R.drawable.wifi_0));
                }
                wifi_name.setText(loop_ssid);
                wifi_name.setTypeface(face);
                if (current_bssid != null) {
                    if (this.highlight((wifiList.get(i)).BSSID)) {
                        connected.setBackground(getResources().getDrawable(R.drawable.wifi_selected));
                        //rowTextView.invalidate();
                        if (first_init) {
                            first_init = false;
                        } else {
                            findViewById(R.id.back_button).performClick();
                        }
                    }
                }
                wifi_list.addView(wifi_item);
                wifi_item.setOnClickListener(this);
                //myTextViews[i] = wifi_item;
            }
        }

        @Override
        public void onClick(View view) {
            TextView w = (TextView) view.findViewById(R.id.wifi_name);
            selected_ssid = w.getText().toString();
            conf.SSID = "\"" + selected_ssid + "\"";
            List<ScanResult> networkList = mainWifi.getScanResults();
            for (ScanResult network : networkList)
            {
                if(network.SSID != null && network.SSID.equals(selected_ssid)) {
                    String cap = network.capabilities;
                    if (cap.contains("WPA")) {
                        security_type = 1;
                        this.showDialog();
                    } else if (cap.contains("WEP")) {
                        security_type = 2;
                        this.showDialog();
                    } else {
                        security_type = 3;
                        connect("");
                    }
                    break;
                }
            }
        }

        public boolean highlight(String bssid) {
            return current_bssid.equals(bssid);
        }

        public void connect(String pw) {
            switch (security_type) {
                case 1:
                    conf.preSharedKey = "\""+ pw +"\"";
                    break;
                case 2:
                    conf.wepKeys[0] = "\"" + pw + "\"";
                    conf.wepTxKeyIndex = 0;
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    break;
                case 3:
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    break;
            }
            mainWifi.addNetwork(conf);
            List<WifiConfiguration> list = mainWifi.getConfiguredNetworks();
            for( WifiConfiguration i : list ) {
                if(i.SSID != null && i.SSID.equals("\"" + selected_ssid + "\"")) {
                    mainWifi.disconnect();
                    mainWifi.enableNetwork(i.networkId, true);
                    BroadcastReceiver wifi_listener = new WifiStateListener();
                    IntentFilter wifi_intents = new IntentFilter();
                    wifi_intents.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                    wifi_intents.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                    registerReceiver(wifi_listener, wifi_intents);
                    mainWifi.reconnect();
                    break;
                }
            }
        }


        public void showDialog() {
            LayoutInflater li = LayoutInflater.from(w_context);
            View promptsView = li.inflate(R.layout.pw, null);
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(w_context);
            alertDialogBuilder.setView(promptsView);
            final EditText userInput = (EditText) promptsView
                    .findViewById(R.id.psw);
            alertDialogBuilder
                    .setCancelable(false)
                    .setNegativeButton("Connect",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    String user_text = (userInput.getText()).toString();
                                    connect(user_text);
                                }
                            })
                    .setPositiveButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    dialog.dismiss();
                                }

                            }
                    );
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back_button:
                this.finish();
                if (MyActivity.no_connection) {
                    Context c = getApplicationContext();
                    Intent i = c.getPackageManager()
                            .getLaunchIntentForPackage(c.getPackageName() );
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK );
                    c.startActivity(i);
                }
                break;
        }
    }
}
