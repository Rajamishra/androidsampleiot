package com.rajeshian.cameraiot;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegSurfaceView;
import com.github.niqdev.mjpeg.MjpegView;

import java.io.File;
import java.io.UnsupportedEncodingException;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity{

    public String Videolink = "";
    public String snapShotPath = "";
    static final String LOG_TAG = "Main Activity";

    Button btnLeft, btnRight, btnAuto;
    LinearLayout mjpegContainer;
    ImageView snapshotView;

    CamIOT mCamIOT;
    MjpegView mjpegView;

    boolean isAutoEnabled = false;

    // This is the main class for interacting with the Transfer Manager
    private TransferUtility transferUtility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        ButterKnife.bind(this);

        mCamIOT = (CamIOT) getApplicationContext();
        mCamIOT.mActivity = this;

        btnLeft = (Button) findViewById(R.id.btnLeft);
        btnRight = (Button) findViewById(R.id.btnRight);
        btnAuto = (Button) findViewById(R.id.btnAuto);

        btnLeft.setOnClickListener(publishClick);
        btnRight.setOnClickListener(publishClick);
        btnAuto.setOnClickListener(publishClick);

        mjpegContainer = (LinearLayout) findViewById(R.id.mjpegContainer);
        snapshotView = (ImageView) findViewById(R.id.imageView);

        mjpegView = (MjpegSurfaceView) findViewById(R.id.mjpegViewDefault);

        transferUtility = Util.getTransferUtility(this);

        if(mCamIOT.connected)
            subscribe();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.video:
                if(mCamIOT.connected)
                    mCamIOT.mqttManager.publishString("ON", Constants.TOPIC_DEVICE_COMMAND, AWSIotMqttQos.QOS0);
                mjpegContainer.setVisibility(View.GONE);
                snapshotView.setVisibility(View.GONE);
                return true;
            case R.id.snapshot:
                mjpegContainer.setVisibility(View.GONE);
                snapshotView.setVisibility(View.GONE);
                if(mCamIOT.connected)
                    mCamIOT.mqttManager.publishString("Picture", Constants.TOPIC_DEVICE_COMMAND, AWSIotMqttQos.QOS0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void subscribe(){
        mCamIOT.subscribed = true;
        mCamIOT.mqttManager.subscribeToTopic(Constants.TOPIC_DEVICE_IP, AWSIotMqttQos.QOS0,
                new AWSIotMqttNewMessageCallback() {
                    @Override
                    public void onMessageArrived(final String topic, final byte[] data) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Videolink=new String(data, "UTF-8");
                                    loadIpCam();

                                } catch (UnsupportedEncodingException e) {
                                    Log.e(LOG_TAG, "Message encoding error.", e);
                                }
                            }
                        });
                    }
                });

        mCamIOT.mqttManager.subscribeToTopic(Constants.TOPIC_DEVICE_SNAP, AWSIotMqttQos.QOS0,
                new AWSIotMqttNewMessageCallback() {
                    @Override
                    public void onMessageArrived(final String topic, final byte[] data) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String received = new String(data, "UTF-8");
                                    Log.d(LOG_TAG,"received :" + received);
                                    beginDownload(received);

                                } catch (UnsupportedEncodingException e) {
                                    Log.e(LOG_TAG, "Message encoding error.", e);
                                }
                            }
                        });
                    }
                });
        Intent intent = new Intent(this, NotificationService.class);
        startService(intent);
        Log.i(LOG_TAG, "subscribed");
    }


    private DisplayMode calculateDisplayMode() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE ?
                DisplayMode.FULLSCREEN : DisplayMode.BEST_FIT;
    }

    private void loadIpCam() {
        if(Videolink.contentEquals(""))
            return;
        mjpegContainer.setVisibility(View.VISIBLE);
        snapshotView.setVisibility(View.GONE);
        Log.e(LOG_TAG,Videolink);
        Mjpeg.newInstance()
                .open(Videolink, 10)
                .subscribe(
                        inputStream -> {
                            mjpegView.setSource(inputStream);
                            mjpegView.setDisplayMode(calculateDisplayMode());
                            mjpegView.showFps(true);
                        },
                        throwable -> {
                            Log.e(getClass().getSimpleName(), "mjpeg error", throwable);
                            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
                        });
    }

    private void beginDownload(String filename) {
        String dir = Environment.getExternalStorageDirectory().toString() + "/" + "SmartCCTV";
        File directory = new File(dir);
        if (! directory.exists()){
            directory.mkdir();
        }

        snapShotPath = dir + '/' + filename;

        File file = new File(snapShotPath);

        // Initiate the download
        TransferObserver observer = transferUtility.download(Constants.BUCKET_NAME, filename, file);

        observer.setTransferListener(new DownloadListener());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadIpCam();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mjpegView.isStreaming())
            mjpegView.stopPlayback();
    }

    View.OnClickListener publishClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String publishString = "";
            switch (v.getId()) {
                case R.id.btnLeft:
                    publishString = "Left";
                    break;
                case R.id.btnRight:
                    publishString = "Right";
                    break;
                case R.id.btnAuto:
                    if(isAutoEnabled) {
                        publishString = "Manual";
                        btnLeft.setEnabled(true);
                        btnRight.setEnabled(true);
                        btnAuto.setText(R.string.Auto);
                    }
                    else {
                        publishString = "Auto";
                        btnLeft.setEnabled(false);
                        btnRight.setEnabled(false);
                        btnAuto.setText(R.string.Manual);
                    }
                    isAutoEnabled = !isAutoEnabled;
                    break;

            }
            try {
                mCamIOT.mqttManager.publishString(publishString, Constants.TOPIC_DEVICE_COMMAND, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };

    @Override
    protected void onStop() {

        super.onStop();
        if(mCamIOT.connected) {
            mCamIOT.mqttManager.publishString("STOP", Constants.TOPIC_DEVICE_COMMAND, AWSIotMqttQos.QOS0);
            mCamIOT.mqttManager.unsubscribeTopic(Constants.TOPIC_DEVICE_IP);
            mCamIOT.subscribed = false;
        }
        mCamIOT.mActivity = null;
    }

    private class DownloadListener implements TransferListener {

        // Simply updates the list when notified.
        @Override
        public void onError(int id, Exception e) {

        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
        }

        @Override
        public void onStateChanged(int id, TransferState state) {
            if(state == TransferState.COMPLETED) {
                File imgFile = new File(snapShotPath);

                if(imgFile.exists()){

                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                    snapshotView.setImageBitmap(myBitmap);
                    snapshotView.setVisibility(View.VISIBLE);
                    mjpegContainer.setVisibility(View.GONE);
                    snapShotPath = "";

                }
            }
        }
    }

}