package org.inspira.polivoto.Threading;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.inspira.polivoto.Activity.VotacionesConf;
import org.inspira.polivoto.Networking.soap.ServiceClient;
import org.inspira.polivoto.Security.Cifrado;
import org.inspira.polivoto.Security.MD5Hash;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import DataBase.Votaciones;
import Shared.ResultadoVotacion;

/**
 * Created by jcapiz on 5/12/15.
 */


public class TerminaVotacionGlobal extends AsyncTask<CharSequence,String,String> {

    Activity context;

    public TerminaVotacionGlobal(Activity context){
        this.context = context;
    }

    @Override
    protected String doInBackground(CharSequence... args){
        String response;
        Votaciones v = new Votaciones(context);
        JSONObject json = new JSONObject();
        try {
            json.put("action", 6);
            json.put("title", v.obtenerTituloVotacionActual());
            response = new ServiceClient().sendAction(json.toString());
            if ("success".equals(response)) {
                String[] rows = v.consultaVoto(v.obtenerTituloVotacionActual());
                String[] settings = v.quienesHanParticipado(v.obtenerTituloVotacionActual());
                String[] votando = v.consultaVotando(v.obtenerTituloVotacionActual());
                String[] logs = v.obtenerLog();
                Cifrado cipher = new Cifrado("MyPriceOfHistory");
                byte[][] votosCifrados = new byte[rows.length][];
                byte[][] participantesCifrados = new byte[settings.length][];
                byte[][] votandoCifrados = new byte[votando.length][];
                byte[][] logsCifrados = new byte[logs.length][];
                Log.d("Capiz", "Tenemos " + rows.length + " votos.");
                for(int index = 0; index < rows.length; index++){
                    Log.d("Capiz",rows[index]);
                }
                for(int index = 0; index<rows.length; index++){
                    votosCifrados[index] = cipher.cipher(rows[index]);
                }
                for(int index = 0; index<settings.length; index++){
                    participantesCifrados[index] = cipher.cipher(settings[index]);
                }
                for(int index = 0; index<votando.length; index++){
                    votandoCifrados[index] = cipher.cipher(votando[index]);
                }
                for(int index = 0; index<logs.length; index++){
                    logsCifrados[index] = cipher.cipher(logs[index]);
                }
                ResultadoVotacion resultadoFinalVotos = new ResultadoVotacion(votosCifrados);
                ResultadoVotacion resultadoFinalParticipantes = new ResultadoVotacion(participantesCifrados);
                ResultadoVotacion resultadoFinalVotando = new ResultadoVotacion(votandoCifrados);
                ResultadoVotacion resultadoFinalLogs = new ResultadoVotacion(logsCifrados);
                try{
                    ObjectOutputStream salidaArchivo = new ObjectOutputStream(new FileOutputStream(VotacionesConf.RESULTS_FILE));
                    salidaArchivo.writeObject(resultadoFinalVotos);
                    salidaArchivo.writeObject(resultadoFinalParticipantes);
                    salidaArchivo.writeObject(resultadoFinalVotando);
                    salidaArchivo.writeObject(resultadoFinalLogs);
                    salidaArchivo.close();
                    String[] chunky = v.consultaVotando(v.obtenerTituloVotacionActual());
                    String[] tmp;
                    for(String str : chunky){
                        tmp = str.split(",");
                        String perfil = v.obtenerPerfilDeUsuario(tmp[0]);
                        int idPregunta = Integer.parseInt(tmp[1]);
                        String pregunta = "";
                        for(int i=2; i<tmp.length;i++)
                            pregunta = pregunta.concat(tmp[i]);
                        String voto = "Anular mi voto";
                        v.insertaVoto(new MD5Hash().makeHashForSomeBytes(tmp[0]),v.obtenerIdVotacionFromPregunta(pregunta),perfil,voto,v.grabAdminLoginAttempt(),idPregunta);
                    }
                    response = v.terminaUltimaVotacion() ? "Hecho" : ":/";
                    Log.d("Finisher Task", "Votación terminada");
                }catch(IOException ex){
                    response =  "Error al finalizar la votación:\n" + ex.toString();
                }
            } else {
                Log.d("Finisher Task", "Servicio no disponible");
                response = ("Servicio no disponible\n" + response);
            }
        } catch (JSONException | IOException | XmlPullParserException e) {
            e.printStackTrace();
            response = ("Servicio por el momento no disponible");
        }
        v.close();
        return response;
    }

    @Override
    protected void onPostExecute(String arg){
        Toast.makeText(context, arg, Toast.LENGTH_LONG).show();
        ((VotacionesConf)context).quitaActividad();
        //((VotacionesConf)context).detenServicio();
    }
}
