package org.kaola.fgbox.store;

import java.io.Serializable;
import java.util.List;

import org.kaola.fgbox.R;
import org.kaola.fgbox.model.Category;
import org.kaola.fgbox.model.FlashGame;
import org.kaola.fgbox.reposity.RemoteReposity;

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

public class CategoriesViewActivity extends LoaderActivity {
	
	private List<Category> categories;
	
	private final ListAdapter adapter = new BaseAdapter() {
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) convertView = View.inflate(parent.getContext(), R.layout.item_category, null);
			TextView t = (TextView) convertView.findViewById(R.id.TextView01);
			t.setText(categories.get(position).getName());
			ImageView i = (ImageView) convertView.findViewById(R.id.ImageView01);
			i.setImageBitmap(categories.get(position).getIcon());
			return convertView;
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public Category getItem(int position) {
			return categories == null ? null : categories.get(position);
		}
		
		@Override
		public int getCount() {
			return categories == null ? 0 : categories.size();
		}
	};
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				startActivity(new Intent(CategoriesViewActivity.this, CategoryViewActivity.class).putExtra("category", (Serializable)adapter.getItem(position)));
			}
		});
	}

	@Override
	protected boolean load() {
		List<Category> categories = RemoteReposity.getInstance().getCategories();
		
		if(categories == null)
			return false;
		/*
		for(Category category : categories)
			if(!category.loadIcon())
				return false;
		*/
		for(Category category : categories) {
			boolean loaded = false;
			for(int i = 0; i < 5 && !loaded && !Thread.interrupted(); ++i)
				loaded = category.loadIcon();
			if(!loaded)return false;
		}
		
		this.categories = categories;
		getListView().post(new Runnable() {
			
			@Override
			public void run() {
				getListView().setAdapter(adapter);
			}
		});
		return true;
	}
	
}
