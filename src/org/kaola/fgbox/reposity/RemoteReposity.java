package org.kaola.fgbox.reposity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.kaola.fgbox.model.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

public class RemoteReposity {
	
	private static RemoteReposity instance;
	
	private Uri reposity;
	
	private RemoteReposity(String reposity) {
		this.reposity = Uri.parse(reposity);
	}
	
	/**
	 * Load an xml file from reposity.
	 * @param url relative url of an xml file
	 * @return document element of the xml file, or null if failed to load.
	 */
	public Element loadXml(String url) {
		DocumentBuilder builder;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalArgumentException(e);
		}
		
		Document document;
		try {
			document = builder.parse(Uri.withAppendedPath(reposity, url).toString());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return document.getDocumentElement();
	}
	
	/**
	 * Load an image from reposity.
	 * @param url relative url of an image file
	 * @return bitmap of the image, or null if failed to load.
	 */
	public Bitmap loadImage(String url) {
		URL imgUrl = loadURL(url);
		try {
			return BitmapFactory.decodeStream(imgUrl.openConnection().getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} 
	}
	
	/**
	 * Load an url from reposity
	 * @param url relative url to load
	 * @return an URL instance
	 */
	public URL loadURL(String url) {
		try {
			return new URL(Uri.withAppendedPath(reposity, url).toString());
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Get list of categories
	 * @return list of categories, or null if load failed
	 */
	public List<Category> getCategories() {
		Element root = loadXml("xml/categories.xml");
		if(root == null)return null;
		
		List<Category> cats = new LinkedList<Category>();
		
		NodeList nodes = root.getElementsByTagName("Category");
		for(int i = 0; i < nodes.getLength(); ++i) {
			Element e = (Element) nodes.item(i);
			cats.add(new Category(e.getAttribute("id"), e.getAttribute("name")));
		}
		
		return cats;
	}
	
	/**
	 * Initial RemoteReposity Class.
	 * @param reposity reposity url
	 */
	public static void init(String reposity) {
		if(instance != null)
			throw new IllegalArgumentException("Remote reposity has been initiated earlier.");
		instance = new RemoteReposity(reposity);
	}
	
	public static RemoteReposity getInstance() {
		if(instance == null)
			throw new IllegalArgumentException("Remote reposity class hasn't been initiated yet.");
		return instance;
	}
}