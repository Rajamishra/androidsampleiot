package com.rajeshian.cameraiot;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Regions;

import java.io.UnsupportedEncodingException;

import static com.rajeshian.cameraiot.MainActivity.LOG_TAG;



public class NotifService extends IntentService {

    public NotifService(){
        super("NotifService");
    }

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "aqwf0ncryp7rw.iot.ap-southeast-2.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "ap-southeast-2:5296b281-1e1c-40c3-81f8-f0c292d87cf0";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.AP_SOUTHEAST_2;

    //Topic for sending device commands
    private static final String TOPIC_DEVICE_Notif="sdk/test/Notif";



    AWSCredentials awsCredentials;
    AWSIotMqttManager mqttManager;
    String clientId;

    NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Camera IoT")
                    .setContentText("Pay Attention!, There is activity at your place");


    @Override
    protected void onHandleIntent(Intent intent){
            mqttManager.subscribeToTopic(TOPIC_DEVICE_Notif, AWSIotMqttQos.QOS0,
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


