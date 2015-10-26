package org.inspira.polivoto.activities;

import org.inspira.polivotoserver.R;
import org.inspira.polivotoserver.SHAExample;

import DataBase.Votaciones;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WelcomeActivity extends AppCompatActivity {

	private String usuario;
	private String psswd;
	private String psswd2;
	private boolean success = false;

	private EditText psswdInput;
	private EditText psswdInput2;
	//private ImageView submit;
	private Button submit;
	private TextView error;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.password_picker);
		if (savedInstanceState == null) {
			usuario = getIntent().getExtras().getString("usuario");
		} else {
			usuario = savedInstanceState.getString("usuario");
		}
		psswdInput = (EditText) findViewById(R.id.psswd1);
		psswdInput2 = (EditText) findViewById(R.id.psswd2);
		submit = (Button) findViewById(R.id.next);
		//submit = (ImageView) findViewById(R.id.next);
		error = (TextView) findViewById(R.id.mensaje_de_error);
		submit.setTypeface(Typeface.createFromAsset(getAssets(), "RobotoCondensed-Regular.ttf"));
		error.setTypeface(Typeface.createFromAsset(getAssets(),"Roboto-Regular.ttf"));
		((TextView)findViewById(R.id.first_time_tag)).setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf"));
		((TextView)findViewById(R.id.greeting_tag)).setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Thin.ttf"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcome_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.next) {
			next(null);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("success", success);
		outState.putString("psswd", psswd);
		outState.putString("psswd2", psswd2);
		outState.putString("usuario", usuario);
		outState.putString("error", error.getText().toString());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		success = savedInstanceState.getBoolean("success");
		psswd = savedInstanceState.getString("psswd");
		psswd2 = savedInstanceState.getString("psswd2");
		usuario = savedInstanceState.getString("usuario");
		error.setText(savedInstanceState.getString("error"));
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		if (success) {
			setResult(RESULT_OK);
			finish();
		}
	}
	
	public void next(View view){
		String psswd = psswdInput.getText().toString();
		String psswd2 = psswdInput2.getText().toString();
		if (psswd.length() > 5 && psswd2.length() > 5) {
			if (!psswd.equals(psswd2)) {
				error.setText(R.string.psswd_missmatch);
				error.setTextColor(Color.RED);
				success = false;
			} else {
				error.setText("");
				WelcomeActivity.this.psswd = psswd;
				WelcomeActivity.this.psswd2 = psswd2;
				//submit.setTextColor(Color.BLUE);
				success = true;
				Votaciones bd = new Votaciones(WelcomeActivity.this);
				bd.altaZonaVoto("Local", 0, 0);
				if (bd.insertKey(new SHAExample().makeHash(psswd), usuario,
						"Local")) {
					Toast.makeText(WelcomeActivity.this,
							R.string.keys_insert_success,
							Toast.LENGTH_SHORT).show();
					setResult(RESULT_OK);
					finish();
				} else {
					error.setTextColor(Color.RED);
					error.setText(R.string.keys_insert_error);
				}
			}
		} else {
			error.setTextColor(Color.RED);
			error.setText(R.string.psswd_length_error);
		}
	}
}
