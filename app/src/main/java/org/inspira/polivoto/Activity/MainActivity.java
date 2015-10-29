package org.inspira.polivoto.Activity;

import org.inspira.polivotoserver.R;

import DataBase.Votaciones;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends AppCompatActivity {

	private static final int SPLASH = 001;
	private static final int SELECTOR_DE_CREDENCIALES = 319;
	private static final int WELCOME_ACTIVITY = 002;
	private static final int UNLOCKER_ACTIVITY = 003;
	private boolean existsAdmin = false;
	private static boolean isFirstLaunched;

	/*
	private ClickHandler clicker;
	private HintLauncher hinter;

	private class ClickHandler implements OnClickListener {

		Map<String,Intent> buttons;
		MainActivity activity;
		
		public ClickHandler(){
			buttons = new TreeMap<String, Intent>();
			Intent unlockActivity = new Intent(activity, UnlockerActivity.class);
			Bundle extras = new Bundle();
			extras.putBoolean("isChangePasswords", true);
			unlockActivity.putExtras(extras);
			Intent votaciones = new Intent(activity, VotacionesConf.class);
			
		}
		
		@Override
		public void onClick(View view) {

		}
	}

	private class HintLauncher implements OnClickListener {

		String hint;

		public void setHint(String hint) {
			this.hint = hint;
		}

		@Override
		public void onClick(View view) {
			launchHint(hint);
		}
	}
	
	private void launchHint(String hint) {
		Intent i = new Intent(this, MensajeInformacion.class);
		i.putExtra("hint", hint);
		startActivity(i);
	}
	 */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu);
		// hinter = new HintLauncher(hint);
		if (savedInstanceState == null) {
			isFirstLaunched = true;
		}
		(findViewById(R.id.Votaciones)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				Intent i = new Intent(MainActivity.this,VotacionesConf.class);
				startActivity(i);
			}
		});
		(findViewById(R.id.Participantes)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				new Thread(){
					@Override
					public void run(){
						String[] rows = new Votaciones(MainActivity.this).obtenerParticipantes();
						Bundle extras = new Bundle();
						extras.putStringArray("list", rows);
						Intent i = new Intent(MainActivity.this,ListaContenido.class);
						i.putExtras(extras);
						startActivity(i);
					} 
				}.start();
			}
		});

	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		if (isFirstLaunched) {
			launchSplash();
		}
	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case WELCOME_ACTIVITY:
				launchSelectorDeCredenciales();
					break;
				case SELECTOR_DE_CREDENCIALES:

					break;
			}
		} else {
			switch (requestCode) {
			case SELECTOR_DE_CREDENCIALES:
				launchSelectorDeCredenciales();
				break;
			case WELCOME_ACTIVITY:
				finish();
				break;
			case UNLOCKER_ACTIVITY:
				launchUnlockerActivity();
				break;
			}
		}
		if (requestCode == SPLASH) {
			existsAdmin = true;
			for (String usuarioActual : SelectorDeCredenciales.USUARIOS) {
				if (!new Votaciones(this)
						.revisaExistenciaDeCredencial(usuarioActual)) {
					Intent i;
					if (usuarioActual.equals("Administrador")) {
						i = new Intent(this, WelcomeActivity.class);
						Bundle extras = new Bundle();
						extras.putString("usuario", usuarioActual);
						i.putExtras(extras);
						existsAdmin = false;
						startActivityForResult(i, WELCOME_ACTIVITY);
					} else {
						i = new Intent(this, SelectorDeCredenciales.class);
						Bundle extras = new Bundle();
						extras.putString("zona", "Local");
						i.putExtras(extras);
						startActivityForResult(i, SELECTOR_DE_CREDENCIALES);
					}
					break;
				}
			}
			if (existsAdmin) {
				Intent unlockerActivity = new Intent(this,
						UnlockerActivity.class);
				startActivityForResult(unlockerActivity, UNLOCKER_ACTIVITY);
			}
		}
	}
	
	private void launchUnlockerActivity(){
		Intent i = new Intent(this, UnlockerActivity.class);
		startActivityForResult(i, UNLOCKER_ACTIVITY);
	}
	
	private void launchSelectorDeCredenciales(){
		Intent i = new Intent(this, SelectorDeCredenciales.class);
		startActivityForResult(i, SELECTOR_DE_CREDENCIALES);
	}
	
	private void launchSplash(){
		Intent i = new Intent(this, SplashActivity.class);
		startActivityForResult(i, SPLASH);
		isFirstLaunched = false;
	}
}