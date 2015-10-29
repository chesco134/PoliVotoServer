package org.inspira.polivoto.Activity;

import org.inspira.polivotoserver.R;
import org.inspira.polivoto.Security.Hasher;

import DataBase.Votaciones;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class UnlockerActivity extends Activity {

	private EditText psswd;
	private boolean isChangePasswords;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.unlocker);
		psswd = (EditText) findViewById(R.id.psswd);
		Bundle extras = getIntent().getExtras();
		if( extras != null ){
			isChangePasswords = extras.getBoolean("isChangePasswords");
		}else{
			isChangePasswords = false;
		}
		findViewById(R.id.submit).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (new Votaciones(UnlockerActivity.this).consultaUsuario(
						"Administrador",
				new Hasher().makeHash(psswd.getText().toString())))
				{
					if(isChangePasswords){
						Intent i = new Intent(UnlockerActivity.this,SelectorDeCredenciales.class);
						i.putExtras(getIntent().getExtras());
						startActivity(i);
					}
					setResult(RESULT_OK);
					finish();
				}
			}
		});
	}
}
