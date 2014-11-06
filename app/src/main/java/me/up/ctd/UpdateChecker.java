package me.up.ctd;

/**
 * Created by addicted on 8/7/14.
 */

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class UpdateChecker extends AsyncTask<String, Void, Boolean> {

    private String version_name;
    private String update_check_url = MyActivity.base_url+"client/php/check_updates.php";
    private Context context;
    private boolean update_needed = false;

    public UpdateChecker(Context context) {
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        update_needed = false;
        try {
            version_name = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            Log.i("Version", version_name);
        } catch (PackageManager.NameNotFoundException e) {}
        if (version_name != null) {
            try
            {
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(update_check_url);

                MultipartEntity reqEntity = new MultipartEntity();
                reqEntity.addPart("request", new StringBody("compare_versions"));
                reqEntity.addPart("version", new StringBody(version_name));
                post.setEntity(reqEntity);
                HttpResponse response = client.execute(post);
                HttpEntity resEntity = response.getEntity();
                final String response_str = EntityUtils.toString(resEntity);
                if (resEntity != null) {
                    Log.i("RESPONSE", response_str);
                    if (response_str.equals("update needed")) {
                        update_needed = true;
                    }
                }
            }
            catch (Exception ex){
                Log.e("Debug", "error: " + ex.getMessage(), ex);
            }
        }

        return update_needed;
    }

    protected void onPostExecute(String page) {
        //onPostExecute
    }
}
