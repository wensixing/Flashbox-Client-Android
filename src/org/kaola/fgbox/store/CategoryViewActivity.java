package org.kaola.fgbox.store;

import java.util.List;

import org.kaola.fgbox.R;
import org.kaola.fgbox.model.Category;
import org.kaola.fgbox.model.FlashGame;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class CategoryViewActivity extends LoaderActivity {
	
	private Category category;
	private List<FlashGame> flashgames;
	
	private final ListAdapter adapter = new BaseAdapter() {
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) convertView = View.inflate(parent.getContext(), R.layout.item_category, null);
			TextView t = (TextView) convertView.findViewById(R.id.TextView01);
			t.setText(flashgames.get(position).getName());
			ImageView i = (ImageView) convertView.findViewById(R.id.ImageView01);
			i.setImageBitmap(flashgames.get(position).getIcon());
			return convertView;
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public FlashGame getItem(int position) {
			return flashgames == null ? null : flashgames.get(position);
		}
		
		@Override
		public int getCount() {
			return flashgames == null ? 0 : flashgames.size();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		category = (Category) getIntent().getSerializableExtra("category");
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				startActivity(new Intent(CategoryViewActivity.this, GameViewActivity.class).putExtra("flashGame", (FlashGame)adapter.getItem(position)));
			}
		});
	}
	
	protected boolean load() {
		List<FlashGame> flashgames = category.getGames();
		if(flashgames == null)return false;
		
		/*
		for(FlashGame flashgame : flashgames)
			if(!flashgame.loadIcon())
				return false;
		*/
		for(FlashGame flashgame : flashgames) {
			boolean loaded = false;
			for(int i = 0; i < 5 && !loaded && !Thread.interrupted(); ++i)
				loaded = flashgame.loadIcon();
			if(!loaded)return false;
		}
		
		this.flashgames = flashgames;
		getListView().post(new Runnable() {
			
			@Override
			public void run() {
				getListView().setAdapter(adapter);
			}
		});
		return true;
	}

}
