package org.inspira.polivoto.Activity;

import org.inspira.polivotoserver.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListaContenido extends Activity{

	private ListView list;
	private String[] rows;
	
	 @Override
	 protected void onCreate(Bundle savedInstanceState){
		 super.onCreate(savedInstanceState);
		 setContentView(R.layout.lista_contenido);
		 list = (ListView)findViewById(R.id.lista_elementos);
		 try{
			 rows = getIntent().getExtras().getStringArray("list");
		}catch(NullPointerException e){
			 rows = new String[2];
			 rows[0] = "hello";
			 rows[1] = "world";
			 list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rows));
		 }
		 list.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
			}
		 });
	 }
}
