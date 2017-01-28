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

    public static final String LOG_TAG = "CamIOT Application";


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
                Constants.COGNITO_POOL_ID, // Identity Pool ID
                Constants.MY_REGION // Region
        );

        Region region = Region.getRegion(Constants.MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, Constants.CUSTOMER_SPECIFIC_ENDPOINT);
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
        mqttManager.subscribeToTopic(Constants.TOPIC_DEVICE_NOTIFICATION, AWSIotMqttQos.QOS0,
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
