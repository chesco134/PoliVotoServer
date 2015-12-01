package org.inspira.polivoto.Threading;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.inspira.capiz.NeoSuperChunk.SuperChunk;
import org.inspira.polivoto.Security.Hasher;

import DataBase.Votaciones;
import Shared.Pregunta;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class AttendantHandler extends Thread {

	public Handler attendantInnerHandler;
	private LinkedList<String> capturistas;
	private LinkedList<String> participantes;
	private Context activity;
	private SuperChunk superChunk;
	private String votacionesTitle;
	private ObjectOutputStream salidaObjeto;
	private ObjectInputStream entradaObjeto;
	private Socket socket;
	private String[] datos;
	private String[] opcionesVoto;
	private Votaciones db;
	private Votaciones keyHandler;
	private int totales[] = new int[3];

    public AttendantHandler(){}
	
	public AttendantHandler(Context activity, SuperChunk superChunk){
		this.activity = activity;
		this.superChunk = superChunk;
	}

	@Override
	public void run() {
		Looper.prepare();
		capturistas = new LinkedList<String>();
		participantes = new LinkedList<String>();
		keyHandler = new Votaciones(activity);
		attendantInnerHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
            // What the attendant has to do when requested...
            socket = (Socket) msg.obj;
            try {
                SocketInputHandler sih = new SocketInputHandler(socket.getInputStream(),socket.getOutputStream());
                sih.setContext(activity);
                sih.setRHost(socket.getRemoteSocketAddress().toString());
                sih.start();// Recieving client's parameters. Name and desired action.
            } catch (Exception e) {
                e.printStackTrace();
            }
			}
		};
		Looper.loop();
	}

    public Context getActivity(){
        return activity;
    }
}
