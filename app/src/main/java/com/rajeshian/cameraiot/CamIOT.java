package com.rajeshian.cameraiot;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

import java.io.File;
import java.net.URI;
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

    TransferUtility transferUtility;

    public MainActivity mActivity = null;

    @Override
    public void onCreate() {
        super.onCreate();

        clientId = UUID.randomUUID().toString();

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

        transferUtility = Util.getTransferUtility(this);

    }

    public void subscribeNotif() {
        mqttManager.subscribeToTopic(Constants.TOPIC_DEVICE_NOTIFICATION, AWSIotMqttQos.QOS0,
                new AWSIotMqttNewMessageCallback() {
                    @Override
                    public void onMessageArrived(final String topic, final byte[] data) {

                        beginDownload("image4.jpg");
                    }
                });
    }

    private void beginDownload(String filename) {
        String dir = Environment.getExternalStorageDirectory().toString() + "/" + "SmartCCTV";
        File directory = new File(dir);
        if (! directory.exists()){
            directory.mkdir();
        }

        File file = new File(dir + '/' + "intruder.jpg");

        // Initiate the download
        TransferObserver observer = transferUtility.download("camiottest", "image4.jpg", file);

        observer.setTransferListener(new DownloadListener());
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
                int mNotificationId = 1;
// Gets an instance of the NotificationManager service
                NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.

                Intent intent = new Intent(getApplicationContext(), ImageViewActivity.class);

                    PendingIntent contentIntent =
                            PendingIntent.getActivity(getApplicationContext(),
                                    id,
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );

                    mBuilder =
                            new NotificationCompat.Builder(getApplicationContext())
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("Camera IoT")
                                    .setContentText("Pay Attention!, There is activity at your place")
                                    .setContentIntent(contentIntent);


                    mNotifyMgr.notify(mNotificationId, mBuilder.build());
//                    Util.deleteFile(getApplicationContext(),"camiottest","image4.jpg");
                }
            }
        }
    }
