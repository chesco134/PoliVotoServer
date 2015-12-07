package org.inspira.polivoto.Activity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.ListIterator;

import org.inspira.capiz.NeoSuperChunk.SuperChunk;
import org.inspira.polivoto.Fragment.MainFragment;
import org.inspira.polivoto.Security.Cifrado;
import org.inspira.polivoto.Threading.RegistraVotacionGlobal;
import org.inspira.polivoto.Threading.TerminaVotacionGlobal;
import org.inspira.polivoto.Threading.TerminaVotacionLocal;
import org.inspira.polivotoserver.MiServicio;
import org.inspira.polivoto.Adapter.MyFragmentStatePagerAdapter;
import org.inspira.polivotoserver.R;

import DataBase.Votaciones;
import Shared.Opcion;
import Shared.Pregunta;
import Shared.ResultadoVotacion;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
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

    private static final int STAND_BY = 128;
    private static final int REQUEST_GO_GLOBAL = 328;
    private Intent myService;
    public MiServicio service;
	private MyFragmentStatePagerAdapter adapter;
	private static LinkedList<MainFragment> fragmentitos;
    private LinkedList<Pregunta> pregs;
    private SuperChunk superChunk;
	// We need to assign the file name entered in the settings.
	public static final String PARTICIPANTES_FILE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/settings.csv";
	public static final String FILE_NAME = Environment.getExternalStorageDirectory().getAbsolutePath() + "/votaciones.conf";
	public static final String RESULTS_FILE = Environment.getExternalStorageDirectory().getAbsolutePath() + "/resultados.polivoto";
	public static final int FREE_CAMPAIGN = 703;
	private static final int FINISH_VOTING_PROCESS_REQUEST = 134;
	private static final int DATA_LOADER = 5;
    private static final int STARTING_SERVICE_FOR_START_DATE = 47;
    private static final int STARTING_SERVICE = 48;
    private String fechaInicioVotacion;
    private String fechaFinVotacion;
	private int counter = 0;
    public boolean isServiceBound;

    /*
    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d("TAG", className.toString() + " service is bound");
            isServiceBound = true;
            service = ((MiServicio.LocalBinder) binder).getService();
            service.setContext(VotacionesConf.this);
            Log.d("TAG", "Starting live data");
            try {
                service.startService();
                if (preRequisites)
                    btStatusTextView.setText(getString(R.string.status_bluetooth_connected));
            } catch (IOException ioe) {
                Log.e(TAG, "Failure Starting live data");
                btStatusTextView.setText(getString(R.string.status_bluetooth_error_connecting));
                doUnbindService();
            }
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        // This method is *only* called when the connection to the service is lost unexpectedly
        // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
        // So the isServiceBound attribute should also be set to false when we unbind from the service.
        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, className.toString() + " service is unbound");
            isServiceBound = false;
        }
    };
    */

	private void launchMensajeConfirmacion(String msj, boolean isChoice, int flag){
		Intent i = new Intent(this,Mensaje.class);
		i.putExtra("msj", msj);// getResources().getString(R.string.mensaje_alerta));
		i.putExtra("isChoice", isChoice);
		startActivityForResult(i,flag);
	}
	
	private void launchDataLoader(String label){
        Votaciones v = new Votaciones(this);
		Bundle extras = new Bundle();
		extras.putString("label", label);
		extras.putStringArray("titulos", v.obtenerPreguntasVotacion(v.obtenerTituloVotacionActual()));
		Intent dataLoader = new Intent(this,CargadorDeMatricula.class);
		dataLoader.putExtras(extras);
		//Toast.makeText(this, "Tul", Toast.LENGTH_LONG).show();
		startActivityForResult(dataLoader, DATA_LOADER);
	}

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString("fechaInicioVotacion", fechaInicioVotacion);
        outState.putString("fechaFinVotacion", fechaFinVotacion);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        fechaInicioVotacion = savedInstanceState.getString("fechaInicioVotacion");
        fechaFinVotacion = savedInstanceState.getString("fechaFinVotacion");
    }

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (fragmentitos == null || savedInstanceState == null) {
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
        Votaciones v = new Votaciones(this);
        Log.d("Loquito", "PREGUNTAS");
        if(v.obtenerTituloVotacionActual() != null)
            for(String itr : v.consultaVotando(v.obtenerTituloVotacionActual()))
                v.obtenerResultadosPorPregunta(itr,v.obtenerIdVotacionFromPregunta(itr));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
        Votaciones v = new Votaciones(this);
		if(resultCode == RESULT_OK){
			switch(requestCode){
            case REQUEST_GO_GLOBAL:
                if( data.getBooleanExtra("response", false) ) {
                    launchStandByActivity("Sincronizando...");
                    new RegistraVotacionGlobal(this, v.grabLastUserIdAttmptSucceded("Consultor")).execute();
                }
                break;
			case FINISH_VOTING_PROCESS_REQUEST:
                if( data.getBooleanExtra("response", false) ) {
                    if (v.isVotacionActualGlobal()) {
                        launchStandByActivity("Terminando votación");
                        new TerminaVotacionGlobal(this).execute();
                    } else {
                        launchStandByActivity("Terminando votación");
                        new TerminaVotacionLocal(this).execute();
                    }
                }
				break;
			case DATA_LOADER:
				startService(myService);
				Toast.makeText(this,"Servicio iniciado", Toast.LENGTH_SHORT)
				.show();
                if(!v.existeLoginAttemptAdmin()) {
                    int id = v.insertaLoginAttempt("Administrador", "localhost");
                    v.insertaAttemptSucceded(id,new byte[]{(byte)(1&0xFF)});
                }
				break;
            case STARTING_SERVICE_FOR_START_DATE:
                /** Aún no está validado el campo de fecha de inicio **/
                int year = data.getExtras().getInt("year");
                int month = data.getExtras().getInt("month");
                int day = data.getExtras().getInt("day");
                int hourOfDay = data.getExtras().getInt("hourOfDay");
                int minute = data.getExtras().getInt("minute");
                fechaInicioVotacion = day + "/" + month + "/" + year + ", " +
                        hourOfDay + ":" + minute;
                launchIniciaVotacion(getString(R.string.define_fecha_fin_votacion),STARTING_SERVICE,false);
                break;
            case STARTING_SERVICE:
                String titVotacion = v.obtenerTituloVotacionActual();
                for(Pregunta preg : pregs){
                    v.insertaPregunta(preg.titulo,titVotacion);
                    for(Opcion op : preg.opciones){
                        v.insertaOpcion(op.nombre);
                        v.insertaPreguntaOpcion(preg.titulo,op.nombre);
                    }
                }
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                Boolean usarMatricula = sharedPref.getBoolean(ConfiguraParticipantesActivity.USAR_MATRICULA_KEY, false);
                if(!usarMatricula)
                    startService(myService); // Debes iniciar el servicio cuando la aplicación haya devuelto adecuadamente.
                else
                    launchDataLoader("Cargando matrícula");
                break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.votaciones_conf_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		Intent i;
		Votaciones v = new Votaciones(this);
		if (id == R.id.iniciar_servicio) {
            if(v.obtenerFechaInicioVotacionActual() == null) {
                ListIterator<MainFragment> currentPregunta = fragmentitos
                        .listIterator();
                pregs = new LinkedList<Pregunta>();
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
                        /*
                        db.insertaPregunta(titulo,titVotacion);
                        db.insertaOpcion(op1);
                        db.insertaPreguntaOpcion(titulo,op1);
                        db.insertaOpcion(op2);
                        db.insertaPreguntaOpcion(titulo,op2);
                        */
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
                            /*
                            db.insertaOpcion(opi.nombre);
                            db.insertaPreguntaOpcion(titulo,opi.nombre);
                            */
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
                    // db.revertLastVotacion();
                }else{
                    superChunk = new SuperChunk(pregs);
                    myService.putExtra("SuperChunk", superChunk);
                    myService.putExtra("operating_mode",FREE_CAMPAIGN);
                    launchIniciaVotacion(getString(R.string.define_fecha_inicio_votacion), STARTING_SERVICE_FOR_START_DATE, true);
                    try{
                        ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(FILE_NAME));
                        salida.writeObject(superChunk);
                        salida.close();
                        Toast.makeText(this, "Servicio iniciado correctamente", Toast.LENGTH_LONG).show();
                    }catch(IOException e){
                        Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                    }
                }

            }else {
                Toast.makeText(this, "Ya hay un proceso de votación programado", Toast.LENGTH_SHORT).show();
            }
		} else if (id == R.id.action_last_server) {
            if(v.obtenerFechaInicioVotacionActual() != null){
                startService(myService); // Iniciar el servicio después de que la actividad haya terminado satisfactoriamente.
                Toast.makeText(this,"Servicio Iniciado", Toast.LENGTH_SHORT)
                    .show();
            }else
                Toast.makeText(this,"No hay votaciones pendientes", Toast.LENGTH_SHORT)
                        .show();
            /*
			try{
				ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(FILE_NAME));
				superChunk = (SuperChunk)entrada.readObject();
				myService.putExtra("SuperChunk", superChunk);
				myService.putExtra("operation_mode",FREE_CAMPAIGN);
				//launchDataLoader("Cargando matrícula", superChunk.getPreguntas().toArray(new Pregunta[0]));
				entrada.close();
			}catch(IOException | ClassNotFoundException e){
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			}
            */
		} else if (id == R.id.detener_servicio) {
			if (stopService(myService))
				Toast.makeText(this, "Servicio Detenido", Toast.LENGTH_SHORT)
						.show();
			else
				Toast.makeText(this, "El servicio ya está detenido",
						Toast.LENGTH_SHORT).show();
		} else if (id == R.id.ver_participantes) {
            //Toast.makeText(this,"Building :)",Toast.LENGTH_SHORT).show();
            v = new Votaciones(this);
            String titulo = v.obtenerTituloVotacionActual();
            String[] participaron = null;
            if( titulo == null )
                titulo = v.obtenerTituloVotacionFromId(v.obtenerIdUltimaVotacionHecha());
            if( titulo == null )
                titulo = "";
                participaron = v.quienesHanParticipado(titulo);
            /*
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
			*/
                i = new Intent(this, SimplePromptActivity.class);
                i.putExtra("rows", participaron);
                i.putExtra("header", "Participantes Registrados");
                startActivity(i);
		} else  if (id == R.id.finalizar_votacion) {
            if(v.obtenerFechaInicioVotacionActual() != null){
                try{
                    FileInputStream fis = new FileInputStream(RESULTS_FILE);
                    fis.close();
                    Toast.makeText(this, "Ya hemos finalizado n.n", Toast.LENGTH_LONG).show();
                }catch(IOException e){
                    launchMensajeConfirmacion(getResources().getString(R.string.mensaje_alerta),true,FINISH_VOTING_PROCESS_REQUEST);
                }
            }else
                Toast.makeText(this,"No hay votaciones inciadas", Toast.LENGTH_SHORT)
                    .show();
		} else if(id==R.id.hacer_global){
            if(!v.isCurrentVotingProcessGlobal())
            if(v.obtenerFechaInicioVotacionActual() == null)
                Toast.makeText(this, "An no hay un proceso de votación programado", Toast.LENGTH_SHORT).show();
            else {
                if( v.grabLastUserIdAttmptSucceded("Consultor") != -1){
                    launchMensajeConfirmacion("¿Desea publicar ésta votación como global? Otros sitios podrán unirse antes de su fecha de comienzo y participar", true, REQUEST_GO_GLOBAL);
                }else{
                    Toast.makeText(this,"Aún no hay un consultor registrado",Toast.LENGTH_SHORT).show();
                }
            }
            else
                Toast.makeText(this,"Ya registramos la votación como global",Toast.LENGTH_SHORT).show();
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

    private void launchIniciaVotacion(String title, int requestCode, boolean requestTitle){
        Intent i = new Intent(this,InputDateAndTimeValuesActivity.class);
        i.putExtra("title",title);
        i.putExtra("requestTitle",requestTitle);
        startActivityForResult(i, requestCode);
    }

    private void launchStandByActivity(String message){
        Intent i = new Intent(this,StandByActivity.class);
        i.putExtra("message", message);
        startActivityForResult(i, STAND_BY);
    }

    public void detenServicio(){
        stopService(myService);
    }
    public void quitaActividad(){
        finishActivity(STAND_BY);
    }
}