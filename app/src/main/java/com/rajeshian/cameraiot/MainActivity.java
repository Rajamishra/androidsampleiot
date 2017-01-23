package com.rajeshian.cameraiot;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegSurfaceView;
import com.github.niqdev.mjpeg.MjpegView;

import java.io.UnsupportedEncodingException;

import butterknife.ButterKnife;

public class MainActivity extends Activity {

    public String Videolink = "";
    static final String LOG_TAG = "Main Activity";

    Button btnLeft, btnRight;

    CamIOT mCamIOT;
    MjpegView mjpegView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        ButterKnife.bind(this);

        mCamIOT = (CamIOT) getApplicationContext();
        mCamIOT.mActivity = this;

        btnLeft = (Button) findViewById(R.id.btnLeft);
        btnRight = (Button) findViewById(R.id.btnRight);

        btnLeft.setOnClickListener(publishClick);
        btnRight.setOnClickListener(publishClick);

        mjpegView = (MjpegSurfaceView) findViewById(R.id.mjpegViewDefault);
        if(mCamIOT.connected)
            subscribe();

    }

    public void subscribe(){
        mCamIOT.subscribed = true;
        mCamIOT.mqttManager.publishString("ON", mCamIOT.TOPIC_DEVICE_COMMAND, AWSIotMqttQos.QOS0);
        mCamIOT.mqttManager.subscribeToTopic(mCamIOT.TOPIC_DEVICE_IP, AWSIotMqttQos.QOS0,
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
        Log.e("saketh",Videolink);
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
            }
            try {
                mCamIOT.mqttManager.publishString(publishString, mCamIOT.TOPIC_DEVICE_COMMAND, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };

    @Override
    protected void onStop() {

        super.onStop();
        if(mCamIOT.connected) {
            mCamIOT.mqttManager.publishString("STOP", mCamIOT.TOPIC_DEVICE_COMMAND, AWSIotMqttQos.QOS0);
            mCamIOT.mqttManager.unsubscribeTopic(mCamIOT.TOPIC_DEVICE_IP);
            mCamIOT.subscribed = false;
        }
        mCamIOT.mActivity = null;
    }

}