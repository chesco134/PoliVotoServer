package org.inspira.polivotoserver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Mensaje extends Activity {

	private static String msj;
	
	@Override
	protected void onCreate(Bundle b){
		super.onCreate(b);
		setContentView(R.layout.mensaje);
		if( b == null ){
			Bundle ex = getIntent().getExtras();
			msj = ex.getString("msj");
		}
		Bundle extras = getIntent().getExtras();
		boolean isChoice = extras.getBoolean("isChoice");
		if( isChoice ){
			LinearLayout buttonContainer = (LinearLayout)findViewById(R.id.button_container);
			Button aceptar = new Button(this);
			aceptar.setText("Aceptar");
			Button cancelar = new Button(this);
			cancelar.setText("Cancelar");
			aceptar.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View view){
					Intent i = new Intent();
					i.putExtra("response", true);
					setResult(RESULT_OK,i);
					finish();
				}
			});
			cancelar.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View view){
					Intent i = new Intent();
					i.putExtra("response", false);
					setResult(RESULT_OK,i);
					finish();
				}
			});
			buttonContainer.addView(aceptar);
			buttonContainer.addView(cancelar);
		}
		TextView text = (TextView)findViewById(R.id.texto);
		text.setText(msj);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle b){
		super.onSaveInstanceState(b);
		b.putString("msj", msj);
	}
	@Override
	protected void onRestoreInstanceState(Bundle b){
		super.onRestoreInstanceState(b);
		msj = b.getString("msj");
	}
}
