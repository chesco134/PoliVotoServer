package org.inspira.polivotoserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.inspira.capiz.NeoSuperChunk.SuperChunk;
import org.inspira.polivoto.Activity.VotacionesConf;
import org.inspira.polivoto.Threading.AttendantHandler;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

public class MiServicio extends Service {
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private ServerSocket server;
	private boolean finishedConnection = false;
	//private final IBinder mBinder = new LocalBinder();

	/*
		public class LocalBinder extends Binder {
			MiServicio getService() {
				return MiServicio.this;
			}
		}
	 */

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {

		private AttendantHandler attendantHandler;
		private SuperChunk superChunk;

		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			superChunk = (SuperChunk)msg.obj;
			if(superChunk == null){
				// You have to say: "We couldn't restart the service, do it manually please."
				return;
			}
			try {
				server = new ServerSocket(23543);
				attendantHandler = new AttendantHandler(MiServicio.this,superChunk);
				attendantHandler.start();
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
		// return mBinder;
		return null;
	}

	public void hasFinished() {
		while (!finishedConnection);
	}
}
