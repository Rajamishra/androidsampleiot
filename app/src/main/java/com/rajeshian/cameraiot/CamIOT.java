package com.rajeshian.cameraiot;

import android.app.Application;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

import java.util.UUID;

public class CamIOT extends Application {

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    public final String CUSTOMER_SPECIFIC_ENDPOINT = "aqwf0ncryp7rw.iot.ap-southeast-2.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    public final String COGNITO_POOL_ID = "ap-southeast-2:5296b281-1e1c-40c3-81f8-f0c292d87cf0";

    // Region of AWS IoT
    public final Regions MY_REGION = Regions.AP_SOUTHEAST_2;

    //Topic for sending device commands
    public final String TOPIC_DEVICE_COMMAND = "sdk/test/Onoff";
    public final String TOPIC_DEVICE_IP="sdk/test/IPadd";
    public final String TOPIC_DEVICE_NOTIFICATION = "sdk/test/Notif";

    public final String LOG_TAG = "CamIOT Application";

    public boolean subscribed = false;
    public boolean connected = false;

    NotificationCompat.Builder mBuilder;

    AWSIotMqttManager mqttManager;
    String clientId;

    AWSCredentials awsCredentials;
    CognitoCachingCredentialsProvider credentialsProvider;

    public MainActivity mActivity = null;

    @Override
    public void onCreate() {
        super.onCreate();

        clientId = UUID.randomUUID().toString();

        mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Camera IoT")
                        .setContentText("Pay Attention!, There is activity at your place");

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                awsCredentials = credentialsProvider.getCredentials();

            }
        }).start();

        try {

            mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    if (status == AWSIotMqttClientStatus.Connected) {
                        connected = true;
                        subscribeNotif();
                        if(mActivity!=null)
                            mActivity.subscribe();
                    } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                        connected = false;
                        if (throwable != null) {
                            Log.e(LOG_TAG, "Connection error.", throwable);
                        }
                    } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                        connected = false;
                        if (throwable != null) {
                            Log.e(LOG_TAG, "Connection error.", throwable);
                            throwable.printStackTrace();
                        }

                    }


                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
        }

    }

    public void subscribeNotif() {
        mqttManager.subscribeToTopic(TOPIC_DEVICE_NOTIFICATION, AWSIotMqttQos.QOS0,
                new AWSIotMqttNewMessageCallback() {
                    @Override
                    public void onMessageArrived(final String topic, final byte[] data) {
                        int mNotificationId = 1;
// Gets an instance of the NotificationManager service
                        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.
                        mNotifyMgr.notify(mNotificationId, mBuilder.build());


                    }
                });
    }
}
