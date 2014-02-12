package org.kaola.fgbox;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.kaola.fgbox.model.FlashGame;
import org.kaola.fgbox.reposity.LocalReposity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewStub;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;

public class FlashActivity extends Activity {
	
	private WebView      webview;
	private ViewStub     left_dpad;
	private ViewStub  	 right_dpad;
	private LinearLayout left_button_bar;
	private LinearLayout right_button_bar;
	
	private FlashGame flashGame;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simple_buttons_flash);
		
		webview  	     = (WebView)      findViewById(R.id.webview);
		left_dpad	     = (ViewStub)     findViewById(R.id.left_dpad);
		right_dpad       = (ViewStub)  	  findViewById(R.id.right_dpad);
		left_button_bar  = (LinearLayout) findViewById(R.id.left_button_bar);
		right_button_bar = (LinearLayout) findViewById(R.id.right_button_bar);
		
		flashGame = (FlashGame) getIntent().getSerializableExtra("flashGame");
		
		parseXML();
	}
	
	private final void parseXML() {
		Document document;
		try {
			/* //TODO: Current Android SDK 2.2 has a buggy schemaFactory, so skip validate XSD phase
			SchemaFactory schemaFactory = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(true);
			factory.setSchema(schemaFactory.newSchema(new StreamSource(getAssets().open("FlashGame.xsd"))));
			
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(new InputSource("document.xml"));
			*/
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(new File(LocalReposity.getInstance().getFullPath(flashGame.getId() + ".xml")));
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		
		Element flashgame = document.getDocumentElement();
		
		NodeList nodes = flashgame.getChildNodes();
		for(int i = 0; i < nodes.getLength(); ++i) if(nodes.item(i).getNodeType() == Element.ELEMENT_NODE) {
			Element element = (Element) nodes.item(i);
			String tag = element.getTagName();
			String position = element.getAttribute("position");
			if(tag.equals("DPad")) { //Generate DPad View From XML
				final int[] buttonIds = new int[]{R.id.B1, R.id.B2, R.id.B3, R.id.B4};
				View dpad = (position.equals("left") ? left_dpad : right_dpad).inflate();
				NodeList buttons = element.getElementsByTagName("Button");
				for(int j = 0; j < buttons.getLength(); ++j) {
					Element button = (Element) buttons.item(j);
					String keycode = button.getAttribute("keycode");
					String display_name = button.getAttribute("name");
					buttonBindKey((Button)(dpad.findViewById(buttonIds[j])), keycode, display_name);
				}
			} else if(tag.equals("ButtonBar")) { //Generate ButtonBar View From XML
				LinearLayout bar = (position.equals("left") ? left_button_bar : right_button_bar);
				NodeList buttons = element.getElementsByTagName("Button");
				for(int j = 0; j < buttons.getLength(); ++j) {
					Element button = (Element) buttons.item(j);
					String keycode = button.getAttribute("keycode");
					String display_name = button.getAttribute("name");
					Button buttonView = new Button(this);
					buttonBindKey(buttonView, keycode, display_name);
					bar.addView(buttonView);
				}
			}
		}
		
		webview.getSettings().setPluginsEnabled(true);
		webview.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.loadUrl("file://" + LocalReposity.getInstance().getFullPath("data/" + flashGame.getId() + "/play.html"));
		webview.setKeepScreenOn(true);
	}
	
	private final void buttonBindKey(Button button, String keycode, String display_name) {
		int code = 0;
		if(keycode.equals("SPACE"))      code = KeyEvent.KEYCODE_SPACE;
		else if(keycode.equals("UP"))    code = KeyEvent.KEYCODE_DPAD_UP;
		else if(keycode.equals("DOWN"))  code = KeyEvent.KEYCODE_DPAD_DOWN;
		else if(keycode.equals("LEFT"))  code = KeyEvent.KEYCODE_DPAD_LEFT;
		else if(keycode.equals("RIGHT")) code = KeyEvent.KEYCODE_DPAD_RIGHT;
		else if(keycode.equals("SHIFT")) code = KeyEvent.KEYCODE_SHIFT_LEFT;
		else if(keycode.equals("ENTER")) code = KeyEvent.KEYCODE_ENTER;
		else if(keycode.length() == 1) {
		    char c = keycode.charAt(0);
		    if(c >= 'A' && c <= 'Z')
		    	code = KeyEvent.KEYCODE_A + c - 'A';
		    else if(c >= '0' && c <= '9')
		    	code = KeyEvent.KEYCODE_0 + c - '0';
		}
		
		final int final_code = code;
		
		button.setOnTouchListener(new OnTouchListener() {
			boolean shouldDown = false;
			TimerTask tt;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				
				if(event.getActionMasked()==MotionEvent.ACTION_DOWN)
				{
					dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, final_code));
					tt = new TimerTask() {
						
						@Override
						public void run() {
							shouldDown = true;
						}
					};
					new Timer().scheduleAtFixedRate(tt, 200, 200);
				}
				else if(event.getActionMasked()==MotionEvent.ACTION_UP) {
					dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, final_code));
					tt.cancel();
				}else if(event.getActionMasked()==MotionEvent.ACTION_MOVE && shouldDown) {
					dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, final_code));
					shouldDown=false;
				}
				return false;
			}
		});
		
		button.setText(display_name);
	}

}
