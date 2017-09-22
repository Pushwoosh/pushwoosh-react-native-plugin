package com.pushwoosh.reactnativeplugin;

import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;

public class PushwooshNotificationServiceExtension extends NotificationServiceExtension {

	@Override
	protected boolean onMessageReceived(final PushMessage pushMessage) {
		return super.onMessageReceived(pushMessage);
	}

	@Override
	protected void startActivityForPushMessage(final PushMessage pushMessage) {
		super.startActivityForPushMessage(pushMessage);
		PushwooshPlugin.openPush(pushMessage.toJson().toString());
	}
}
