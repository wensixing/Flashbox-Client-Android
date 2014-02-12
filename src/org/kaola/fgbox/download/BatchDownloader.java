package org.kaola.fgbox.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;


public class BatchDownloader {
	
	private static final int NETWORK_TIME_OUT = 5000;
	private static final int BUFFER_SIZE      = 8192;
	
	private final Vector<URL>    urls  = new Vector<URL>();
	private final Vector<String> paths = new Vector<String>();
	
	private OnBatchDownloadListener listener;
	
	/**
	 * Add a new download task.
	 * @param url
	 * @param path path to save downloaded file
	 */
	public void add(URL url, String path) {
		urls.add(url);
		paths.add(path);
	}
	
	public void setOnBatchDownloadListener(OnBatchDownloadListener listener) {
		this.listener = listener;
	}
	
	public static interface OnBatchDownloadListener {
		void onDownloadProgress(int size);
		void onDownloadFailed();
		void onDownloadFinished();
	}
	
	public void start() throws InterruptedException {
		int size = 0;
		for(int i = 0; i < urls.size(); ++i) {
			try {
				URLConnection conn = urls.get(i).openConnection();
				conn.setConnectTimeout(NETWORK_TIME_OUT);
				conn.setReadTimeout(NETWORK_TIME_OUT);
				conn.connect();
				
				new File(FilenameUtils.getPath(paths.get(i))).mkdirs();
				
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(paths.get(i)), BUFFER_SIZE);
				BufferedInputStream  in  = new BufferedInputStream(conn.getInputStream(), BUFFER_SIZE);
				byte[] buffer = new byte[BUFFER_SIZE];
				for(int len; (len = in.read(buffer)) != -1;) {
					if(Thread.interrupted()) {
						in.close(); out.close();
						throw new InterruptedException();
					}
					out.write(buffer, 0, len);
					size += len;
					if(listener != null)listener.onDownloadProgress(size);
				}
				in.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
				if(listener != null)listener.onDownloadFailed();
				return;
			}
		}
		if(listener != null)listener.onDownloadFinished();
	}
}