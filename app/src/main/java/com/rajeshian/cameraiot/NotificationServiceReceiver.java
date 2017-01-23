package com.rajeshian.cameraiot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent ServiceIntent = new Intent(context, NotificationService.class);
        context.startService(ServiceIntent);
    }
}
