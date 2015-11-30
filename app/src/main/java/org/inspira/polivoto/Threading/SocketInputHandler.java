package org.inspira.polivoto.Threading;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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
import java.util.Arrays;
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
    private static volatile List<Integer> idAttPostulados;

    public SocketInputHandler(InputStream entrada, OutputStream salida){
        this.entrada = new DataInputStream(entrada);
        this.salida = new DataOutputStream(salida);
        bytes = new ArrayList<>();
        messages = new ArrayList<>();
        if(idAttPostulados == null)
            idAttPostulados = new ArrayList<>();
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
            Cipher cip;
            // Wait for the first byte and analyse it.
            cByte = entrada.read();
            b = Byte.parseByte(String.valueOf((byte) (cByte & 0xFF)));
            Log.d("SocketHandler", "Guten tag " + rHost);
            Votaciones db = new Votaciones(context);
            if( b == Byte.parseByte(String.valueOf((byte) (-1 & 0xFF))) ){ // Make key Exchange.
                try{
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
                    SecretKeySpec skp = new SecretKeySpec(encodedAESKey,"AES");
                    System.out.println("" + encodedAESKey.length);
                    byte[] cipheredMessage = new byte[Integer.valueOf(entrada.read())];
                    entrada.read(cipheredMessage);
                    cip = Cipher.getInstance("AES");
                    cip.init(Cipher.DECRYPT_MODE,skp);
                    // Remaining bytes conform a JSON String.
                    String jstr = new String(cip.doFinal(cipheredMessage));
                    Log.d("Shura",jstr);
                    // This json contains the User credentials.
                    JSONObject json = new JSONObject(jstr);
                    String uName = json.getString("uName");
                    String psswd = json.getString("psswd"); // Password is hashed with sha-254
                    boolean bol = db.consultaUsuario(uName, Hasher.hexStringToByteArray(psswd));
                    // You need to keep the secret key for the user.
                    int lid = db.insertaLoginAttempt(uName,rHost);
                    if(bol) {
                        db.insertaAttemptSucceded(lid, skp.getEncoded());
                        Log.d("La ruptura","" + lid); // Hence you use the id to retrieve
                        // the sKey to know the content of the messages.
                    }
                    salida.write(lid);
                    salida.flush();
                }catch(IOException e){
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{
                // Read the first byte to know which sKey to load so you can decipher the String.
                int idAttempt = entrada.read();
                SecretKeySpec sk = new SecretKeySpec(db.obtenerSKeyEncoded(idAttempt),"AES");
                byte[] chunk = new byte[entrada.read()]; // El siguiente byte contiene cuantos siguen.
                entrada.read(chunk); // Obtenemos los bytes cifrados.
                try {
                    cip = Cipher.getInstance("AES");
                    String jstr = new String(cip.doFinal(chunk));
                    JSONObject json = new JSONObject(jstr);
                    // En el objecto JSON esperamos encontrar al usuario y a sus intenciones
                    db.insertUserAction(idAttempt,jstr);
                    String boleta;
                    byte[] idVoto;
                    String perfil;
                    String voto;
                    String pregunta;
                    int requestedAction = json.getInt("action"); // La acción es un entero.
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
                    cip.init(Cipher.ENCRYPT_MODE,sk);
                    switch(requestedAction){
                        case 1:
                            if("Participante".equals(db.obtenerUsuarioPorIdAttempt(idAttempt))){
                                idAttPostulados.add(idAttempt);
                                // Participante may need to wait until a new Capturista contacts it
                            }else if("Capturista".equals(db.obtenerUsuarioPorIdAttempt(idAttempt))){
                                while(idAttPostulados.size() == 0);
                                salida.write(cip.doFinal(String.valueOf(idAttPostulados.remove(idAttPostulados.size() - 1)).getBytes()));
                                salida.flush();
                            }
                            break;
                        case 2:
                            boleta = json.getString("boleta");
                            if(db.consultaExistenciaBoleta(boleta)){
                                salida.write(cip.doFinal(String.valueOf(1).getBytes()));
                            }else{
                                salida.write(cip.doFinal(String.valueOf(0).getBytes()));
                            }
                            break;
                        case 3:
                            idVoto = Hasher.hexStringToByteArray(json.getString("idVoto"));
                            pregunta = json.getString("pregunta");
                            int idVotacion = db.obtenerIdVotacionFromPregunta(pregunta);
                            perfil = json.getString("perfil");
                            voto = json.getString("voto");
                            if(db.insertaVoto(idVoto,idVotacion,perfil,voto,idAttempt,db.obtenerIdPregunta(pregunta))!=-1)
                                salida.write(cip.doFinal(String.valueOf(1).getBytes()));
                            else
                                salida.write(cip.doFinal(String.valueOf(0).getBytes()));
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
                            salida.write(cip.doFinal(jsresp.toString().getBytes()));
                            break;
                        case 5:
                            String titulo = db.obtenerTituloVotacionActual();
                            salida.write(cip.doFinal(titulo.getBytes()));
                            break;
                        case 6:
                            String preguntas[] = db.obtenerPreguntasVotacion(db.obtenerTituloVotacionActual());
                            JSONArray jarr = new JSONArray();
                            for(int i=0; i<preguntas.length;i++){
                                jarr.put(i,preguntas[i]);
                            }
                            salida.write(cip.doFinal(jarr.toString().getBytes()));
                            break;
                        case 7:
                            salida.write(cip.doFinal(db.obtenerFechaInicioVotacionActual().getBytes()));
                            break;
                        case 8:
                            salida.write(cip.doFinal(db.obtenerFechaFinVotacionActual().getBytes()));
                            break;
                        case 9:
                            boleta = json.getString("boleta");
                            perfil = json.getString("perfil");
                            String escuela = json.getString("escuela");
                            String nombre = json.getString("nombre");
                            String apPaterno = json.getString("ap_paterno");
                            String apMaterno = json.getString("ap_materno");
                            db.insertaParticipante(boleta,perfil,escuela);
                            db.insertaNombreParticipante(boleta, nombre, apPaterno, apMaterno);
                            String[] pregs = db.obtenerPreguntasVotacion(db.obtenerTituloVotacionActual());
                            for(String preg : pregs)
                                db.insertaParticipantePregunta(boleta,preg);
                            break;
                        case 10:
                            String[] perfiles = db.obtenerPerfiles();
                            JSONArray jsonArray = new JSONArray();
                            for(int i=0; i<perfiles.length;i++)
                                jsonArray.put(i,perfiles[i]);
                            salida.write(cip.doFinal(jsonArray.toString().getBytes()));
                            break;
                        case 11:
                            boleta = json.getString("boleta");
                            String[] pregsParticipante = db.consultaParticipantePreguntas(boleta);
                            JSONArray jsonArray1 = new JSONArray();
                            for(int i=0; i<pregsParticipante.length;i++)
                                jsonArray1.put(i,pregsParticipante[i]);
                            salida.write(cip.doFinal(jsonArray1.toString().getBytes()));
                            break;
                        case 12:
                            boleta = json.getString("boleta");
                            pregunta = json.getString("pregunta");
                            db.actualizaParticipantePregunta(boleta,pregunta);
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
        }catch(IOException e){//this can be very funy
            Log.d("SocketInputHandler", e.getMessage());
        }
    }

    private void take(){
        try {
            if (b == Byte.parseByte(String.valueOf((byte) ('\n' & 0xFF)))) { // Read bytes.
                byte[] bts = new byte[bytes.size()];
                for (int i = 0; i < bts.length; i++)
                    bts[i] = bytes.get(i);
                messages.add(new String(bts, "UTF-8"));
                bytes.clear();
                if (messages.size() >= 2) {
                    salida.write(8);
                    salida.flush(); // Confirm you've recieved all data.
                }
            } else
                bytes.add(b);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
