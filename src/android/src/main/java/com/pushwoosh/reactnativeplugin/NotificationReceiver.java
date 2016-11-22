package com.pushwoosh.reactnativeplugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.pushwoosh.PushManager;
import com.pushwoosh.internal.PushManagerImpl;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONObject;

public class NotificationReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent)
    {
        if (intent == null)
            return;

        PWLog.debug(PushwooshPlugin.TAG, "NotificationReceiver onReceive");

        Bundle pushBundle = PushManagerImpl.preHandlePush(context, intent);
        if(pushBundle == null)
            return;

        JSONObject dataObject = PushManagerImpl.bundleToJSON(pushBundle);

        Intent launchIntent  = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        launchIntent.addCategory("android.intent.category.LAUNCHER");

        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        launchIntent.putExtras(pushBundle);
        launchIntent.putExtra(PushManager.PUSH_RECEIVE_EVENT, dataObject.toString());

        context.startActivity(launchIntent);

        PushManagerImpl.postHandlePush(context, intent);

        PushwooshPlugin.openPush(dataObject.toString());
    }
}
