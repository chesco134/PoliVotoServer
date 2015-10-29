package org.inspira.polivoto.Activity;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.ListIterator;

import org.inspira.capiz.NeoSuperChunk.SuperChunk;
import org.inspira.polivoto.Security.Cifrado;
import org.inspira.polivoto.Fragment.MainFragment;
import org.inspira.polivotoserver.MiServicio;
import org.inspira.polivoto.Adapter.MyFragmentStatePagerAdapter;
import org.inspira.polivotoserver.R;

import DataBase.Votaciones;
import Shared.Opcion;
import Shared.Pregunta;
import Shared.ResultadoVotacion;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class VotacionesConf extends AppCompatActivity implements
		ActionBar.TabListener {

	private Intent myService;
	private MyFragmentStatePagerAdapter adapter;
	private static LinkedList<MainFragment> fragmentitos;
	private SuperChunk superChunk;
	public static final String PARTICIPANTES_FILE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/participantes.csv";
	public static final String FILE_NAME = Environment.getExternalStorageDirectory().getAbsolutePath() + "/votaciones.conf";
	public static final String RESULTS_FILE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/resultados.polivoto";
	private static final int FINISH_VOTING_PROCESS_REQUEST = 134;
	private static final int DATA_LOADER = 5;
	private int counter = 0;

	private void launchMensajeConfirmacion(){
		Intent i = new Intent(this,Mensaje.class);
		i.putExtra("msj", getResources().getString(R.string.mensaje_alerta));
		i.putExtra("isChoice", true);
		startActivityForResult(i,FINISH_VOTING_PROCESS_REQUEST);
	}
	
	private void launchDataLoader(String label, Pregunta[] preguntas){
		String[] titulos = new String[preguntas.length];
		for(int i = 0; i < titulos.length; i++){
			titulos[i] = preguntas[i].titulo;
		}
		Bundle extras = new Bundle();
		extras.putString("label", label);
		extras.putStringArray("titulos", titulos);
		Intent dataLoader = new Intent(this,CargadorDeMatricula.class);
		dataLoader.putExtras(extras);
		//Toast.makeText(this, "Tul", Toast.LENGTH_LONG).show();
		startActivityForResult(dataLoader, DATA_LOADER);
	}

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			fragmentitos = new LinkedList<MainFragment>();
			for (int arg0 = 0; arg0 < 5; arg0++) {
				MainFragment newMainFragment = new MainFragment();
				Bundle args = new Bundle();
				args.putString("header", "");
				newMainFragment.setArguments(args);
				fragmentitos.add(newMainFragment);
			}
		}

		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		adapter = new MyFragmentStatePagerAdapter(getSupportFragmentManager());
		adapter.setPreguntas(fragmentitos);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(adapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < adapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(adapter.getPageTitle(i))
					.setTabListener(this));
		} 
		//new Votaciones(this).selectPendingVotes();
		myService = new Intent(this, MiServicio.class);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK){
			switch(requestCode){
			case FINISH_VOTING_PROCESS_REQUEST:
				if( data.getBooleanExtra("response", false) ){
					Votaciones v = new Votaciones(this);
					String[] rows;
					rows = v.consultaVoto();
					Log.d("Capiz","Tenemos " + rows.length + " votos.");
					for(int i = 0; i < rows.length; i++){
						Log.d("Capiz",rows[i]);
					}
					String[] participantes = v.consultaParticipantes();
					String[] votando = v.consultaVotando();
					Cifrado cipher = new Cifrado("MyPriceOfHistory");
					byte[][] votosCifrados = new byte[rows.length][];
					byte[][] participantesCifrados = new byte[participantes.length][];
					byte[][] votandoCifrados = new byte[votando.length][];
					for(int index = 0; index<rows.length; index++){
						votosCifrados[index] = cipher.cipher(rows[index]);
					}
					for(int index = 0; index<participantes.length; index++){
						participantesCifrados[index] = cipher.cipher(participantes[index]);
					}
					for(int index = 0; index<votando.length; index++){
						votandoCifrados[index] = cipher.cipher(votando[index]);
					}
					ResultadoVotacion resultadoFinalVotos = new ResultadoVotacion(votosCifrados);
					ResultadoVotacion resultadoFinalParticipantes = new ResultadoVotacion(participantesCifrados);
					ResultadoVotacion resultadoFinalVotando = new ResultadoVotacion(votandoCifrados);
					try{
						ObjectOutputStream salidaArchivo = new ObjectOutputStream(new FileOutputStream(RESULTS_FILE));
						salidaArchivo.writeObject(resultadoFinalVotos);
						salidaArchivo.writeObject(resultadoFinalParticipantes);
						salidaArchivo.writeObject(resultadoFinalVotando);
						salidaArchivo.close();
						Toast.makeText(this, "Éxito al finalizar la votación!", Toast.LENGTH_LONG).show();
					}catch(IOException ex){
						Toast.makeText(this, "Error al finalizar la votación:\n" + ex.toString(), Toast.LENGTH_LONG).show();
					}
					v.terminarVotaciones();
					stopService(myService);
				}
				break;
			case DATA_LOADER:
				startService(myService);
				Toast.makeText(this,"Servicio iniciado", Toast.LENGTH_SHORT)
				.show();
				break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		Intent i;
		Votaciones v;
		String[] rows;
		if (id == R.id.iniciar_servicio) {
			ListIterator<MainFragment> currentPregunta = fragmentitos
					.listIterator();
			LinkedList<Pregunta> pregs = new LinkedList<Pregunta>();
			String texto = new String();
			int count = 0;
			while (currentPregunta.hasNext()) {
				MainFragment mFragment = currentPregunta.next();
				String titulo = null;
				try {
					titulo = mFragment.getTitle().getText().toString();
				} catch (NullPointerException e) {
					titulo = "";
				}
				String op1 = null;
				try {
					op1 = mFragment.getTitle_option().getText().toString();
				} catch (NullPointerException e) {
					op1 = "";
				}
				String op2 = null;
				try {
					op2 = mFragment.getTitle_option_2().getText().toString();
				} catch (NullPointerException e) {
					op2 = "";
				}
				if (!titulo.equals("") && !op1.equals("") && !op2.equals("")) {
					texto = texto.concat("\n\n" + count + "\nTitulo:"+titulo+"\nopcion1:"+op1+"\nopcion2:"+op2);
					Pregunta pregunta = new Pregunta();
					LinkedList<Opcion> opciones = new LinkedList<Opcion>();
					Opcion opcion1 = new Opcion();
					opcion1.nombre = op1;
					opcion1.cantidad = 0;
					Opcion opcion2 = new Opcion();
					opcion2.nombre = op2;
					opcion2.cantidad = 0;
					opciones.add(opcion1);
					opciones.add(opcion2);
					ListIterator<View> currentRowView = mFragment
							.getAdditionalRows().listIterator();
					counter = 3;
					while (currentRowView.hasNext()) {
						Opcion opi = new Opcion();
						opi.nombre = ((EditText) currentRowView.next()
								.findViewById(R.id.set_title_option)).getText()
								.toString();
						opi.cantidad = 0;
						opciones.add(opi);
						texto = texto.concat("\nopcion" + counter++ + ":" + opi.nombre);
					}
					pregunta.opciones = opciones;
					pregunta.titulo = titulo;
					pregs.add(pregunta);
				}
				count++;
			}
			if (pregs.size() == 0) {
				Toast.makeText(this, "Error, al menos debes llenar una forma",
						Toast.LENGTH_SHORT).show();
			}else{
				superChunk = new SuperChunk(pregs);
				myService.putExtra("SuperChunk", superChunk);
				// startService(myService); Debes iniciar el servicio cuando la aplicación haya devuelto adecuadamente.
				launchDataLoader("Cargando matrícula", superChunk.getPreguntas().toArray(new Pregunta[0]));
				try{
					ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(FILE_NAME));
					salida.writeObject(superChunk);
					salida.close();
				}catch(IOException e){
					Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				}
			}
		} else if (id == R.id.action_last_server) {
			try{
				ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(FILE_NAME));
				superChunk = (SuperChunk)entrada.readObject();
				myService.putExtra("SuperChunk", superChunk);
				launchDataLoader("Cargando matrícula", superChunk.getPreguntas().toArray(new Pregunta[0]));
				//startService(myService); // Iniciar el servicio después de que la actividad haya terminado satisfactoriamente.
				//Toast.makeText(this,"Servicio Iniciado", Toast.LENGTH_SHORT)
				//.show();
				entrada.close();
			}catch(IOException | ClassNotFoundException e){
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			}
		} else if (id == R.id.detener_servicio) {
			if (stopService(myService))
				Toast.makeText(this, "Servicio Detenido", Toast.LENGTH_SHORT)
						.show();
			else
				Toast.makeText(this, "El servicio ya está detenido",
						Toast.LENGTH_SHORT).show();
		} else if (id == R.id.ver_participantes) {
			v = new Votaciones(this);
			rows = v.consultaVotando();
			File participantesFile = new File(PARTICIPANTES_FILE);
			DataOutputStream salPF = null;
			try {
				salPF = new DataOutputStream(new FileOutputStream(participantesFile));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try{
				for(int index=0; index<rows.length;index++)
						salPF.writeChars((index+1) + ", " +rows[index]+"\n");
				salPF.close();
			}catch(NullPointerException | IOException e){
				e.printStackTrace();
				Toast.makeText(this, "No pudimos escribir el archivo.", Toast.LENGTH_SHORT).show();
			}
			i = new Intent(this, SimplePromptActivity.class);
			i.putExtra("rows", rows);
			i.putExtra("header", "Participantes Registrados");
			startActivity(i);
		} else  if (id == R.id.finalizar_votacion) {
			try{
				FileInputStream fis = new FileInputStream(RESULTS_FILE);
				fis.close();
				Toast.makeText(this, "Ya hemos finalizado n.n", Toast.LENGTH_LONG).show();
			}catch(IOException e){
				launchMensajeConfirmacion();
			}
		}
		
		//else if(id == R.id.codigo_legalidad){
			//new Savior(this, myService).rescueRemoteServer();
		//}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager. 
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}
}