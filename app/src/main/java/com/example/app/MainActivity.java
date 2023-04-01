package com.example.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;


public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    public static final int STATUS_CODE = 0;

    Button select;
    ImageView imageView;
    Bitmap bitmap;
    Mat mat, mat_dst;


    static {
        System.loadLibrary("scanner");
        System.loadLibrary("opencv_java4");
    }


    // server settings
    private final String SERVER_ADDRESS = "192.168.1.23"; // make sure this matches whatever the server tells you
    private final String SERVER_PORT = "8080";

    // client settings
    private Socket clientSocket;


    public void hash2JSON(String[] hash_array) throws IOException {
        JSONObject jsonObj = new JSONObject();
        JSONArray jsonArr = new JSONArray();
        try {
            for (String h : hash_array) {
                JSONObject hash = new JSONObject();
                hash.put("hash", h);
                jsonArr.put(hash);
            }
            jsonObj.put("SAMSUNG", jsonArr);

        } catch (JSONException e) {
            e.printStackTrace();
        }

       new SendJSON().execute("http://"+SERVER_ADDRESS+":"+ SERVER_PORT +"/jsonreq", jsonObj.toString());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.sample_text);

        if (OpenCVLoader.initDebug()) {
            textView.setText(textView.getText() + "\n OPENCV LOADED SUCCESSFULLY");
            // start socket operations on a non-UI thread
            // new Thread(socketThread).start();
        } else {
            Log.d(TAG, "OPENCV DİD NOT LOAD");
        }


        select = findViewById(R.id.btnFlip);
        imageView = findViewById(R.id.imageView);

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int internet = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET);
                int wifi = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_WIFI_STATE);
                int network = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_NETWORK_STATE);

                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            STATUS_CODE);
                } else {
                    textView.setText(textView.getText() + "\n" + validate(500, 500));
                    bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.snap1)).getBitmap().copy(Bitmap.Config.ARGB_8888, true);
                    // bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.snap_9, null)).getBitmap().copy(Bitmap.Config.ARGB_8888, true);

                    mat = new Mat();
                    Utils.bitmapToMat(bitmap, mat);


                    String[] hash_array = getHashArray(mat.getNativeObjAddr(), 33);
                    try {
                        hash2JSON(hash_array);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    imageView.setImageBitmap(bitmap);
                }
            }
        });
    }


    public native String validate(long madAddrGr, long matAddrRgba);
    public native String[] getHashArray(long in, int cropping);
}

class SendJSON extends AsyncTask<String, Void, String> {

    public String ServerResponse;

    @Override
    protected String doInBackground(String... params) {

        String data = "";

        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = (HttpURLConnection) new URL(params[0]).openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            httpURLConnection.setRequestProperty("Accept", "application/json");
            httpURLConnection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());

            wr.write(params[1].getBytes());
            wr.flush();
            wr.close();

            InputStream in = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(in);

            int inputStreamData = inputStreamReader.read();
            while (inputStreamData != -1) {
                char current = (char) inputStreamData;
                inputStreamData = inputStreamReader.read();
                data += current;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }

        return data;
    }

    @Override
    protected void onPostExecute(String result) {
        ServerResponse = result;
        super.onPostExecute(result);
        Log.e("TAG", result); // this is expecting a response code to be sent from your server upon receiving the POST data
    }
}