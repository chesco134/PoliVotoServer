package org.inspira.polivoto.Threading;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
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
	private Hasher hasher;
	private ObjectOutputStream salidaObjeto;
	private ObjectInputStream entradaObjeto;
	private Socket socket;
	private String[] datos;
	private String[] opcionesVoto;
	private Votaciones db;
	private Votaciones keyHandler;
	private int totales[] = new int[3];
	
	public AttendantHandler(Context activity, SuperChunk superChunk){
		this.activity = activity;
		this.superChunk = superChunk;
	}

	@Override
	public void run() {
		Looper.prepare();
		capturistas = new LinkedList<String>();
		participantes = new LinkedList<String>();
		db = new Votaciones(activity);
		keyHandler = new Votaciones(activity);
		hasher = new Hasher();
		totales[0] = db.consultaPAAE();
		totales[1] = db.consultaDOCENTE();
		totales[2] = db.consultaAlumno();
		attendantInnerHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// What the attendant has to do when requested...
				socket = (Socket) msg.obj;
				try {
					entradaObjeto = new ObjectInputStream(socket.getInputStream());
					datos = (String[]) entradaObjeto.readObject();
					if (datos[0].equals("")) {
						entradaObjeto.close();
						socket.close();
						return;
					}
					salidaObjeto = new ObjectOutputStream(
							socket.getOutputStream());
					if (datos[2].equals("")) {
						if( datos[0].equals("Capturista")){
							if(!keyHandler.consultKey(hasher.makeHash(datos[1]), "Capturista")){
								salidaObjeto.writeUTF("NO");
								salidaObjeto.flush();
								socket.close();
								return;
							}
						}else{
							if(datos[0].equals("Participante")){
								if(!keyHandler.consultKey(hasher.makeHash(datos[1]), "Participante")){
									salidaObjeto.writeUTF("NO");
									salidaObjeto.flush();
									socket.close();
									return;
									}
							}else{
								if(datos[0].equals("Consultor")){
									if(!keyHandler.consultKey(hasher.makeHash(datos[1]),"Consultor")){
										salidaObjeto.writeUTF("NO");
										salidaObjeto.flush();
										socket.close();
										return;
										}
								}else{
									LinkedList<String> titulos = new LinkedList<String>();
									titulos.add("NO");
									salidaObjeto.writeObject(titulos);
									salidaObjeto.flush();
									salidaObjeto.writeObject(titulos);
									salidaObjeto.flush();
									socket.close();
									return;
								}
							}
						}
							if (datos[0].equals("Capturista")) {
								salidaObjeto.writeUTF("YES");
								salidaObjeto.flush();
								String capturistaHost = socket.getInetAddress()
										.getHostAddress();
								synchronized (capturistas) {
									if (!capturistas.contains(capturistaHost)) {
										capturistas.add(capturistaHost);
									}
								}
							} else {
								if (datos[0].equals("Participante")) {

									/* *** El hecho de manejar parejas 4eva, impide el cambio de
									 * dispositivos posteriormente. Comento esto para permitir
									 * cambios (es decir, que alguien se quiera llevar el cel después,
									 * sólo se tendría que poner a un capturista como nuevo candidato
									 * (haciendo "back & forth")y el nuevo participante conectarse
									 * para reclamarlo.	
									 * // Revisar que haya ya un capturista registrado.

									  synchronized (participantes) {
										ListIterator<String> iterator = participantes
												.listIterator();
										while (iterator.hasNext()) {
											String aux = iterator.next();
											String[] thePair = aux.split(" - ");
											if (thePair[1].equals(socket
													.getInetAddress()
													.getHostAddress())) {
												LinkedList<String> tempList = new LinkedList<String>();
												tempList.add(thePair[0]);
												salidaObjeto.writeObject(tempList);
												salidaObjeto.flush();
												socket.close();
												return;
											}
										}
									}
									*/
									synchronized (capturistas) {
										salidaObjeto.writeObject(capturistas);
										salidaObjeto.flush();
									}									
								} else {
									LinkedList<String> titulos = new LinkedList<String>();
									if (datos[0].equals("Consultor")) {
										titulos.add("YES"); // El cliente busca la confirmación en el primer elemento de la lista.
										ListIterator<Pregunta> iter = superChunk.getPreguntas().listIterator();
										while(iter.hasNext()){
											titulos.add(iter.next().titulo);
										}
										salidaObjeto.writeObject(titulos);
										salidaObjeto.flush();
									}
								}
							}
							entradaObjeto.close();
							salidaObjeto.close();
							socket.close();
							return;
						}

					if (datos[0].equals("Capturista")) {
						if (datos.length != 4 && datos.length != 5) {
							salidaObjeto
									.writeUTF("Aun no hay una terminal emparejada.");
							salidaObjeto.flush();
							salidaObjeto.close();
							entradaObjeto.close();
							socket.close();
							return;
						}
						
						if( datos.length == 5){
							long result = db.insertaRegistroUnico(datos[2], datos[3], datos[4], superChunk.getPreguntas());
							if( result != -1 )
								salidaObjeto
								.writeUTF("Registro insertado con éxito.");
							else
								salidaObjeto
								.writeUTF("Error al poner registro.");
							salidaObjeto.flush();
							salidaObjeto.close();
							entradaObjeto.close();
							socket.close();
							return;
						}
						
						/****************************************************************************************
						 * 
						 * 
						 * 		El capturista debe validar una boleta y en caso de s�, mandar todas las opciones 
						 * 	al participante. Contacta al participante en el puerto 36523. Por cada pregunta hace
						 * 	un intento de conexi�n con el participante.
						 * 
						 * 
						 ******************************************************************************************/

						List<String> papeletasRestantes;
						/* UNCOMMENT FOr a VOTING PROCESS WITH DATASET*/
						
						if( db.consultaParticipante(datos[2]) ){
							if(db.consultaParticipanteHoraVoto(datos[2])){
								salidaObjeto.writeUTF("El participante ha votado.");
								salidaObjeto.flush();
								salidaObjeto.close();
								entradaObjeto.close();
								socket.close();
								return;
							}
						}else{
						/*
							LinkedList<Pregunta> preguntas = new LinkedList<Pregunta>();
							ListIterator<Pregunta> pregs = superChunk.getPreguntas().listIterator();
							while(pregs.hasNext()){
								preguntas.add(pregs.next());
							}
							long tempId = db.insertaRegistroUnico("AyRodrigo", datos[2], "Tul", preguntas);
						*/
							// This is for a voting process with dataset
							salidaObjeto.writeUTF("El participante no está registrado.");
							salidaObjeto.flush();
							salidaObjeto.close();
							entradaObjeto.close();
							socket.close();
							return;
						
						}
						/* UNCOMMENT FOr a VOTING PROCESS WITH DATASET*/

						/* UNCOMMENT FOr a NORMA VOTING PROCESS */
						/*
						LinkedList<Pregunta> preguntas = new LinkedList<Pregunta>();
						ListIterator<Pregunta> pregs = superChunk.getPreguntas().listIterator();
						while(pregs.hasNext()){
							preguntas.add(pregs.next());
						}
						*/
						
						//db.insertaRegistroUnico("AyRodrigo", datos[2], "tul", preguntas);
						//db.insertaBoleta(datos[2],preguntas); // El método puede devolver -1 en 
						// caso de no haber podido insertar la boleta.
						/* UNCOMMENT FOr a NORMA VOTING PROCESS */
						
						papeletasRestantes = db.consultaPapeletasFaltantes(datos[2]);
						try {
							for (int i = 0; i< papeletasRestantes.size(); i++) {
								Socket slaveTerminal = new Socket(datos[3], 36523);
								ObjectOutputStream salida = new ObjectOutputStream(
										slaveTerminal.getOutputStream());
								Pregunta currentQuestion = superChunk.getPregunta(papeletasRestantes.get(i));
								opcionesVoto = new String[currentQuestion.opciones.size()];
								for( int j=0; j<opcionesVoto.length; j++){
									opcionesVoto[j] = currentQuestion.opciones.get(j).nombre;
								}
								votacionesTitle = currentQuestion.titulo;
								LinkedList<String> dataOptionsList = new LinkedList<String>();
								for (String iter : opcionesVoto) {
									dataOptionsList.add(iter);
								}
								dataOptionsList.addFirst(votacionesTitle);
								dataOptionsList.addFirst(datos[2]);
								//SuperChunk sc = new SuperChunk(superChunk.getPreguntas());
								//sc.setBoleta(datos[2]);
								salida.writeObject(dataOptionsList);
								salida.close();
								slaveTerminal.close();
							}
							salidaObjeto.writeUTF("Papeleta generada.");
							salidaObjeto.flush();
						} catch (NullPointerException | IOException e) {
							e.printStackTrace();
							salidaObjeto.writeUTF("Servicio no disponible.\n" + e.toString());
							salidaObjeto.flush();
							PrintWriter pw = new PrintWriter(salidaObjeto);
							e.printStackTrace(pw);
							pw.flush();
						}
						salidaObjeto.close();
						entradaObjeto.close();
						socket.close();
						return;
					}
						/* ***
						 * 
						 *  SuperChunk es un elemento que debe ser colocado al iniciar el servicio.
						 *  
						 * ****/
						
					
					/***************************************************************************
					 * 
					 * 		El participante debe saber con qu� capturista trabajar, para evitar
					 * 	programar m�s puertos.
					 *
					 **********************************************************************/							
					
					if (datos[0].equals("Participante")) {
						
						if (datos.length != 5) {
							String participanteHost = socket.getInetAddress()
									.getHostAddress();	//AL CAPTURISTA HAY QUE DECIRLE QUIEN ES SU PARTICIPANTE
							try {
								synchronized (capturistas) {
									boolean wasIn = false;
									ListIterator<String> currentCapturista = capturistas.listIterator();
									while(currentCapturista.hasNext()){
										if( currentCapturista.next().equals(datos[2]) ){
											wasIn = true;
											break;
										}
									}
									if (wasIn) {
										Socket capturista = new Socket(datos[2],
												33401);
										
										ObjectOutputStream salida = new ObjectOutputStream(
												capturista.getOutputStream());
										salida.writeUTF(participanteHost);
										salida.flush();
										capturistas.remove(datos[2]);
										synchronized (participantes) {
											String pair = datos[2] + " - "
													+ participanteHost;
											if (!participantes.contains(pair)) {
												participantes.add(pair);
											}
										}
										salidaObjeto.writeUTF("Registrado.");
										salidaObjeto.flush();
										
										salida.close();
										capturista.close();
									} else {
										salidaObjeto
												.writeUTF("No se encontró al capturista.");
										salidaObjeto.flush();
									}
								}
							} catch (IOException e) { // Deja al participante en
														// lista de espera.
								salidaObjeto
								.writeUTF("Error: " + e.toString());
								salidaObjeto.flush();
								
								/*
								String pair = datos[2] + " - "
										+ socket.getInetAddress().getHostAddress();
								synchronized (participantes) {
									if (!participantes.contains(pair)) {
										participantes.add(pair);
									}
								}*/
							}
							salidaObjeto.close();
							entradaObjeto.close();
							socket.close();
							return;
						}
						
						/***************************************************************
						 * 
						 * 		El participante debe "actualizarVoto".
						 *
						 **********************************************************************/
						
						
						String result = db.actualizaVotando(datos[2], datos[3], datos[4]);
						ListIterator<String> actualPair = participantes.listIterator();
						String capturist = null;
						while(actualPair.hasNext()){
							String currentPair = actualPair.next();
							String[] thePair = currentPair.split(" - ");
							if( thePair[1].equals(socket.getInetAddress().getHostAddress()) ){
								capturist = thePair[0];
							}
						}
						try{
							Socket so = new Socket(capturist,5001);
							DataOutputStream salida = new DataOutputStream(so.getOutputStream());
							salida.flush();
							if(result != null){
								salida.writeUTF(datos[2] +", ha votado con éxito.");
							}else{
								salida.writeUTF("El participante ha emitido un voto.");
							}
							salida.flush();
							so.close();
						}catch(IOException e){
							//salidaObjeto.writeUTF("Problemas al contactar al capturista... notifíquelo por favor.");
						}
						salidaObjeto.writeUTF("Su voto ha sido guardado.");
						salidaObjeto.flush();
						salidaObjeto.close();
						entradaObjeto.close();
						socket.close();
						return;
					}
					if (datos[0].equals("Consultor")) {
						
						/***************************************************************
						 * 
						 * 		El Consultor pregunta con un t�tulo cuales son los resultados
						 * 	que desea obtener, as�, se debe llamar a "consultarVotos", pasando
						 * 	el nombre de la pregunta como par�metro de b�squeda y armando una
						 * 	lista ligada con los t�tulos de las opciones @ n�mero de votos.
						 *
						 **********************************************************************/							
						
						LinkedList<String> Resultados = new LinkedList<String>();
						LinkedList<Pregunta> preguntas = superChunk.getPreguntas();
						ListIterator<Pregunta> iter = preguntas.listIterator();
						Pregunta theChosenOne = null;
						while(iter.hasNext()){
							Pregunta aux = iter.next();
							if(aux.titulo.equals(datos[2]))
								theChosenOne = aux;
						}
						if( theChosenOne != null ){
							opcionesVoto = new String[theChosenOne.opciones.size()];
							for(int i = 0; i<theChosenOne.opciones.size(); i++){
								opcionesVoto[i] = theChosenOne.opciones.get(i).nombre;
							}
							int count = 0;
							//for (int k=0; k<3; k++)
							int k = 3;
							for (String iterator : opcionesVoto) {
								try{count = db.consultaVotos(iterator, theChosenOne.titulo,k);}
								catch(Exception e){ Log.e("consultaVotos", e.getMessage()); }
								switch(k){
								case 0:
									Resultados.add(iterator + " (PAAEs)" + "@" + (count));// + currentOption.cantidad));
									//Resultados.add(iterator + " (PAAEs)" + "@" + (count/totales[0]));// + currentOption.cantidad));
									break;
								case 1:
									Resultados.add(iterator + " (Docentes)" + "@" + (count));// + currentOption.cantidad));
									//Resultados.add(iterator + " (Docentes)" + "@" + (count/totales[1]));// + currentOption.cantidad));
									break;
								case 2:
									Resultados.add(iterator + " (Alumnos)" + "@" + (count));// + currentOption.cantidad));
									//Resultados.add(iterator + " (Alumnos)" + "@" + (count/totales[2]));// + currentOption.cantidad));
									break;
									default:
										Resultados.add(iterator + "@" + count);
								}
							}
							salidaObjeto.writeObject(Resultados);
							salidaObjeto.flush();
						}else{
							Resultados.add("Error en el título.");
							salidaObjeto.writeObject(Resultados);
							salidaObjeto.flush();
						}
					}
					salidaObjeto.close();
					entradaObjeto.close();
					socket.close();
					return;
				} catch (IOException e) {
					
				} catch (ClassNotFoundException e) {
				}

			}
		};
		Looper.loop();
	}

}
