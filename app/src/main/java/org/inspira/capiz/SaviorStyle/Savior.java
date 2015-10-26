package org.inspira.capiz.SaviorStyle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.ListIterator;

import Shared.Opcion;
import Shared.Pregunta;
import Shared.SuperChunk;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class Savior {
	SuperChunk superChunk;
	LinkedList<Pregunta> preguntas;
	Context context;
	Intent myService;
	
	public Savior(Context context, Intent myService){
		this.context = context;
		this.myService = myService;
	}
	
	class Runner extends AsyncTask<String,String,String>{
		@Override
		protected String doInBackground(String... args){
			String algo = null;
			String[] titles = null;
			try{
				Socket socket = new Socket("192.168.43.1",23543);
				ObjectOutputStream salida = new ObjectOutputStream(
						socket.getOutputStream());
				salida.writeObject(new String[]{"Consultor","mecatronica",""});
				salida.flush();
				ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
				LinkedList<?> titulos = (LinkedList<?>)input.readObject();
				socket.close();
				titles = new String[titulos.size()];
				onProgressUpdate(titles);
				for(int i = 0; i<titles.length; i++){
					titles[i] = (String)titulos.get(i);
				}
			}catch(ClassNotFoundException | IOException e){
				titles = null;
				e.printStackTrace();
			}
			
			if (titles != null){
				preguntas = new LinkedList<Pregunta>();
				for(int i=0; i<titles.length; i++){
					String[] connectionParams = {
						"Consultor","mecatronica", titles[i]
					};
					try{
						Socket socket = new Socket("192.168.43.1",23543);
						ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
						salida.writeObject(connectionParams);
						ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream()); 
						LinkedList<?> results = (LinkedList<?>)entrada.readObject();
						socket.close();
						ListIterator<?> rows = results.listIterator();
						LinkedList<Opcion> opciones = new LinkedList<Opcion>();
						while(rows.hasNext()){
							String currentRow = (String)rows.next();
							String[] par = currentRow.split("@");
							Opcion op = new Opcion();
							op.nombre = par[0];
							op.cantidad = Integer.parseInt(par[1]);
							opciones.add(op);
						}
						Pregunta nuevaPregunta = new Pregunta();
						nuevaPregunta.opciones = opciones;
						nuevaPregunta.titulo = titles[i];
						preguntas.add(new Pregunta());
					}catch(ClassNotFoundException | IOException e){
						Log.e("Savior: ",e.getMessage());
						e.printStackTrace();
					}
				}
			}
			if( preguntas != null){
				SuperChunk superChunk = new SuperChunk(preguntas);
				myService.putExtra("SuperChunk", superChunk);
				context.startService(myService);
				algo = "Servicio Iniciado";	
			}else{
				algo = "Error al conectar";
			}
			return algo;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			for(String iter : values){
				Log.e("Current Progress",iter + "\n");
			}
		};
		
		@Override
		protected void onPostExecute(String result){
			Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void rescueRemoteServer(){
		new Runner().execute("");
	}
}
