package org.inspira.polivoto.Threading;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.inspira.polivoto.Activity.ConfiguraParticipantesActivity;
import org.inspira.polivoto.Networking.IOHandler;
import org.inspira.polivoto.Networking.soap.ServiceClient;
import org.inspira.polivoto.Security.Hasher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

    private IOHandler ioHandler;
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
        ioHandler = new IOHandler(new DataInputStream(entrada), new DataOutputStream(salida));
        bytes = new ArrayList<>();
        messages = new ArrayList<>();
        if(hostsPostulados == null)
            hostsPostulados = new ArrayList<>();
    }

    public void setContext(Context context){
        this.context = context;
    }

    public void setRHost(String host){
        rHost = host.split(":")[0].substring(1, host.split(":")[0].length());
    }

    @Override
    public void run(){
        try{
            // Wait for the first byte and analyse it.
            cByte = ioHandler.readInt();
            Log.d("SocketHandler", "Guten tag2 " + rHost + "\t" + cByte);
            db = new Votaciones(context);
            // Make key Exchange.
            if(cByte == -2){
                JSONObject json;
                try {
                    json = new JSONObject(new String(chunk));
                    Votaciones v = new Votaciones(context);
                    JSONObject row;
                    JSONArray jarr = json.getJSONArray("content");
                    Log.d("RESCUER","GOT HERE WITH: " + jarr.toString());
                    switch (json.getInt("action")){
                        case 1:
                            for(int i=0; i<jarr.length(); i++){
                                row = jarr.getJSONObject(i);
                                Log.d("Mayunia","Inserting: " + jarr.toString());
                                Log.d("Maynunia","Inerted: " +v.insertaVoto2(Hasher.hexStringToByteArray(row.getString("idVoto")),
                                        row.getInt("idPerfil"),
                                        row.getString("voto"),
                                        row.getString("pregunta")));
                            }
                            break;
                        case 2:
                            for(int i=0; i<jarr.length(); i++){
                                row = jarr.getJSONObject(i);
                                v.insertaParticipante(row.getString("boleta"),
                                        row.getString("perfil"),
                                        row.getString("escuela"));
                            }
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if( cByte == Byte.parseByte(String.valueOf((byte) (-1 & 0xFF))) ) {
                try {
                    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                    kpg.initialize(1024);
                    KeyPair kp = kpg.genKeyPair();
                    Key publicKey = kp.getPublic();
                    Key privateKey = kp.getPrivate();
                    ioHandler.sendMessage(publicKey.getEncoded()); //** Successfuly sent public key **//
                    byte[] cipheredAESKey; // = new byte[128];
                    cipheredAESKey = ioHandler.handleIncommingMessage();
                    System.out.println("**********************************" + cipheredAESKey.length);
                    cip = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // That String is needed in ANDROID
                    cip.init(Cipher.DECRYPT_MODE, privateKey);
                    byte[] encodedAESKey = cip.doFinal(cipheredAESKey);
                    SecretKeySpec skp = new SecretKeySpec(encodedAESKey, "AES");
                    byte[] cipheredMessage = ioHandler.handleIncommingMessage();
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
                        if("Consultor".equals(json.getString("uName"))){
                            if(db.isVotacionActualGlobal()){
                                JSONObject js = new JSONObject();
                                js.put("action",3);
                                js.put("title", db.obtenerTituloVotacionActual());
                                js.put("place", db.obtenerUltimaEscuela());
                                js.put("host",rHost);
                                ServiceClient sc = new ServiceClient();
                                try {
                                    String resp = sc.sendAction(js.toString());
                                } catch (XmlPullParserException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        Log.d("La ruptura", "" + lid + ", lon: " + lon); // Hence you use the id to retrieve
                        // keep the sKey to know the content of the messages.
                    }else{
                        lid = -1;
                    }
                    ioHandler.writeInt(lid);
                } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | NoSuchPaddingException | BadPaddingException | JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                // Read the first byte to know which sKey to load so you can decipher the String.
                idAttempt = (cByte);
                SecretKeySpec sk = new SecretKeySpec(db.obtenerSKeyEncoded(idAttempt),"AES");
                chunk = ioHandler.handleIncommingMessage(); // Obtenemos los bytes cifrados.
                try {
                    cip = Cipher.getInstance("AES");
                    cip.init(Cipher.DECRYPT_MODE, sk);
                    String jstr = new String(cip.doFinal(chunk));
                    JSONObject json = new JSONObject(jstr);
                    // En el objecto JSON esperamos encontrar al usuario y a sus intenciones
                    db.insertaUserAction(idAttempt, jstr);
                    // La tarea a realizar est&aacute; indicada por un entero.
                    int requestedAction = json.getInt("action");
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
                    // 14 Pide agente externo validación de boleta.
                    // 15 Pide cantidad de votos al momento.
                    cip.init(Cipher.ENCRYPT_MODE, sk);
                    Log.d("TULMAN","Ras " + idAttempt + ", " + jstr);
                    switch(requestedAction){
                        case 1: // 1 Postulate me!.
                            action1();
                            break;
                        case 2:// 2 Pide validación de boleta.
                            action2(json);
                            break;
                        case 3: // 3 Entrega voto.
                            action3(json);
                            break;
                        case 4: // 4 Pide conteo de votos.
                            action4(json);
                            break;
                        case 5:  // 5 Pide título de votación.
                            action5();
                            break;
                        case 6:  // 6 Pide preguntas de votación.
                            action6();
                            break;
                        case 7:  // 7 Pide fecha de inicio de votación.
                            action7();
                            break;
                        case 8:  // 8 Pide fecha de término de votación.
                            action8(json);
                            break;
                        case 9: // 9 Alta participante.
                            action9(json);
                            break;
                        case 10: // 10 Pide perfiles.
                            action10();
                            break;
                        case 11: // 11 Pide preguntas para participante.
                            action11(json);
                            break;
                        case 12:  // 12 Participante contestó pregunta.
                            action12(json);
                            break;
                        case 13:  // 13 Pide opciones de pregunta.
                            action13(json);
                            break;
                        case 14: // 14 Pide agente externo validación de boleta.
                            action14(json);
                            break;
                        case 15: // 15 Pide cantidad de votos al momento.
                            action15();
                            break;
                        default:
                    }
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | JSONException | InvalidKeyException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            }
        }catch(IOException e){//this can be very funy
            e.printStackTrace();
        } catch (IllegalArgumentException e){
            e.printStackTrace();
            Log.d("IllegalAccess", "Alguien no identificado intentó acceder, con id: " + idAttempt + " y host: " + rHost);
        }finally {
            try {
                ioHandler.close();
            }catch(IOException e){
                Log.e("From finally","Error closing everything?");
            }
        }
    }

    private void action1() throws IOException, BadPaddingException, IllegalBlockSizeException, JSONException {
        if("Participante".equals(db.obtenerUsuarioPorIdAttempt(idAttempt))){
            hostsPostulados.add(rHost);
            // Participante may need to wait until a new Capturista contacts it
        }else if ("Capturista".equals(db.obtenerUsuarioPorIdAttempt(idAttempt))) {
            Log.d("HostsPostulados",String.valueOf(hostsPostulados.size()));
            while (hostsPostulados.size() == 0);
            JSONObject json = new JSONObject();
            json.put("nHost", hostsPostulados.remove(hostsPostulados.size() - 1));
            json.put("key", Hasher.bytesToString(db.obtenerKeyByHost(json.getString("nHost"))));
            chunk = cip.doFinal(json.toString().getBytes());
            ioHandler.sendMessage(chunk);
            ioHandler.close();
        }
    }

    private void action2(JSONObject json) throws IOException, BadPaddingException, IllegalBlockSizeException, JSONException {
        // Las siguientes dos líneas deben ser implementación del servicio.
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean usarMatricula = sharedPref.getBoolean(ConfiguraParticipantesActivity.USAR_MATRICULA_KEY, false);
        String boleta = json.getString("boleta");
        if(db.consultaExistenciaBoleta(boleta, db.obtenerTituloVotacionActual())){
            resp = 1;
        }else{
            resp = 0;
        }
        if(resp == 0 && !usarMatricula){
            action9(json);
        }
        // Termina proceso de validación local.
        // Debemos revisar si se está participando en una votación global y validar también.
        // Debemos contactar a la entidad encargada de hacer la validación remota y esperar.
        if(db.isVotacionActualGlobal()) {
            int consultorIdAttepmt = db.grabLastUserIdAttmptSucceded("Consultor");
            String host = db.grabHostForUserLoginAttempt(consultorIdAttepmt);
            Log.d("Lover","Connecting to the capturista at: " + host + ":5010");
            Socket socket = new Socket(host,5010);
            JSONObject j = new JSONObject();
            j.put("boleta",new Hasher().makeHashString(boleta));
            j.put("action",1);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.write(j.toString().getBytes().length);
            out.write(j.toString().getBytes());
            out.flush();
            Log.d("Lover","Data sent (" + j.toString() + "), waiting for response...");
            DataInputStream in = new DataInputStream(socket.getInputStream());
            byte[] bts = new byte[in.read()];
            in.read(bts);
            Log.d("Lover","Response has arrived (" + j.toString() + ")");
            j = new JSONObject(new String(bts));
            resp = j.getInt("response");
            socket.close();
        }
        chunk = cip.doFinal(String.valueOf(resp).getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }

    private void action3(JSONObject json) throws IOException, BadPaddingException, IllegalBlockSizeException, JSONException {
        byte[] idVoto = Hasher.hexStringToByteArray(json.getString("idVoto"));
        String pregunta = json.getString("pregunta");
        int idVotacion = db.obtenerIdVotacionFromPregunta(pregunta);
        String perfil = json.getString("perfil");
        String voto = json.getString("voto");
        if(db.insertaVoto(idVoto,idVotacion,perfil,voto,idAttempt,db.obtenerIdPregunta(pregunta))!=-1)
            resp = 1;
        else
            resp = 0;
        chunk = cip.doFinal(String.valueOf(resp).getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }

    private void action4(JSONObject json) throws IOException, BadPaddingException, IllegalBlockSizeException, JSONException {
        /***************************************************************
         *
         * 		El Consultor pregunta con un título cuales son los resultados
         * 	que desea obtener, así, se debe llamar a "consultarVotos", pasando
         * 	el nombre de la pregunta como parámetro de búsqueda y armando una
         * 	lista ligada con los títulos de las opciones @ número de votos.
         *
         **********************************************************************/
        String pregunta = json.getString("pregunta");
        LinkedList<String> resultados = db.obtenerResultadosPorPregunta(pregunta, db.obtenerIdVotacionFromPregunta(pregunta));
        // Podría darse el caso en que mejor se cambie a ObjectOutputStream y se mande la lista ligada.
        JSONObject jsresp;
        String[] pair;
        JSONArray jarr = new JSONArray();
        for(String str : resultados) {
            pair = str.split("@");
            jsresp = new JSONObject();
            jsresp.put("reactivo",pair[0]);
            jsresp.put("cantidad", pair[1]);
            jarr.put(jsresp);
        }
        chunk = cip.doFinal(jarr.toString().getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }

    private void action5() throws IOException, BadPaddingException, IllegalBlockSizeException {
        String titulo = db.obtenerTituloVotacionActual();
        chunk = cip.doFinal(titulo.getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }

    private void action6() throws JSONException, BadPaddingException, IllegalBlockSizeException, IOException {
        String preguntas[] = db.obtenerPreguntasVotacion(db.obtenerTituloVotacionActual());
        JSONArray jarr = new JSONArray();
        for(int i=0; i<preguntas.length;i++){
            jarr.put(i,preguntas[i]);
        }
        chunk = cip.doFinal(jarr.toString().getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }

    private void action7() throws BadPaddingException, IllegalBlockSizeException, IOException {
        chunk = cip.doFinal(db.obtenerFechaInicioVotacionActual().getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }

    private void action8(JSONObject json) throws BadPaddingException, IllegalBlockSizeException, IOException {
        chunk = cip.doFinal(db.obtenerFechaFinVotacionActual().getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }

    private void action9(JSONObject json) throws JSONException {
        String boleta = null;
        try{boleta = json.getString("boleta");}catch(JSONException e){}
        String perfil = null;
        try{perfil = json.getString("perfil");}catch(JSONException e){}
        String escuela = null;
        try{escuela = json.getString("escuela");}catch(JSONException e){}
        String nombre = null;
        try{nombre = json.getString("nombre");}catch(JSONException e){}
        String apPaterno = null;
        try{apPaterno = json.getString("ap_paterno");}catch(JSONException e){}
        String apMaterno = null;
        try{apMaterno = json.getString("ap_materno");}catch(JSONException e){}
        if(db.insertaParticipante(boleta,perfil == null ? "" : perfil,escuela == null ? "" : escuela) == -1){
            // Si no se proporcionan
            resp = db.insertaParticipante(boleta,db.obtenerPerfiles()[0],db.obtenerUltimaEscuela());
        }
        if( nombre != null && apPaterno != null && apMaterno != null)
            if(db.insertaNombreParticipante(boleta, nombre, apPaterno, apMaterno) == -1 )
                resp = -1;
        String[] pregs = db.obtenerPreguntasVotacion(db.obtenerTituloVotacionActual());
        for(String preg : pregs) { // Habilita una pregunta para cada participante.
            if( db.insertaParticipantePregunta(boleta, preg) == -1 )
                resp = -1;
        }
        if(resp != -1)
            resp = 1;
    }

    public void action10() throws BadPaddingException, IllegalBlockSizeException, JSONException, IOException {
        String[] perfiles = db.obtenerPerfiles();
        JSONArray jsonArray = new JSONArray();
        for(int i=0; i<perfiles.length;i++)
            jsonArray.put(i,perfiles[i]);
        chunk = cip.doFinal(jsonArray.toString().getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }

    public void action11(JSONObject json) throws JSONException, BadPaddingException, IllegalBlockSizeException, IOException {
        String boleta = json.getString("boleta");
        String[] pregsParticipante = db.consultaParticipantePreguntas(boleta, db.obtenerTituloVotacionActual());
        JSONArray jsonArray1 = new JSONArray();
        for(int i=0; i<pregsParticipante.length;i++)
            jsonArray1.put(pregsParticipante[i]);
        chunk = cip.doFinal(jsonArray1.toString().getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }

    public void action12(JSONObject json) throws JSONException {
        String boleta = json.getString("boleta");
        String pregunta = json.getString("pregunta");
        db.actualizaParticipantePregunta(boleta, pregunta);
    }

    public void action13(JSONObject json) throws IOException, JSONException, BadPaddingException, IllegalBlockSizeException {
        chunk = cip.doFinal(db.obtenerOpcionesPregunta(json.getString("pregunta")).toString().getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }

    public void action14(JSONObject json) throws BadPaddingException, IllegalBlockSizeException, IOException, JSONException {
        // Las siguientes dos líneas deben ser implementación del servicio.
        String boleta = json.getString("boleta");
        Log.d("Dragon Slayer", "Estamos por comprobar una boleta extrangera! La boleta es: " + boleta);
        if(db.consultaRemotaExistenciaBoleta(boleta, db.obtenerTituloVotacionActual())){
            resp = 1;
        }else{
            resp = 0;
        }
        chunk = cip.doFinal(String.valueOf(resp).getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }

    public void action15() throws IOException, BadPaddingException, IllegalBlockSizeException {
        int cantidad = db.obtenerCantidadParticipantes(db.obtenerTituloVotacionActual());
        // Actualmente el criterio para la decisión es que la hora actual sea distinta de la de fin.
        String lugar = db.obtenerUltimaEscuela();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean usarMatricula = sharedPref.getBoolean(ConfiguraParticipantesActivity.USAR_MATRICULA_KEY, false);
        Integer poblacion;
        Log.e("Painter","Painting my destiny");
        if(usarMatricula)
            poblacion = db.cantidadUsuariosRegistradosVotacionActual();
        else
            poblacion = null;
        JSONObject json = new JSONObject();
        try{
            json.put("poblacion",poblacion);
            json.put("votos",cantidad);
            json.put("lugar",lugar);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            String fechaFin = db.obtenerFechaFinVotacionActual();
            Date fFin = sdf.parse(fechaFin);
            Calendar cF = Calendar.getInstance();
            long tul = (fFin.getTime()-new Date().getTime());
            Log.d("48435468432468", "" + tul);
            if( tul > 0) {
                cF.setTime(new Date(fFin.getTime() - new Date().getTime()));
                int horas = cF.get(Calendar.HOUR);
                int minutos = cF.get(Calendar.MINUTE);
                int segundos = cF.get(Calendar.SECOND);
                json.put("horas",horas);
                json.put("minutos", minutos);
                json.put("segundos", segundos);
            }else{
                json.put("horas", null);
                json.put("minutos", null);
                json.put("segundos", null);
            }
        }catch(ParseException | JSONException e){
            e.printStackTrace();
        }
        Log.e("Painter",json.toString());
        chunk = cip.doFinal(json.toString().getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
    }
}