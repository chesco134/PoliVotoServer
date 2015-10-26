package org.inspira.polivoto.activities;

import org.inspira.polivotoserver.R;

import DataBase.Votaciones;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class CargadorDeMatricula extends Activity{
	
	private String[] titulos;
	
	private TextView label;
	
	private class DatasetHandler extends Thread{
		
		private Activity activity;
		
		public DatasetHandler(Activity activity){
			this.activity = activity;
		}
		
		@Override
		public void run(){
			Votaciones bd = new Votaciones(activity);
			if( !bd.checkMatricula() )
				bd.insertaRegistro(titulos);
			setResult(RESULT_OK);
			finish();
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sala_de_espera);
		label = (TextView)findViewById(R.id.label);
		if( savedInstanceState == null ){
			Bundle extras = getIntent().getExtras();
			label.setText(
					extras.getString("label")
			);
			titulos = extras.getStringArray("titulos");
			DatasetHandler dataHandler = new DatasetHandler(this);
			dataHandler.start();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putString("label", label.getText().toString());
		outState.putStringArray("titulos",titulos);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		label.setText(savedInstanceState.getString("label"));
		titulos = savedInstanceState.getStringArray("titulos");
	}
}