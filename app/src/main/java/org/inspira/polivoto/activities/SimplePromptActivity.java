package org.inspira.polivoto.activities;

import org.inspira.polivotoserver.MyListAdapter;
import org.inspira.polivotoserver.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SimplePromptActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_view);
		try{
			Bundle extras = getIntent().getExtras();
			String[] rows = extras.getStringArray("rows");
			if( rows == null ){
				rows = new String[2];
				rows[0] = "Hola";
				rows[1] = "Mundo";
			}
			((TextView)findViewById(R.id.header)).setText(extras.getString("header"));
			ListView list = (ListView)findViewById(R.id.usrs_list);
			MyListAdapter adapter = new MyListAdapter(rows, this);
			list.setAdapter(adapter);
		}catch(Exception e){
			Toast.makeText(this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
			finish();
		}
	}
}