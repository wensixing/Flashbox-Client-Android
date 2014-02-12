package org.kaola.fgbox.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.kaola.fgbox.reposity.RemoteReposity;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.graphics.Bitmap;

public class Category implements Serializable {

	private static final long serialVersionUID = -4857069357991057903L;
	
	private final     String id;
	private final     String name;
	private transient Bitmap icon;
	
	public Category(String id, String name) {
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
		Bitmap icon = RemoteReposity.getInstance().loadImage("image/category/" + id + ".png");
		if(icon != null) {
			this.icon = icon;
			return true;
		}else
			return false;
	}
	
	/**
	 * Load game list of this category.
	 * @return list of flash games, or null if load failed.
	 */
	public List<FlashGame> getGames() {
		Element root = RemoteReposity.getInstance().loadXml("xml/category/" + id + ".xml");
		if(root == null)return null;
		
		List<FlashGame> games = new LinkedList<FlashGame>();
		
		NodeList nodes = root.getElementsByTagName("FlashGame");
		for(int i = 0; i < nodes.getLength(); ++i) {
			Element e = (Element) nodes.item(i);
			games.add(new FlashGame(e.getAttribute("id"), e.getAttribute("name")));
		}
		
		return games;
	}
}
