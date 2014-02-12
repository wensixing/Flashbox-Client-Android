package org.kaola.fgbox.download;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.kaola.fgbox.download.BatchDownloader.OnBatchDownloadListener;
import org.kaola.fgbox.model.FlashGame;
import org.kaola.fgbox.reposity.LocalReposity;
import org.kaola.fgbox.reposity.RemoteReposity;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.content.Context;

import static org.apache.commons.io.FilenameUtils.concat;

public class DownloaderFactory {
	
	public static interface DownloaderStatus {
		int NOT_START   = 0;
		int PREPARE     = 1;
		int DOWNLOADING = 2;
		int FAILED      = 3;
		int FINISHED    = 4;
	}
	
	private final Map<String, Downloader> downloaders = new HashMap<String, Downloader>();
	
	public DownloaderFactory(Context context) {
	}
	
	/**
	 * Start downloading a flash game
	 * @param id Flash game id to download
	 * @return false if a download task with this id already exists
	 */
	public synchronized boolean start(String id) {
		if(!downloaders.containsKey(id)) {
			downloaders.put(id, new Downloader(id));
			return true;
		} else return false;
	}
	
	/**
	 * Cancel a downloading task
	 * @param id Flash game id to cancel
	 */
	public synchronized void cancel(String id) {
		Downloader downloader = downloaders.get(id);
		if(downloader != null)
			downloader.cancel();
	}
	
	public synchronized void registerListener(String id, OnDownloadListener listener) {
		Downloader downloader = downloaders.get(id);
		if(downloader != null)
			downloader.registerListener(listener);
		else
			listener.onStatusChanged(DownloaderStatus.NOT_START);
			//throw new IllegalArgumentException("Flash game id " + id + "doesn't exist.");
	}
	
	private synchronized void remove(String id) {
		downloaders.remove(id);
	}
	
	public static interface OnDownloadListener {
		void onStatusChanged(int status);
		void onPercentChanged(int percent);
	}
	
	private class Downloader implements OnDownloadListener {
		private       int    status;
		private       int    percent;
		private final String id;
		private final Thread thread;
		
		private final List<WeakReference<OnDownloadListener>> listeners = new LinkedList<WeakReference<OnDownloadListener>>();
		
		public Downloader(String flashGameId) {
			this.id        = flashGameId;
			this.status    = DownloaderStatus.PREPARE;
			this.percent   = 0;
			this.thread    = new Thread() {
				public void run() {
					LocalReposity   local = LocalReposity.getInstance();
					RemoteReposity  remote = RemoteReposity.getInstance();
					BatchDownloader batchDownloader = new BatchDownloader();
					
					//Remove existing data
					if(!FlashGame.remove(id)) {
						onStatusChanged(DownloaderStatus.FAILED);
						return;
					}
					if(Thread.interrupted())return;
					
					//Load file list from xml
					Element root = remote.loadXml("xml/Filelist/" + id + ".xml");
					if(root == null){onStatusChanged(DownloaderStatus.FAILED); return;}
					if(Thread.interrupted())return;
					
					//Initial download size
					final int totalsize = Integer.parseInt(root.getAttribute("size"));
					
					//Add data files to batch downloader
					NodeList nodes = root.getElementsByTagName("File");
					for(int i = 0; i < nodes.getLength(); ++i) {
						Element e = (Element) nodes.item(i);
						batchDownloader.add(remote.loadURL(e.getAttribute("url")), 
								concat(local.getFullPath(FlashGame.getDataDir(id)), e.getAttribute("path")));
					}
					
					//Add icon file to batch downloader
					batchDownloader.add(remote.loadURL(FlashGame.getIconPath(id)),
							local.getFullPath(FlashGame.getIconPath(id)));
					
					//Add xml file to batch downloader
					batchDownloader.add(remote.loadURL("xml/FlashGame/" + id + ".xml"),
							local.getFullPath(id + ".xml"));
					
					//Set up batch downloader
					if(Thread.interrupted())return;
					onStatusChanged(DownloaderStatus.DOWNLOADING);
					batchDownloader.setOnBatchDownloadListener(new OnBatchDownloadListener() {
						
						@Override
						public void onDownloadProgress(int size) {
							int newpercent = (int) (size * 100L / totalsize);
							if(newpercent > 100)newpercent = 100;
							if(newpercent != percent) {
								percent = newpercent;
								onPercentChanged(percent);
							}
						}
						
						@Override
						public void onDownloadFailed() {
							onStatusChanged(DownloaderStatus.FAILED);
						}

						@Override
						public void onDownloadFinished() {
							onStatusChanged(DownloaderStatus.FINISHED);
						}
					});
					
					//Start download
					try {
						batchDownloader.start();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			thread.start();
		}
		
		public synchronized void registerListener(OnDownloadListener listener) {
			if(!listeners.contains(listener))
				listeners.add(new WeakReference<OnDownloadListener>(listener));
			listener.onStatusChanged(status);
			if(status == DownloaderStatus.DOWNLOADING)listener.onPercentChanged(percent);
		}
		
		public synchronized void cancel() {
			thread.interrupt();
			try {
				thread.join();
				onStatusChanged(DownloaderStatus.FAILED);
			} catch (InterruptedException e) {
				throw new IllegalArgumentException(e);
			}
		}
		
		/*
		public synchronized void removeListener(OnDownloadListener listener) {
			listeners.remove(listener);
		}
		*/
		
		@Override
		public synchronized void onStatusChanged(int status) {
			this.status = status;
			for(WeakReference<OnDownloadListener> ref : listeners) {
				OnDownloadListener listener = ref.get();
				if(listener != null)
					listener.onStatusChanged(status);
			}
			if(status == DownloaderStatus.FAILED || status == DownloaderStatus.FINISHED)
				DownloaderFactory.this.remove(id);
		}

		@Override
		public synchronized void onPercentChanged(int percent) {
			this.percent = percent;
			for(WeakReference<OnDownloadListener> ref : listeners) {
				OnDownloadListener listener = ref.get();
				if(listener != null)
					listener.onPercentChanged(percent);
			}
		}
	}

}