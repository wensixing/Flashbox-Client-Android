package org.kaola.fgbox.reposity;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.kaola.fgbox.model.FlashGame;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class LocalReposity {
	
	private static LocalReposity instance;
	
	private final String dataDir;
	
	private LocalReposity(String dataDir) {
		new File(dataDir).mkdirs();
		this.dataDir = dataDir;
	}
	
	/**
	 * Get flash games from local reposity.
	 * @return list of games
	 */
	public List<FlashGame> getGames() {
		List<FlashGame> games = new LinkedList<FlashGame>();
		for(File file : FileUtils.listFiles(new File(dataDir), new String[]{"xml"}, false)) if(file.isFile()) {
			if(FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("xml")) {
				String id, name;
				try {
					DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					Document document = builder.parse(file);
					Element root = document.getDocumentElement();
					id = root.getAttribute("id");
					name = root.getAttribute("name");
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				if(id != null && name != null)
					games.add(new FlashGame(id, name) {
						@Override
						public boolean loadIcon() {
							Bitmap icon = loadImage(getIconPath(id));
							if(icon != null) {
								this.icon = icon;
								return true;
							}else
								return false;
						}
					});
			}
		}
		
		return games;
	}
	
	/**
	 * Load an image from local reposity.
	 * @param path relative path of an image file
	 * @return bitmap of the image, or null if failed to load.
	 */
	public Bitmap loadImage(String url) {
		return BitmapFactory.decodeFile(new File(dataDir, url).toString());
	}
	
	/**
	 * Get an absolute path by a relative path based on reposity
	 * @param pathFragment(don't start with "/")
	 * @return an absolute path
	 */
	public String getFullPath(String pathFragment) {
		return FilenameUtils.concat(dataDir, pathFragment);
	}
		
	public static void init(String dataDir) {
		if(instance != null)
			throw new IllegalArgumentException("Local reposity has been initiated earlier.");
		instance = new LocalReposity(dataDir);
	}
	
	public static LocalReposity getInstance() {
		if(instance == null)
			throw new IllegalArgumentException("Local reposity class hasn't been initiated yet.");
		return instance;
	}
}
