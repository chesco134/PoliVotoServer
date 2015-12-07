package org.inspira.polivotoserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import org.inspira.capiz.NeoSuperChunk.SuperChunk;
import org.inspira.polivoto.Activity.VotacionesConf;
import org.inspira.polivoto.Networking.soap.ServiceClient;
import org.inspira.polivoto.Threading.AttendantHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import DataBase.Votaciones;

public class MiServicio extends Service {
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private ServerSocket server;
	private boolean finishedConnection = false;
    public static boolean isRunning = false;
	private final IBinder mBinder = new LocalBinder();
    private Activity ctx;

    private TimerTask task = new TimerTask(){
        @Override
        public void run(){
            try{
                Votaciones db = new Votaciones(MiServicio.this);
                wait(new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(db.obtenerFechaFinVotacionActual()).getTime());
                if(db.isVotacionActualGlobal()) {
                    JSONObject json = new JSONObject();
                    try {
                        json.put("action",6);
                        json.put("title",db.obtenerTituloVotacionActual());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if("success".equals(new ServiceClient().sendAction(json.toString()))){
                        db.terminaUltimaVotacion();
                        Log.d("Finisher Task", "Votación terminada");
                        Toast.makeText(MiServicio.this,"Votación terminada",Toast.LENGTH_LONG).show();
                    }else{
                        Log.d("Finisher Task", "Servicio no disponible");
                        Toast.makeText(MiServicio.this,"Servicio no disponible",Toast.LENGTH_LONG).show();
                    }
                }else {
                    db.terminaUltimaVotacion();
                    Log.d("Finisher Task", "Votación terminada");
                    Toast.makeText(MiServicio.this, "Votación terminada", Toast.LENGTH_LONG).show();
                }
            }catch(ParseException | InterruptedException e){
                e.printStackTrace();
                Log.d("Finisher Task","Error al esperar terminar la votación: " + e.getMessage());
            } catch (XmlPullParserException | IOException e) {
				e.printStackTrace();
				Toast.makeText(MiServicio.this,"Servicio no disponible",Toast.LENGTH_LONG).show();
			}
		}
    };

    public class LocalBinder extends Binder {
        public MiServicio getService() {
            return MiServicio.this;
        }
    }

    public void setContext(Activity c) {
        ctx = c;
    }

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {

		private AttendantHandler attendantHandler;
		private SuperChunk superChunk;

		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			try {
				server = new ServerSocket(23543);
				attendantHandler = new AttendantHandler(MiServicio.this,superChunk);
				attendantHandler.start();
                isRunning = true;
                Log.d("ASMODIAN","We are about to begin");
				// You need to set up a Timer to finish the voting process...
				if(!isRunning) {
                    isRunning = true;
                    new Timer().schedule(task, 0);
                    Toast.makeText(MiServicio.this,"Cuenta iniciada", Toast.LENGTH_SHORT).show();
                }
				while (true) {
					Socket socket = server.accept();
					Message attendantMsg = attendantHandler.attendantInnerHandler
							.obtainMessage();
					attendantMsg.obj = socket;
					attendantMsg.arg2 = msg.arg2;
                    attendantHandler.attendantInnerHandler
                            .sendMessage(attendantMsg);
				}
			} catch (IOException e) {
				Toast.makeText(MiServicio.this, "Server down!",
						Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the
		// job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		if( intent != null ){
			msg.obj = intent.getSerializableExtra("SuperChunk");
			msg.arg2 = intent.getIntExtra("operating_mode", VotacionesConf.FREE_CAMPAIGN);
		} else {
			try{
				ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(VotacionesConf.FILE_NAME));
				SuperChunk superChunk = (SuperChunk)entrada.readObject();
				entrada.close();
				msg.obj = superChunk;
				msg.arg2 = VotacionesConf.FREE_CAMPAIGN;
			}catch(IOException | ClassNotFoundException e){
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			}
		}
		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart (START_STICKY)
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread("ServiceStartArguments",
				android.os.Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			server.close();
		} catch (IOException | NullPointerException e) {
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		 return mBinder;
		//return null;
	}

    public boolean isRunning(){
        return isRunning;
    }

	public void hasFinished() {
		while (!finishedConnection);
	}
}
