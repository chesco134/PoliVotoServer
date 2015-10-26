package org.inspira.polivotoserver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MenuListAdapter extends BaseAdapter{

	Activity activity;
	String[] rows;
	int contentType;
	
	public MenuListAdapter(Activity activity, String[] rows){
		this.activity = activity;
		this.rows = rows;
	}
	
	public void setContentType(int contentType){
		this.contentType = contentType;
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@SuppressLint("ViewHolder")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View rootView = null;
		int resourceID = 0;
		LayoutInflater inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if( convertView != null)
			return convertView;
		switch(contentType){
		case 0:
			resourceID = R.layout.fila_votacion;
			rootView = inflater.inflate(resourceID, parent, false);
			String[] contents = rows[position].split(",");
			((TextView)rootView.findViewById(R.id.titulo)).setText(contents[0]);
			((TextView)rootView.findViewById(R.id.fecha_inicio)).setText(contents[1]);
			((TextView)rootView.findViewById(R.id.fecha_fin)).setText(contents[2]);
			break;
		case 1:
			resourceID = R.layout.fila_zona;
			rootView = inflater.inflate(resourceID, parent, false);
			((TextView)rootView.findViewById(R.id.direccion)).setText(rows[position]);
			break;
		case 2:
			resourceID = R.layout.fila_perfil;
			rootView = inflater.inflate(resourceID, parent, false);
			((TextView)rootView.findViewById(R.id.perfil)).setText(rows[position]);
		default:
			resourceID = -1;
		}
		
		return rootView;
	}

}
