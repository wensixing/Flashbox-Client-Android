package org.kaola.fgbox.download;

import org.kaola.fgbox.download.DownloaderFactory.OnDownloadListener;
import org.kaola.fgbox.download.NotificationFactory.DownloadNotification;
import org.kaola.fgbox.model.FlashGame;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class DownloadService extends Service {
	
	private NotificationFactory notificationFactory;
	private DownloaderFactory   downloaderFactory;
	
	private final LocalBinder localBinder = new LocalBinder();
	
	public class LocalBinder extends Binder {
		
		public synchronized void startDownload(FlashGame flashGame) {
			if(downloaderFactory.start(flashGame.getId())) {
				DownloadNotification notification = notificationFactory.create(flashGame);
				downloaderFactory.registerListener(flashGame.getId(), notification);
			}
		}
		
		public synchronized void registerListener(String id, OnDownloadListener listener) {
			downloaderFactory.registerListener(id, listener);
		}
		
		public synchronized void cancelDownload(String id) {
			downloaderFactory.cancel(id);
		}
	}
	
	@Override
	public void onCreate() {
		notificationFactory = new NotificationFactory(this);
		downloaderFactory   = new DownloaderFactory(this);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		startService(new Intent(this, DownloadService.class));
		setForeground(true);
		return localBinder;
	}
}