package me.up.ctd;

/**
 * Created by addicted on 8/8/14.
 */


import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import android.util.Base64;

import java.io.File;

public class CtdFileManager extends Fragment {

    private Context context;
    public static boolean allow_init = false;
    public static boolean running = false;
    public static CtdFileManager ctd_file_manager;

    public CtdFileManager() {

    }

    @Override
    public void onStart() {
        super.onStart();
        this.context = getActivity().getApplicationContext();
        ctd_file_manager = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.ctd_file_manager, container, false);
    }

    public String[] read_dir(String path) {
        File dir = new File(path);
        String[] files = dir.list();
        return files;
    }

    public void append_gallery(String[] files) {
        String dir = Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera/";
        Bitmap bitmap_img;
        ByteArrayOutputStream baos;
        String base64;
        byte[] b;
        for (int i = 0; i < files.length; i++) {
            Log.i("File", files[i]);
            baos = new ByteArrayOutputStream();
            bitmap_img = BitmapFactory.decodeFile(dir + files[i]);
            bitmap_img.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            b = baos.toByteArray();
            base64 = Base64.encodeToString(b, Base64.DEFAULT);
            Log.i("Base 64", base64);
        }
    }

}
