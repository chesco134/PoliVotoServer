package org.inspira.polivoto.Activity;

import java.util.Vector;

import org.inspira.polivoto.Networking.HelloWebService;
import org.inspira.polivotoserver.R;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class CargarPasswordActivity extends Activity{

	EditText inputEscuela;
	Button login;
	String nombreUsuario;
	String usrPsswd;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.carga_password);
		((TextView)findViewById(R.id.input_usr_label)).setTypeface(Typeface.createFromAsset(getAssets(), "Roboto-Thin.ttf"));
		inputEscuela = (EditText)findViewById(R.id.input_psswd);
		login = (Button)findViewById(R.id.logIn);
		login.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				usrPsswd = inputEscuela.getText().toString();
				String[] args = {usrPsswd};
				HandleNetworking networker = new HandleNetworking();
				networker.execute(args);
			}
		});
	}
	
	private class HandleNetworking extends AsyncTask<String,String,String>{
		
		@Override
		protected String doInBackground(String... args){
			String result = null;
			String[] publish = null;
			HelloWebService webService = new HelloWebService();
			Vector<String> response = webService.loadPsswd("root","Chesco134", usrPsswd);
			if (response != null){
				try{
					Object[] resps = response.toArray();
					String[] resultados = new String[resps.length];
					for(int i=0; i<resultados.length; i++){
						resultados[i] = resps[i].toString();
					}
					publish = resultados;
				}catch(ClassCastException e){
					publish = new String[1];
					publish[0] = "Error en el formato de los datos.";
				}
			}else{
				publish = new String[1];
				publish[0] = "Error de conexión.";
			}
			publishProgress(publish);
			return result;
		}
		
		@Override
		protected void onProgressUpdate(String... args){
			for(int i = 0; i<args.length; i++){
			//	launchAlert(args[i]);
			}
		}
	}
}
