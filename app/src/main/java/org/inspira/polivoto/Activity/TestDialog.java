package org.inspira.polivoto.Activity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.inspira.polivotoserver.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class TestDialog extends Activity {

	private String serverHostAddress;
	private Socket socket;
	private Thread connectToServer;
	private boolean isTestParticipante;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wait_view);
		if (savedInstanceState == null) {
			Bundle extras = getIntent().getExtras();
			serverHostAddress = extras.getString("serverHostAddress");
			isTestParticipante = extras.getBoolean("isTestParticipante", false);
			connectToServer = new Thread() {

				@Override
				public void run() {
					try {
						socket = new Socket();
						socket.connect(new InetSocketAddress(
								serverHostAddress, 23543), 5000);
						if( isTestParticipante ){
							String[] datos = {"Prueba","","Prueba"};
							ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
							salida.writeObject(datos);
							ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
							String participanteHost = entrada.readUTF();
							Intent i = new Intent();
							i.putExtra("participanteHost", participanteHost);
							setResult(RESULT_OK,i);
							finish();
						}
						socket.close();
						setResult(RESULT_OK);
						finish();
					} catch (IOException e) {
						Intent i = new Intent();
						i.putExtra("error", e.toString());
						setResult(RESULT_CANCELED, i);
						finish();
					}
				}
			};
		} else
			serverHostAddress = savedInstanceState
					.getString("serverHostAddress");
		connectToServer.start();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("serverHostAddress", serverHostAddress);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		serverHostAddress = savedInstanceState.getString("serverHostAddress");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (connectToServer != null) {
			connectToServer.interrupt();
		} else {
			Toast.makeText(this, "Ni siquiera se conect�.", Toast.LENGTH_SHORT)
					.show();
		}
	}

	/*
	 * 
	 * MiServicio mService; boolean mBound = false;
	 * 
	 * /** Defines callbacks for service binding, passed to bindService() //
	 * private ServiceConnection mConnection = new ServiceConnection() {
	 * 
	 * @Override public void onServiceConnected(ComponentName className, IBinder
	 * service) { // We've bound to LocalService, cast the IBinder and get
	 * LocalService instance LocalBinder binder = (LocalBinder) service;
	 * mService = binder.getService(); mBound = true; }
	 * 
	 * @Override public void onServiceDisconnected(ComponentName arg0) { mBound
	 * = false; } };
	 * 
	 * @Override protected void onCreate(Bundle b){ super.onCreate(b);
	 * setContentView(R.layout.wait_view); /* TextView text = new
	 * TextView(this); text.setText(getIntent().getExtras().getString("param"));
	 * setContentView(text); // }
	 * 
	 * @Override protected void onStart() { super.onStart(); // Bind to
	 * LocalService Intent intent = new Intent(this, MiServicio.class);
	 * bindService(intent, mConnection, Context.BIND_AUTO_CREATE); }
	 * 
	 * @Override protected void onResume(){ super.onResume(); if (mBound) { //
	 * Call a method from the LocalService. // However, if this call were
	 * something that might hang, then this request should // occur in a
	 * separate thread to avoid slowing down the activity performance. Thread t
	 * = new Thread(){
	 * 
	 * @Override public void run(){ mService.hasFinished(); finish(); } };
	 * t.start(); } }
	 * 
	 * 
	 * @Override protected void onStop() { super.onStop(); // Unbind from the
	 * service if (mBound) { unbindService(mConnection); mBound = false; } }
	 */
}
