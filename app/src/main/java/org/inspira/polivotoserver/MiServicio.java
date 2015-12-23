package org.inspira.polivotoserver;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import org.inspira.polivoto.Activity.Lobby;
import org.inspira.polivoto.Networking.soap.ServiceClient;
import org.inspira.polivoto.Threading.SocketInputHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import DataBase.Votaciones;

public class MiServicio extends Service {
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private ServerSocket server;
    private static boolean isRunning = false;
	private final IBinder mBinder = new LocalBinder();
    private ConcurrentLinkedQueue<Runnable> attendants;
    private NotificationManager mNM;
    private NotificationCompat.Builder mBuilder;
    private static final int POLIVOTO_SERVICE = 319;
    private Activity mActivity;

    private TimerTask task = new TimerTask(){
        @Override
        public void run(){
            synchronized(this){
                try{
                    mServiceHandler.post(new Runnable(){
                        @Override
                        public void run(){
                            Toast.makeText(MiServicio.this,"Testing...",Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.d("Survivor","Anything's normal here");
                    Votaciones db = new Votaciones(MiServicio.this);
                    wait(new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(db.obtenerFechaFinVotacionActual()).getTime());
                    if(db.isVotacionActualGlobal()) {
                        JSONObject json = new JSONObject();
                        json.put("action",6);
                        json.put("title",db.obtenerTituloVotacionActual());
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
                        Log.d("Finisher Task", "Votación terminada");
                    }
                }catch(ParseException | InterruptedException e){
                    e.printStackTrace();
                    Log.d("Finisher Task","Error al esperar terminar la votación: " + e.getMessage());
                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                    Log.d("Finisher Task", "Servicio no disponible");
                } catch (JSONException e) {
                    // This is just for fun.
                }
            }
        }
    };

    public class LocalBinder extends Binder {
        public MiServicio getService() {
            return MiServicio.this;
        }
    }

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {

		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			try {
				server = new ServerSocket(23543);
                Log.d("ASMODIAN","We are about to begin");
				// You need to set up a Timer to finish the voting process...
				if(!isRunning) {
                    isRunning = true;
                    new Timer().schedule(task, 0);
                    Toast.makeText(MiServicio.this,"Cuenta iniciada", Toast.LENGTH_SHORT).show();
                }
				while (true) { // En éste loop se producen tareas.
					Socket socket = server.accept();
                    SocketInputHandler sih = new SocketInputHandler(socket.getInputStream(),socket.getOutputStream());
                    sih.setContext(mActivity);
                    sih.setRHost(socket.getRemoteSocketAddress().toString());
                    sih.start();
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
		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart (START_STICKY)
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread("ServiceStartArguments",
				android.os.Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
        attendants = new ConcurrentLinkedQueue<>();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.logo_notification)
						.setOngoing(true)
						.setContentTitle("PoliVoto")
						.setContentText("Vote usté");
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, Lobby.class);
		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(Lobby.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
				);
		mBuilder.setContentIntent(resultPendingIntent);
		mBuilder.setVibrate(new long[]{100,100,100,600});
		mNM.notify(POLIVOTO_SERVICE, mBuilder.build());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        mNM.cancel(POLIVOTO_SERVICE);
        isRunning = false;
        task.cancel();
		try {
			server.close();
		} catch (IOException | NullPointerException e) {
            e.printStackTrace();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if(mServiceHandler != null)
            return mBinder;
        else {
            stopSelf();
            return null;
        }
	}

	public void setMainActivity(Activity mActivity){
		this.mActivity = mActivity;
	}

	private void updateParticipationCount(){

	}

    private class WorkerThread extends Thread{

        private int selfId;

        public WorkerThread(int id){
            selfId = id;
        }

        @Override
        public void run(){
            while(isRunning){
                Runnable task = attendants.poll();
                if(task != null )
                    task.run();
            }
        }
    }
}