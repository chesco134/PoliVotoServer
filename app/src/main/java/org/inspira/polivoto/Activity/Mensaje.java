package org.inspira.polivoto.Activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.inspira.polivotoserver.R;

public class Mensaje extends Activity {

	private static String msj;
    private EditText inputField;

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
		boolean isInputMethod = extras.getBoolean("isInputMethod");
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
			cancelar.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent();
                    i.putExtra("response", false);
                    setResult(RESULT_OK, i);
                    finish();
                }
            });
            buttonContainer.setGravity(Gravity.END);
            buttonContainer.addView(cancelar);
			buttonContainer.addView(aceptar);
		}else if(isInputMethod){
            setTitle("Título de Votación");
            ((LinearLayout)findViewById(R.id.main_container)).removeView(findViewById(R.id.texto));
            inputField = new EditText(this);
            inputField.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            Button aceptar = new Button(this);
            LinearLayout.LayoutParams aceptarLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            aceptarLP.setMargins(0, 20, 0, 20);
            aceptar.setLayoutParams(aceptarLP);
            aceptar.setText("Aceptar");
            aceptar.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
					if(!"".equals(inputField.getText().toString())) {
						Intent i = new Intent();
						i.putExtra("response", inputField.getText().toString());
						setResult(RESULT_OK, i);
						finish();
					}else{
						inputField.setHint("Escribe título");
					}
                }
            });
            LinearLayout buttonContainer = (LinearLayout)findViewById(R.id.button_container);
            buttonContainer.setGravity(Gravity.END);
            buttonContainer.setOrientation(LinearLayout.VERTICAL);
            buttonContainer.addView(inputField);
			buttonContainer.addView(aceptar);
		}else
			setTitle("Enhorabuena!!");
		TextView text = (TextView)findViewById(R.id.texto);
        if( text != null)
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
