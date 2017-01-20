package com.rajeshian.cameraiot;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegView;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {

    public String Videolink = "";
    static final String LOG_TAG = "Main Activity";
    @BindView(R.id.mjpegViewDefault)
    MjpegView mjpegView;
    String RegId="";

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "aqwf0ncryp7rw.iot.ap-southeast-2.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "ap-southeast-2:5296b281-1e1c-40c3-81f8-f0c292d87cf0";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.AP_SOUTHEAST_2;

    //Topic for sending device commands
   // private static final String TOPIC_DEVICE_COMMAND = "sdk/test/Python";
    private static final String TOPIC_DEVICE_IP="sdk/test/Python1";

    TextView tvStatus;
    Button btnConnect;
    Button btnDisconnect;
    Button btnUp, btnDown, btnLeft, btnRight;

    AWSIotMqttManager mqttManager;
    String clientId;
    String message;

    AWSCredentials awsCredentials;
    CognitoCachingCredentialsProvider credentialsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(connectClick);
        btnConnect.setEnabled(false);

        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(disconnectClick);



        btnUp = (Button) findViewById(R.id.btnUp);
        btnDown = (Button) findViewById(R.id.btnDown);
        btnLeft = (Button) findViewById(R.id.btnLeft);
        btnRight = (Button) findViewById(R.id.btnRight);

        btnUp.setOnClickListener(publishClick);
        btnDown.setOnClickListener(publishClick);
        btnLeft.setOnClickListener(publishClick);
        btnRight.setOnClickListener(publishClick);



        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // The following block uses IAM user credentials for authentication with AWS IoT.
        //awsCredentials = new BasicAWSCredentials("ACCESS_KEY_CHANGE_ME", "SECRET_KEY_CHANGE_ME");
        //btnConnect.setEnabled(true);

        // The following block uses a Cognito credentials provider for authentication with AWS IoT.
        new Thread(new Runnable() {
            @Override
            public void run() {
                awsCredentials = credentialsProvider.getCredentials();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnConnect.setEnabled(true);
                    }
                });
            }
        }).start();
//        Toast.makeText(this.getApplicationContext(), "Hi Rajesh!!!",Toast.LENGTH_LONG);
    }
    private DisplayMode calculateDisplayMode() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE ?
                DisplayMode.FULLSCREEN : DisplayMode.BEST_FIT;
    }

    private void loadIpCam() {
        Mjpeg.newInstance()
                .open("http://10.31.130.160:8090/?action=stream", 5)
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
        mjpegView.stopPlayback();
    }



    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, "clientId = " + clientId);
//            Toast.makeText(getApplicationContext(), "Hi Rajesh!!!",Toast.LENGTH_LONG).show();


            try {

                mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status == AWSIotMqttClientStatus.Connecting) {
                                    tvStatus.setText("Connecting...");

                                } else if (status == AWSIotMqttClientStatus.Connected) {
                                    tvStatus.setText("Connected");
//                                    Toast.makeText(getApplicationContext(), "Hi Rajesh!!!",Toast.LENGTH_LONG).show();
                                    mqttManager.publishString("ON", TOPIC_DEVICE_IP, AWSIotMqttQos.QOS0);
                                    mqttManager.subscribeToTopic(TOPIC_DEVICE_IP, AWSIotMqttQos.QOS0,
                                            new AWSIotMqttNewMessageCallback() {
                                                @Override
                                                public void onMessageArrived(final String topic, final byte[] data) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                String message = new String(data, "UTF-8");
                                                                Videolink=message;
                                                                //loadIpCam();
                                                                Log.d(LOG_TAG, "Message arrived:");
                                                                Log.d(LOG_TAG, "   Topic: " + topic);
                                                                Log.d(LOG_TAG, " Message: " + message);

                                                                tvStatus.setText(message);

                                                            } catch (UnsupportedEncodingException e) {
                                                                Log.e(LOG_TAG, "Message encoding error.", e);
                                                            }
                                                        }
                                                    });
                                                }
                                            });

                                } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Reconnecting");
                                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                        throwable.printStackTrace();
                                    }
                                    tvStatus.setText("Disconnected");
                                } else {
                                    tvStatus.setText("Disconnected");

                                }
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                Log.e(LOG_TAG, "Connection error.", e);
                tvStatus.setText("Error! " + e.getMessage());
            }

        }
    };

    View.OnClickListener publishClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String publishString = "";
            switch (v.getId()) {
                case R.id.btnUp:
                    publishString = "Up";
                    break;
                case R.id.btnDown:
                    publishString = "Down";
                    break;
                case R.id.btnLeft:
                    publishString = "Left";
                    break;
                case R.id.btnRight:
                    publishString = "Right";
                    break;
            }
            try {
                mqttManager.publishString(publishString, TOPIC_DEVICE_IP, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };

    View.OnClickListener disconnectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.disconnect();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Disconnect error.", e);
            }

        }
    };

    public void onStop() {

        super.onStop();
        mqttManager.publishString("OFF",TOPIC_DEVICE_IP,AWSIotMqttQos.QOS0);
    }

    public static String getLogTag() {
        return LOG_TAG;
    }

    public class GCMTokenRefresh extends FirebaseInstanceIdService {

        @Override
        public void onTokenRefresh() {
            // Get updated InstanceID token.
            String refreshedToken = FirebaseInstanceId.getInstance().getToken();
            Log.d(TAG, "Refreshed token: " + refreshedToken);
//            Toast.makeText(getApplicationContext(), "Rajesh" + refreshedToken,Toast.LENGTH_LONG).show();
  //          RegId = refreshedToken;

            // TODO: Implement this method to send any registration to your app's servers.
            sendRegistrationToServer(refreshedToken);
        }

        private void sendRegistrationToServer(String refreshedToken) {

            mqttManager.publishString(refreshedToken, TOPIC_DEVICE_IP, AWSIotMqttQos.QOS0);
        }
    }


}
