package com.pushwoosh.reactnativeplugin;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;

public class PushwooshNotificationServiceExtension extends NotificationServiceExtension {

	private boolean mBroadcastPush;

	public PushwooshNotificationServiceExtension() {
		try {
			String packageName = getApplicationContext().getPackageName();
			ApplicationInfo ai = getApplicationContext().getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);

			if (ai.metaData != null) {
				mBroadcastPush = ai.metaData.getBoolean("PW_BROADCAST_PUSH", true);
			}
		} catch (Exception e) {
			PWLog.exception(e);
		}

		PWLog.debug(PushwooshPlugin.TAG, "broadcastPush = " + mBroadcastPush);
	}

	@Override
	protected boolean onMessageReceived(final PushMessage pushMessage) {
		PushwooshPlugin.messageReceived(pushMessage.toJson().toString());
		return mBroadcastPush && super.onMessageReceived(pushMessage);
	}

	@Override
	protected void startActivityForPushMessage(final PushMessage pushMessage) {
		super.startActivityForPushMessage(pushMessage);
		PushwooshPlugin.openPush(pushMessage.toJson().toString());
	}
}
