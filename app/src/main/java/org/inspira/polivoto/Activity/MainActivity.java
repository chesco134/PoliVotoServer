package org.inspira.polivoto.Activity;

import org.inspira.polivotoserver.R;

import DataBase.Votaciones;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

	private static final int SPLASH = 001;
	private static final int SELECTOR_DE_CREDENCIALES = 319;
	private static final int WELCOME_ACTIVITY = 002;
	private static final int UNLOCKER_ACTIVITY = 003;
    private static final int DATA_PICKER_ACTIVITY = 004;
    private static final int FINISH_VOTING_PROCESS_REQUEST = 005;
    private static final int SELECTOR_DE_CREDENCIALES_UPDATE = 006;
    public static final int ZONA_FRAGMENT_ACTION = 007;
    public static final int PERFILES_ACTION = 8;
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
		((TextView)findViewById(R.id.Credenciales)).setTypeface(Typeface.createFromAsset(getAssets(), "RobotoCondensed-Bold.ttf"));
		((TextView)findViewById(R.id.Votaciones)).setTypeface(Typeface.createFromAsset(getAssets(), "RobotoCondensed-Bold.ttf"));
		((TextView)findViewById(R.id.Perfiles)).setTypeface(Typeface.createFromAsset(getAssets(), "RobotoCondensed-Bold.ttf"));
		((TextView)findViewById(R.id.Zonas)).setTypeface(Typeface.createFromAsset(getAssets(), "RobotoCondensed-Bold.ttf"));
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		if (isFirstLaunched) {
			launchSplash();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.settings) {
			settings();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			    case WELCOME_ACTIVITY:
					launchSelectorDeCredenciales();
					break;
                case SELECTOR_DE_CREDENCIALES:
                    launchDataPickerActivity();
					break;
                case DATA_PICKER_ACTIVITY:
                    launchVotacionesConf();
                    break;
			}
		} else {
			switch(requestCode){
				case UNLOCKER_ACTIVITY:
					finish();
					break;
                case DATA_PICKER_ACTIVITY:
                    finish();
                    break;
			}
		}
		if (requestCode == SPLASH) {
			existsAdmin = true;
            Votaciones db = new Votaciones(this);
			for (String usuarioActual : SelectorDeCredenciales.USUARIOS) {
				if (!db.revisaExistenciaDeCredencial(usuarioActual)) {
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
					return;
				}
			}
            if(!db.consultaEscuela() || !db.consultaPerfiles()){
                launchDataPickerActivity();
                return;
            }
			if (existsAdmin) {
                launchUnlockerActivity();
			}
		}
	}

    public void credenciales(View v){
        launchSelectorDeCredencialesUpdate();
    }

    public void votaciones(View v){
        launchVotacionesConf();
    }

    public void perfiles(View v){
        launchEditaPerfiles();
    }

    public void zonas(View v){
        launchZonaChanger();
    }

    public void settings(){
		Votaciones db = new Votaciones(this);
        /*
        for( String par : db.obtenerParticipantes())
            Log.d("TURARARARA",par);
        try {
            for (String st : db.consultaParticipantePreguntasTUL("2011640351", db.obtenerTituloVotacionActual()))
                Log.d("M;ASDMSAD", st);
        }catch(NullPointerException e){
            e.printStackTrace();
        }
        */
		launchPreferenceActivity();
		if(db.obtenerFechaInicioVotacionActual() == null)
			launchPreferenceActivity();
		else {
			Toast.makeText(this, "Ya hay un proceso de votación programado", Toast.LENGTH_SHORT).show();
		}
        /*
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
        */
    }

    private void launchVotacionesConf(){
        Intent i = new Intent(MainActivity.this,VotacionesConf.class);
        startActivity(i);
    }

    private void launchDataPickerActivity(){
        Intent i = new Intent(this,DataPickerActivity.class);
        startActivityForResult(i, DATA_PICKER_ACTIVITY);
    }
	
	private void launchUnlockerActivity(){
		Intent i = new Intent(this, UnlockerActivity.class);
		startActivityForResult(i, UNLOCKER_ACTIVITY);
	}
	
	private void launchSelectorDeCredenciales(){
		Intent i = new Intent(this, SelectorDeCredenciales.class);
		startActivityForResult(i, SELECTOR_DE_CREDENCIALES);
	}

    private void launchSelectorDeCredencialesUpdate(){
        Intent i = new Intent(this, SelectorDeCredenciales.class);
        i.putExtra("isChangePasswords",true);
        startActivityForResult(i, SELECTOR_DE_CREDENCIALES_UPDATE);
    }
	
	private void launchSplash(){
		Intent i = new Intent(this, SplashActivity.class);
		startActivityForResult(i, SPLASH);
		isFirstLaunched = false;
	}

    private void launchMensajeConfirmacion(String mensaje, boolean isChoice){
        Intent i = new Intent(this,Mensaje.class);
        i.putExtra("msj", mensaje);
        i.putExtra("isChoice", isChoice);
        startActivity(i);
    }

    private void launchZonaChanger(){
        Intent i = new Intent(this,CambiaZonaPerfilesActivity.class);
        i.putExtra("action",ZONA_FRAGMENT_ACTION);
        startActivity(i);
    }

    private void launchEditaPerfiles(){
        Intent i = new Intent(this,CambiaZonaPerfilesActivity.class);
        i.putExtra("action",PERFILES_ACTION);
        startActivity(i);
    }

    private void launchInputDateAndTimeValues(){
        Intent i = new Intent(this,InputDateAndTimeValuesActivity.class);
        startActivity(i);
    }

	private void launchPreferenceActivity(){
		Intent i = new Intent(this,ConfiguraParticipantesActivity.class);
		startActivity(i);
	}
}