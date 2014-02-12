package org.kaola.fgbox.store;

import org.kaola.fgbox.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public abstract class LoaderActivity extends Activity {
	
	private View     loading;
	private ListView listView;
	private Thread   thread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_store);
		
		loading  = findViewById(R.id.loading);
		listView = (ListView) findViewById(R.id.list_view);
	}
	
	@Override
	protected void onResume() {
		loading.setVisibility(View.VISIBLE);
		thread   = new Thread() {
			public void run() {
				while(!load()) if(isInterrupted()) return;
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						loading.setVisibility(View.INVISIBLE);
					}
				});
			}
		};
		thread.start();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		thread.interrupt();
		/*
		try {
			thread.join();
		} catch (InterruptedException e) {
			throw new IllegalArgumentException(e);
		}
		*/
		super.onPause();
	}
	
	protected ListView getListView() { return listView; }
	
	abstract protected boolean load();
}
