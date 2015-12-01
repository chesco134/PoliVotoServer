package org.inspira.polivoto.Threading;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.inspira.polivoto.Activity.ConfiguraParticipantesActivity;
import org.inspira.polivoto.Security.Hasher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import DataBase.Votaciones;

/**
 * Created by jcapiz on 28/11/15.
 */
public class SocketInputHandler extends Thread {

    private DataInputStream entrada;
    private DataOutputStream salida;
    private int cByte;
    private byte b;
    private List<Byte> bytes;
    private List<String> messages;
    private Context context;
    private String rHost;
    private static volatile List<String> hostsPostulados;

    private int idAttempt;
    private Votaciones db;
    private byte[] chunk;
    private Cipher cip;
    int resp;

    public SocketInputHandler(InputStream entrada, OutputStream salida){
        this.entrada = new DataInputStream(entrada);
        this.salida = new DataOutputStream(salida);
        bytes = new ArrayList<>();
        messages = new ArrayList<>();
        if(hostsPostulados == null)
            hostsPostulados = new ArrayList<>();
    }

    public void setContext(Context context){
        this.context = context;
    }

    public void setRHost(String host){
        rHost= host;
    }

    @Override
    public void run(){
        try{
            Log.d("RE3","Entered the nightmare");
            // Wait for the first byte and analyse it.
            cByte = entrada.read();
            b = Byte.parseByte(String.valueOf((byte) (cByte & 0xFF)));
            Log.d("SocketHandler", "Guten tag " + rHost);
            db = new Votaciones(context);
            // Make key Exchange.
            if( b == Byte.parseByte(String.valueOf((byte) (-1 & 0xFF))) ) {
                try {
                    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                    kpg.initialize(1024);
                    KeyPair kp = kpg.genKeyPair();
                    Key publicKey = kp.getPublic();
                    Key privateKey = kp.getPrivate();
                    salida.write(publicKey.getEncoded());
                    salida.flush(); //** Successfuly sent public key **//
                    byte[] cipheredAESKey = new byte[128];
                    System.out.println("**********************************" + entrada.read(cipheredAESKey));
                    byte[] encodedAESKey;
                    cip = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // That String is needed in ANDROID
                    cip.init(Cipher.DECRYPT_MODE, privateKey);
                    encodedAESKey = cip.doFinal(cipheredAESKey);
                    SecretKeySpec skp = new SecretKeySpec(encodedAESKey, "AES");
                    System.out.println("" + encodedAESKey.length);
                    byte[] cipheredMessage = new byte[Integer.valueOf(entrada.read())];
                    entrada.read(cipheredMessage);
                    cip = Cipher.getInstance("AES");
                    cip.init(Cipher.DECRYPT_MODE, skp);
                    // Remaining bytes conform a JSON String.
                    String jstr = new String(cip.doFinal(cipheredMessage));
                    Log.d("Shura", jstr);
                    // This json contains the User credentials.
                    JSONObject json = new JSONObject(jstr);
                    String uName = json.getString("uName");
                    String psswd = json.getString("psswd"); // Password is hashed with sha-254
                    boolean bol = db.consultaUsuario(uName, Hasher.hexStringToByteArray(psswd));
                    // You need to keep the secret key for the user.
                    int lid = db.insertaLoginAttempt(uName, rHost);
                    if (bol) {
                        long lon = db.insertaAttemptSucceded(lid, skp.getEncoded());
                        Log.d("La ruptura", "" + lid + ", lon: " + lon); // Hence you use the id to retrieve
                        // keep the sKey to know the content of the messages.
                    }
                    salida.write(lid);
                    salida.flush();
                } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | NoSuchPaddingException | BadPaddingException | JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                // Read the first byte to know which sKey to load so you can decipher the String.
                idAttempt = (b);
                Log.d("RAKATATAKA","" + idAttempt);
                SecretKeySpec sk = new SecretKeySpec(db.obtenerSKeyEncoded(idAttempt),"AES");
                chunk = new byte[entrada.read()]; // El siguiente byte contiene cuantos siguen.
                entrada.read(chunk); // Obtenemos los bytes cifrados.
                try {
                    cip = Cipher.getInstance("AES");
                    cip.init(Cipher.DECRYPT_MODE,sk);
                    String jstr = new String(cip.doFinal(chunk));
                    JSONObject json = new JSONObject(jstr);
                    // En el objecto JSON esperamos encontrar al usuario y a sus intenciones
                    db.insertUserAction(idAttempt,jstr);
                    int resp;
                    String boleta;
                    byte[] idVoto;
                    String perfil;
                    String voto;
                    String pregunta;
                    int requestedAction = json.getInt("action"); // La tarea a realizar es un entero.
                    // 1 Postulate me!.
                    // 2 Pide validación de boleta.
                    // 3 Entrega voto.
                    // 4 Pide conteo de votos.
                    // 5 Pide título de votación.
                    // 6 Pide preguntas de votación.
                    // 7 Pide fecha de inicio de votación.
                    // 8 Pide fecha de término de votación.
                    // 9 Alta participante.
                    // 10 Pide perfiles.
                    // 11 Pide preguntas para participante.
                    // 12 Participante contestó pregunta.
                    // 13 Pide opciones de pregunta.
                    cip.init(Cipher.ENCRYPT_MODE, sk);
                    Log.e("Man", db.obtenerUsuarioPorIdAttempt(idAttempt) + "\n------------> " + requestedAction);
                    switch(requestedAction){
                        case 1:
                            action1(salida);
                            break;
                        case 2:
                            // Las siguientes dos líneas deben ser implementación del servicio.
                            action2(salida,json);
                            break;
                        case 3:
                            idVoto = Hasher.hexStringToByteArray(json.getString("idVoto"));
                            pregunta = json.getString("pregunta");
                            int idVotacion = db.obtenerIdVotacionFromPregunta(pregunta);
                            perfil = json.getString("perfil");
                            voto = json.getString("voto");
                            if(db.insertaVoto(idVoto,idVotacion,perfil,voto,idAttempt,db.obtenerIdPregunta(pregunta))!=-1)
                                resp = 1;
                            else
                                resp = 0;
                            chunk = cip.doFinal(String.valueOf(resp).getBytes());
                            salida.write(chunk.length);
                            salida.write(chunk);
                            break;
                        case 4:
                            /***************************************************************
                             *
                             * 		El Consultor pregunta con un título cuales son los resultados
                             * 	que desea obtener, así, se debe llamar a "consultarVotos", pasando
                             * 	el nombre de la pregunta como parámetro de búsqueda y armando una
                             * 	lista ligada con los títulos de las opciones @ número de votos.
                             *
                             **********************************************************************/
                            pregunta = json.getString("pregunta");
                            LinkedList<String> resultados = db.obtenerResultadosPorPregunta(pregunta,db.obtenerIdVotacionFromPregunta(pregunta));
                            // Podría darse el caso en que mejor se cambie a ObjectOutputStream y se mande la lista ligada.
                            JSONObject jsresp = new JSONObject();
                            String[] pair;
                            for(String str : resultados) {
                                pair = str.split("@");
                                jsresp.put(pair[0], pair[1]);
                            }
                            chunk = cip.doFinal(jsresp.toString().getBytes());
                            salida.write(chunk.length);
                            salida.write(chunk);
                            break;
                        case 5:
                            String titulo = db.obtenerTituloVotacionActual();
                            chunk = cip.doFinal(titulo.getBytes());
                            salida.write(chunk.length);
                            salida.write(chunk);
                            break;
                        case 6:
                            String preguntas[] = db.obtenerPreguntasVotacion(db.obtenerTituloVotacionActual());
                            JSONArray jarr = new JSONArray();
                            for(int i=0; i<preguntas.length;i++){
                                jarr.put(i,preguntas[i]);
                            }
                            chunk = cip.doFinal(jarr.toString().getBytes());
                            salida.write(chunk.length);
                            salida.write(chunk);
                            break;
                        case 7:
                            chunk = cip.doFinal(db.obtenerFechaInicioVotacionActual().getBytes());
                            salida.write(chunk.length);
                            salida.write(chunk);
                            break;
                        case 8:
                            chunk = cip.doFinal(db.obtenerFechaFinVotacionActual().getBytes());
                            salida.write(chunk.length);
                            salida.write(chunk);
                            break;
                        case 9:
                            action9(json);
                            break;
                        case 10:
                            String[] perfiles = db.obtenerPerfiles();
                            JSONArray jsonArray = new JSONArray();
                            for(int i=0; i<perfiles.length;i++)
                                jsonArray.put(i,perfiles[i]);
                            chunk = cip.doFinal(jsonArray.toString().getBytes());
                            salida.write(chunk.length);
                            salida.write(chunk);
                            break;
                        case 11:
                            boleta = json.getString("boleta");
                            String[] pregsParticipante = db.consultaParticipantePreguntas(boleta);
                            JSONArray jsonArray1 = new JSONArray();
                            for(int i=0; i<pregsParticipante.length;i++)
                                jsonArray1.put(i,pregsParticipante[i]);
                            chunk = cip.doFinal(jsonArray1.toString().getBytes());
                            salida.write(chunk.length);
                            salida.write(chunk);
                            break;
                        case 12:
                            boleta = json.getString("boleta");
                            pregunta = json.getString("pregunta");
                            db.actualizaParticipantePregunta(boleta,pregunta);
                            break;
                        case 13:
                            chunk = cip.doFinal(db.obtenerOpcionesVotacion(json.getString("pregunta")).toString().getBytes());
                            salida.write(chunk.length);
                            salida.write(chunk);
                            break;
                        default:
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }
            }
            salida.close();
            entrada.close();
        }catch(IOException e){//this can be very funy
            Log.d("SocketInputHandler", e.getMessage());
            e.printStackTrace();
        }
    }

    private void action1(DataOutputStream salida) throws IOException, BadPaddingException, IllegalBlockSizeException {
        if("Participante".equals(db.obtenerUsuarioPorIdAttempt(idAttempt))){
            hostsPostulados.add(rHost);
            // Participante may need to wait until a new Capturista contacts it
        }else if ("Capturista".equals(db.obtenerUsuarioPorIdAttempt(idAttempt))) {
            Log.d("Pollos",String.valueOf(hostsPostulados.size()));
            while (hostsPostulados.size() == 0);
            Log.d("Pollos", "Entered the underworld");
            chunk = cip.doFinal(String.valueOf(hostsPostulados.remove(hostsPostulados.size() - 1)).getBytes());
            salida.write(chunk.length);
            salida.write(chunk);
            salida.flush();
        }
    }

    private void action2(DataOutputStream salida, JSONObject json) throws IOException, BadPaddingException, IllegalBlockSizeException, JSONException {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean usarMatricula = sharedPref.getBoolean(ConfiguraParticipantesActivity.USAR_MATRICULA_KEY, false);
        String boleta = json.getString("boleta");
        if(db.consultaExistenciaBoleta(boleta)){
            resp = 1;
        }else{
            resp = 0;
        }
        if(resp == 0 && !usarMatricula){
            action9(json);
        }
        chunk = cip.doFinal(String.valueOf(resp).getBytes());
        salida.write(chunk.length);
        salida.write(chunk);
    }

    private void action9(JSONObject json) throws JSONException {
        String boleta = json.getString("boleta");
        String perfil = json.getString("perfil");
        String escuela = json.getString("escuela");
        String nombre = json.getString("nombre");
        String apPaterno = json.getString("ap_paterno");
        String apMaterno = json.getString("ap_materno");
        if(db.insertaParticipante(boleta,perfil == null ? "" : perfil,escuela == null ? "" : escuela) == -1){
            // Si no se proporcionan
            db.insertaParticipante(boleta,db.obtenerPerfiles()[0],db.obtenerUltimaEscuela());
        }
        if( nombre != null && apPaterno != null && apMaterno != null)
            db.insertaNombreParticipante(boleta, nombre, apPaterno, apMaterno);
        String[] pregs = db.obtenerPreguntasVotacion(db.obtenerTituloVotacionActual());
        for(String preg : pregs) // Habilita una pregunta para cada participante.
            db.insertaParticipantePregunta(boleta,preg);
    }
}
