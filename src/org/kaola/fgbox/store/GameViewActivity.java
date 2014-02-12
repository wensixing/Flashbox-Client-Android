package org.kaola.fgbox.store;

import org.kaola.fgbox.FlashActivity;
import org.kaola.fgbox.R;
import org.kaola.fgbox.download.DownloadService;
import org.kaola.fgbox.download.DownloadService.LocalBinder;
import org.kaola.fgbox.download.DownloaderFactory.DownloaderStatus;
import org.kaola.fgbox.download.DownloaderFactory.OnDownloadListener;
import org.kaola.fgbox.model.FlashGame;
import org.kaola.fgbox.reposity.RemoteReposity;
import org.w3c.dom.Element;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GameViewActivity extends Activity {
	
	private ServiceConnection connection;
	private LocalBinder binder;
	private FlashGame flashGame;
	
	private Thread loadingThread;
	
	private TextView    nameView;
	private ImageView   iconView;
	private TextView    sizeView;
	private View        loading;
	private TextView    despView;
	private View        statusBar;
	private TextView    statusView;
	private ProgressBar progress;
	private Button      downloadBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_view);
		
		//repo       = new FgboxReposity(getString(R.string.REPOSITORY_URI));
		
		nameView   =  (TextView) findViewById(R.id.name);
		iconView   =  (ImageView) findViewById(R.id.icon);
		sizeView   =  (TextView) findViewById(R.id.size);
		loading    =  findViewById(R.id.loading);
		despView   =  (TextView) findViewById(R.id.description);
		statusBar  =  findViewById(R.id.statusbar);
		statusView =  (TextView) findViewById(R.id.status);
		progress   =  (ProgressBar) findViewById(R.id.progress);
		downloadBtn = (Button) findViewById(R.id.download);
		
		onNewIntent(getIntent());
	}
	
	@Override
	protected synchronized void onNewIntent(Intent intent) {
		//Set Loading
		loading.setVisibility(View.VISIBLE);
		
		//Get flash game from intent
		flashGame = (FlashGame) intent.getSerializableExtra("flashGame");
		
		//Clear current task
		if(loadingThread != null) {
			loadingThread.interrupt();
			try {
				loadingThread.join();
			} catch (InterruptedException e) {
				throw new IllegalArgumentException(e);
			}
			loadingThread = null;
		}
		if(connection != null) {
			unbindService(connection);
			connection = null;
		}
		
		//Start loading thread
		loadingThread = new Thread(){
			Bitmap icon;
			String description;
			String size;
			
			@Override
			public void run() {
				//Load FlashGame xml
				Element flashgame;
				for(flashgame = null; flashgame == null;)
					flashgame = RemoteReposity.getInstance().loadXml("xml/FlashGame/" + flashGame.getId() + ".xml");
				for(icon = null; icon == null;)
					icon = RemoteReposity.getInstance().loadImage(flashgame.getAttribute("icon"));
				description = flashgame.getElementsByTagName("Description").item(0).getTextContent();
				
				//Load Filelist xml
				Element filelist;
				for(filelist = null; filelist == null;)
					filelist = RemoteReposity.getInstance().loadXml(String.format("xml/Filelist/%s.xml", flashGame.getId()));
				double sizeF = Float.parseFloat(filelist.getAttribute("size"));
				int i;
				String[] suffix = {"B", "K", "M"};
				for(i = 0; sizeF >= 1000; ++i) sizeF /= 1000;
				size = String.format("%.2f%s", sizeF, suffix[i]);
				
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						nameView.setText(flashGame.getName());
						iconView.setImageBitmap(icon);
						sizeView.setText(size);
						despView.setText(description);
						loading.setVisibility(View.INVISIBLE);
					}
				});
			}
		};
		loadingThread.start();
		
		//Bind download service
		bindService(new Intent(this, DownloadService.class).putExtra("flashGame", flashGame), connection = new ServiceConnection() {
			
			@Override
			public void onServiceDisconnected(ComponentName name) {}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				binder = (LocalBinder) service;
				binder.registerListener(flashGame.getId(), downloadListener);
			}
		}, BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onDestroy() {
		unbindService(connection);
		super.onDestroy();
	}
	
	private final OnDownloadListener downloadListener = new OnDownloadListener() {
		
		private final Runnable onNotStart = new Runnable() {
			
			@Override
			public void run() {
				statusBar.setVisibility(View.INVISIBLE);
				if(FlashGame.hasInstalled(flashGame.getId())) {
					//downloadBtn.setText(R.string.download_again);
					downloadBtn.setText(R.string.play_game);
					downloadBtn.setOnClickListener(new OnClickListener() {
					
						@Override
						public void onClick(View v) {
							startActivity(new Intent(GameViewActivity.this, FlashActivity.class).putExtra("flashGame", flashGame));
							finish();
						}
					});
				}
				else {
					downloadBtn.setText(R.string.download);
					downloadBtn.setOnClickListener(new OnClickListener() {
					
						@Override
						public void onClick(View v) {
							binder.startDownload(flashGame);
							binder.registerListener(flashGame.getId(), downloadListener);
						}
					});
				}
			}
		};
		
		private final Runnable onPrepare = new Runnable() {
			
			@Override
			public void run() {
				progress.setVisibility(View.VISIBLE);
				statusView.setText(R.string.status_prepare);
				progress.setIndeterminate(true);
				downloadBtn.setText(R.string.cancel_download);
				downloadBtn.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						binder.cancelDownload(flashGame.getId());
					}
				});
				statusBar.setVisibility(View.VISIBLE);
			}
		};
		
		private final Runnable onDownloading = new Runnable() {
			
			@Override
			public void run() {
				progress.setVisibility(View.VISIBLE);
				statusBar.setVisibility(View.VISIBLE);
				downloadBtn.setText(R.string.cancel_download);
				downloadBtn.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						binder.cancelDownload(flashGame.getId());
					}
				});
			}
		};
		
		private final Runnable onFailed = new Runnable() {
			
			@Override
			public void run() {
				statusView.setText(R.string.status_failed);
				progress.setVisibility(View.GONE);
				if(FlashGame.hasInstalled(flashGame.getId())) {
					//downloadBtn.setText(R.string.download_again);
					downloadBtn.setText(R.string.play_game);
					downloadBtn.setOnClickListener(new OnClickListener() {
					
						@Override
						public void onClick(View v) {
							startActivity(new Intent(GameViewActivity.this, FlashActivity.class).putExtra("flashGame", flashGame));
							finish();
						}
					});
				}
				else {
					downloadBtn.setText(R.string.download);
					downloadBtn.setOnClickListener(new OnClickListener() {
					
						@Override
						public void onClick(View v) {
							binder.startDownload(flashGame);
							binder.registerListener(flashGame.getId(), downloadListener);
						}
					});
				}
			}
		};
		
		private final Runnable onFinished = new Runnable() {
			
			@Override
			public void run() {
				statusView.setText(R.string.status_finish);
				progress.setVisibility(View.GONE);
				if(FlashGame.hasInstalled(flashGame.getId())) {
					//downloadBtn.setText(R.string.download_again);
					downloadBtn.setText(R.string.play_game);
					downloadBtn.setOnClickListener(new OnClickListener() {
					
						@Override
						public void onClick(View v) {
							startActivity(new Intent(GameViewActivity.this, FlashActivity.class).putExtra("flashGame", flashGame));
							finish();
						}
					});
				}
				else {
					downloadBtn.setText(R.string.download);
					downloadBtn.setOnClickListener(new OnClickListener() {
					
						@Override
						public void onClick(View v) {
							binder.startDownload(flashGame);
							binder.registerListener(flashGame.getId(), downloadListener);
						}
					});
				}
			}
		};

		@Override
		public void onStatusChanged(int status) {
			switch(status){
			case DownloaderStatus.NOT_START:
				runOnUiThread(onNotStart);
				break;
			case DownloaderStatus.PREPARE:
				runOnUiThread(onPrepare);
				break;
			case DownloaderStatus.DOWNLOADING:
				runOnUiThread(onDownloading);
				break;
			case DownloaderStatus.FAILED:
				runOnUiThread(onFailed);
				break;
			case DownloaderStatus.FINISHED:
				runOnUiThread(onFinished);
				break;
			}
		}
		
		@Override
		public void onPercentChanged(final int percent) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					statusView.setText(getString(R.string.status_downloading, percent));
					statusBar.setVisibility(View.VISIBLE);
					progress.setIndeterminate(false);
					progress.setMax(100);
					progress.setProgress(percent);
				}
			});
		}
	};
}
