package com.rajeshian.cameraiot;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

public class NotificationService extends Service{

    CamIOT mCamIOT;

    @Override
    public void onCreate() {
        super.onCreate();
        mCamIOT = (CamIOT) getApplicationContext();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mCamIOT.subscribeNotif();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
