package org.kaola.fgbox;

import org.kaola.fgbox.reposity.LocalReposity;
import org.kaola.fgbox.reposity.RemoteReposity;

import android.app.Application;

public class MainApplication extends Application {
	
	@Override
	public void onCreate() {
		RemoteReposity.init("http://www.kaolasoft.com");
		LocalReposity.init("/sdcard/FlashGameBox/reposity/");
		super.onCreate();
	}
	
}
