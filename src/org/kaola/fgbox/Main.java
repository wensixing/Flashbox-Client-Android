package org.kaola.fgbox;

import java.util.LinkedList;
import java.util.List;

import org.kaola.fgbox.model.FlashGame;
import org.kaola.fgbox.reposity.LocalReposity;
import org.kaola.fgbox.store.CategoriesViewActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Main extends Activity {
	
	private Button       gameStoreView;
	private ListView     listView;
	private View         noGameView;
	private GamesAdapter adapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_center);
        
        gameStoreView = (Button) findViewById(R.id.game_store);
        gameStoreView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Main.this, CategoriesViewActivity.class));
			}
		});
        
        listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				play(adapter.getItem(position));
			}
		});
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				new AlertDialog.Builder(Main.this)
					.setTitle(R.string.main_on_long_click)
					.setItems(R.array.main_on_long_click, new OnClickDialogMenu(adapter.getItem(position)))
					.show();
				return false;
			}
		});
        
        noGameView = findViewById(R.id.no_game);
        
        adapter = new GamesAdapter();
        
    }
    
    @Override
    protected void onResume() {
    	refresh();
    	super.onResume();
    }
    
    private class GamesAdapter extends BaseAdapter {
    	
    	private List<FlashGame> games = new LinkedList<FlashGame>();
    	
    	public void load() {
    		games = LocalReposity.getInstance().getGames();
    	}

		@Override
		public int getCount() {
			return games.size();
		}

		@Override
		public FlashGame getItem(int position) {
			return games.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) convertView = View.inflate(parent.getContext(), R.layout.item_category, null);
			TextView tv = (TextView) convertView.findViewById(R.id.TextView01);
			tv.setText(getItem(position).getName());
			ImageView img = (ImageView) convertView.findViewById(R.id.ImageView01);
			getItem(position).loadIcon();
			img.setImageBitmap(getItem(position).getIcon());
			return convertView;
		}
    	
    }
    
    private class OnClickDialogMenu implements android.content.DialogInterface.OnClickListener {
    	
    	private FlashGame flashGame;
    	
    	public OnClickDialogMenu(FlashGame flashGame) {
    		this.flashGame = flashGame;
    	}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch(which) {
			case 0:
				play(flashGame);
				break;
			case 1:
				FlashGame.remove(flashGame.getId());
				refresh();
		        break;
			}
		}
    	
    }
    
    private void play(FlashGame flashGame) {
    	startActivity(new Intent(this, FlashActivity.class).putExtra("flashGame", new FlashGame(flashGame.getId(), flashGame.getName())));
    }
    
    private void refresh() {
    	adapter.load();
    	if(adapter.getCount() == 0)
    		noGameView.setVisibility(View.VISIBLE);
    	else
    		noGameView.setVisibility(View.INVISIBLE);
        listView.setAdapter(adapter);  
    }
}