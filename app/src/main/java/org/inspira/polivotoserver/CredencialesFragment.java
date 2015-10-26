package org.inspira.polivotoserver;

import DataBase.Votaciones;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CredencialesFragment extends Fragment{

	private SelectorDeCredenciales activity;
	
	private String usuario;
	private String psswd;
	private String psswd2;
	private String zona;
	private boolean isChangePasswords;
	private boolean success = false;
	
	private EditText psswdInput;
	private EditText psswdInput2;
	private TextView label1;
	private TextView label2;
	private Button submit;
	private TextView error;
	
	private Context context;

	public boolean hasSucceded(){
		return success;
	}
	
	public void notificaCredencialesFaltantes(String[] cFaltantes){
		if( cFaltantes != null){
			error.setTextColor(Color.GREEN);
			error.setText("HECHO");
		}
	}
	
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		this.activity = (SelectorDeCredenciales) activity;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
		View rootView = inflater.inflate(R.layout.escribe_credenciales_usuario, parent, false);
		context = inflater.getContext();
		usuario = getArguments().getString("usuario");
		isChangePasswords = getArguments().getBoolean("isChangePasswords");
		zona = getArguments().getString("zona");
		label1 = (TextView)rootView.findViewById(R.id.first_time_tag);
		label2 = (TextView)rootView.findViewById(R.id.confirmar_contraseña);
		psswdInput = (EditText)rootView.findViewById(R.id.psswd1);
		psswdInput2 = (EditText)rootView.findViewById(R.id.psswd2);
		submit = (Button)rootView.findViewById(R.id.confirmar);
		submit.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),"RobotoCondensed-Regular.ttf"));
		error = (TextView)rootView.findViewById(R.id.mensaje_de_error);
		if( isChangePasswords ){
			label1.setText("Escriba antigua contraseña:");
			label2.setText("Escriba nueva contraseña:");
		}
		submit.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				String psswd = psswdInput.getText().toString();
				String psswd2 = psswdInput2.getText().toString();
				if( psswd.length() > 5 && psswd2.length() > 5 ){
					if(isChangePasswords){
						Votaciones db = new Votaciones(context); 
						if( db.consultKey(new SHAExample().makeHash(psswd), usuario)){
							if(db.updateKey(psswd, psswd2, usuario, zona)){
								Toast.makeText(context,"Credenciales actualizadas con éxito.",Toast.LENGTH_SHORT).show();
								psswdInput.setText("");
								psswdInput2.setText("");
								error.setTextColor(Color.BLUE);
								error.setText("Credenciales actualizadas.");
							}
						}else{
							error.setText("Contraseña previa incorrecta.");
							error.setTextColor(Color.RED);
							success = false;
						}
					}else{
						if( !psswd.equals(psswd2) ){
							error.setText("Las contraseñas no coinciden.");
							error.setTextColor(Color.RED);
							success = false;
						}else{
							error.setText("");
							CredencialesFragment.this.psswd = psswd;
							CredencialesFragment.this.psswd2 = psswd2;
							success = true;
							if (activity.guardarCredencial(usuario, psswd)){
								Toast.makeText(context,"Credenciales actualizadas con éxito.",Toast.LENGTH_SHORT).show();
								notificaCredencialesFaltantes(activity.checkProgress());
								SelectorDeCredenciales act = (SelectorDeCredenciales)getActivity();
								int position = act.getCurrentTabPosition();
								if(position == 3)
									act.changeTab(position-1);
								else
									act.changeTab(position+1);	
							}else{
								success = false;
								error.setTextColor(Color.RED);
								error.setText("Error al actualizar credenciales.");
							}
						}
					}
				}else{
					error.setTextColor(Color.RED);
					error.setText("Error, la longitud de las contraseñas debe ser de más de 5 caracteres.");
				}
			}
		});
		if( savedInstanceState != null ){
			success = savedInstanceState.getBoolean("success");
			psswd = savedInstanceState.getString("psswd");
			psswd2 = savedInstanceState.getString("psswd2");
			error.setText(savedInstanceState.getString("error"));
			label1.setText(savedInstanceState.getString("label1"));
			label2.setText(savedInstanceState.getString("label2"));
		}else{
			success = activity.compruebaCredencial(usuario);
		}
		if(getArguments().getBoolean("isChangePasswords")){
			success = false;
		}
		return rootView;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		if( success ){
			notificaCredencialesFaltantes(activity.checkProgress());
			submit.setEnabled(false);
		}else{
			submit.setEnabled(true);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putBoolean("success",success);
		outState.putString("psswd", psswd);
		outState.putString("psswd2", psswd2);
		outState.putString("error", error.getText().toString());
		outState.putString("label1", label1.getText().toString());
		outState.putString("label2", label2.getText().toString());
	}
}
