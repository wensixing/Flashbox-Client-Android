package org.kaola.fgbox.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.io.FileUtils;
import org.kaola.fgbox.reposity.LocalReposity;
import org.kaola.fgbox.reposity.RemoteReposity;

import android.graphics.Bitmap;

public class FlashGame implements Serializable {
	
	private static final long serialVersionUID = -3145708199718486288L;
	
	protected final     String id;
	private   final     String name;
	protected transient Bitmap icon;
	
	public FlashGame(String id, String name) {
		this.id   = id;
		this.name = name;
	}
	
	public String getId() { return id; }
	
	public String getName() { return name; }
	
	public Bitmap getIcon() { return icon; }
	
	/**
	 * Lazy load the icon.
	 * @return true if load successfully, false if failed and won't change the old icon.
	 */
	public boolean loadIcon() {
		Bitmap icon = RemoteReposity.getInstance().loadImage(getIconPath(id));
		if(icon != null) {
			this.icon = icon;
			return true;
		}else
			return false;
	}
	
	/**
	 * Get icon path based on reposity(relative).
	 * @return relative icon path
	 */
	public static String getIconPath(String id) {
		return "image/flashgame/" + id + ".png";
	}
	
	/**
	 * Get data directory based on reposity(relative).
	 * @return relative data directory
	 */
	public static String getDataDir(String id) {
		return "data/" + id + "/";
	}
	
	/**
	 * Remove a flash game.
	 * @param dataDir Data directory to store flash games
	 * @param id id of flash game to remove
	 */
	public static boolean remove(String id) {
		File dataDirFile = new File(LocalReposity.getInstance().getFullPath(getDataDir(id)));
		File xmlFile     = new File(LocalReposity.getInstance().getFullPath(""), id + ".xml");
		
		if(xmlFile.exists() && !xmlFile.delete())
			return false;
		
		try {
			if(dataDirFile.exists())
				FileUtils.deleteDirectory(dataDirFile);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check if a flash game has been installed in local reposity.
	 * @param id Flash game id to check
	 * @return true if flash game with id has been installed
	 */
	public static boolean hasInstalled(String id) {
		File xmlFile = new File(LocalReposity.getInstance().getFullPath(id + ".xml"));
		return xmlFile.exists();
	}
	
}
