package org.inspira.polivotoserver.tul;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import org.inspira.polivotoserver.R;

public class PasswordPicker extends AppCompatActivity {
	
	private EditText psswd;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.password_picker_tul);
		psswd = (EditText)findViewById(R.id.password);
		psswd.setTypeface(Typeface.createFromAsset(getAssets(), "RobotoCondensed-Regular.ttf"));
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putString("psswd", psswd.getText().toString());
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		psswd.setText(savedInstanceState.getString("psswd"));
	}
	
	public void aceptar(View view){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = sharedPref.edit();
		editor.putString("psswd", psswd.getText().toString());
		editor.commit();
		finish();
	}

}
