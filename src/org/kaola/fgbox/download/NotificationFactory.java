package org.kaola.fgbox.download;

import java.util.HashMap;
import java.util.Map;

import org.kaola.fgbox.Main;
import org.kaola.fgbox.R;
import org.kaola.fgbox.download.DownloaderFactory.DownloaderStatus;
import org.kaola.fgbox.download.DownloaderFactory.OnDownloadListener;
import org.kaola.fgbox.model.FlashGame;
import org.kaola.fgbox.store.GameViewActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

public class NotificationFactory {
	
	private final Context             context;
	private final NotificationManager notificationManager;
	private       int                 next_id;
	
	private final Map<Integer, DownloadNotification> notifications = new HashMap<Integer, DownloadNotification>();
	
	public NotificationFactory(Context context) {
		this.context             = context;
		this.next_id             = 0;
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	/**
	 * Create a new {@link DownloadNotification}.
	 * @param flashGame FlashGame bound to this notification
	 */
	public synchronized DownloadNotification create(FlashGame flashGame) {
		DownloadNotification notification = new DownloadNotification(next_id, flashGame);
		notifications.put(next_id, notification);
		++next_id;
		return notification;
	}
	
	public class DownloadNotification implements OnDownloadListener {
		
		private final int          id;
		private final RemoteViews  remoteViews;
		private final Notification notification;
		
		private DownloadNotification(int id, FlashGame flashGame) {
			this.id           = id;
			this.remoteViews  = new RemoteViews(context.getPackageName(), R.layout.download_notification);
			this.notification = new Notification(android.R.drawable.stat_sys_download, null, System.currentTimeMillis());
		
			notification.flags         = Notification.FLAG_NO_CLEAR;
			notification.contentView   = this.remoteViews;
			notification.contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, GameViewActivity.class).putExtra("flashGame", flashGame), PendingIntent.FLAG_UPDATE_CURRENT);
			
			remoteViews.setTextViewText(R.id.name, flashGame.getName());
			remoteViews.setInt(R.id.progress, "setMax", 100);
		}
		
		/**
		 * Notify this notification.
		 */
		public void show() {
			notificationManager.notify(id, notification);
		}
		
		/**
		 * Remove this notification from status bar.
		 */
		public void remove() {
			notificationManager.cancel(id);
		}

		@Override
		public void onStatusChanged(int status) {
			switch(status) {
			case DownloaderStatus.PREPARE:
				remoteViews.setTextViewText(R.id.status, context.getText(R.string.status_prepare));
				remoteViews.setBoolean(R.id.progress, "setIndeterminate", true);
				break;
			case DownloaderStatus.DOWNLOADING:
				remoteViews.setBoolean(R.id.progress, "setIndeterminate", false);
				break;
			case DownloaderStatus.FAILED:
				remoteViews.setTextViewText(R.id.status, context.getString(R.string.status_failed));
				remoteViews.setInt(R.id.progress_wrapper, "setVisibility", View.INVISIBLE);
				notification.flags = Notification.FLAG_AUTO_CANCEL;
				notifications.remove(id);
				break;
			case DownloaderStatus.FINISHED:
				remoteViews.setTextViewText(R.id.status, context.getString(R.string.status_finish));
				remoteViews.setInt(R.id.progress_wrapper, "setVisibility", View.INVISIBLE);
				notification.flags = Notification.FLAG_AUTO_CANCEL;
				notification.contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, Main.class), PendingIntent.FLAG_UPDATE_CURRENT);
				notifications.remove(id);
				break;
			}
			show();
		}

		@Override
		public void onPercentChanged(int percent) {
			remoteViews.setInt(R.id.progress, "setMax", 100);
			remoteViews.setInt(R.id.progress, "setProgress", percent);
			remoteViews.setTextViewText(R.id.status, context.getString(R.string.status_downloading, percent));
			show();
		}
	}
}