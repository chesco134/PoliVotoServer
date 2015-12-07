package org.inspira.polivoto.Threading;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import org.inspira.polivoto.Activity.VotacionesConf;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DataBase.Votaciones;

/**
 * Created by jcapiz on 5/12/15.
 */
public class RegistraVotacionGlobal extends AsyncTask<String,String,String> {

    private int consultorIdAttempt;
    private Activity activity;

    public RegistraVotacionGlobal(Activity activity, int consultorIdAttempt){
        this.activity = activity;
        this.consultorIdAttempt = consultorIdAttempt;
    }

    @Override
    protected String doInBackground(String... args){
        String result;
        Votaciones v = new Votaciones(activity);
        try {
            JSONObject json = new JSONObject();
            json.put("Title", v.obtenerTituloVotacionActual());
            json.put("StartDate", v.obtenerFechaInicioVotacionActual());
            json.put("FinishDate", v.obtenerFechaFinVotacionActual());
            HandleSoapRequest handler = new HandleSoapRequest();
            handler.setAction(1);
            handler.setJson(json);
            handler.start();
            handler.join();
            if(!handler.error()) {
                json = new JSONObject();
                json.put("Name", v.obtenerUltimaEscuela());
                handler = new HandleSoapRequest();
                handler.setJson(json);
                handler.setAction(2);
                handler.start();
                handler.join();
                if(!handler.error()) {
                    json = new JSONObject();
                    json.put("title", v.obtenerTituloVotacionActual());
                    json.put("place", v.obtenerUltimaEscuela());
                    json.put("host", v.grabHostForUserLoginAttempt(consultorIdAttempt));
                    handler = new HandleSoapRequest();
                    handler.setAction(3);
                    handler.setJson(json);
                    handler.start();
                    handler.join();
                    if(!handler.error()) {
                        json = new JSONObject();
                        JSONArray content = new JSONArray();
                        String[] preguntas = v.obtenerPreguntasVotacion(v.obtenerTituloVotacionActual());
                        JSONArray opsPregunta;
                        JSONObject row;
                        for (String str : preguntas) {
                            row = new JSONObject();
                            opsPregunta = v.obtenerOpcionesPregunta(str);
                            row.put("pregunta", str);
                            row.put("opciones", opsPregunta);
                            content.put(row);
                        }
                        json.put("title", v.obtenerTituloVotacionActual());
                        json.put("quiz", content);
                        handler = new HandleSoapRequest();
                        handler.setJson(json);
                        handler.setAction(4);
                        handler.start();
                        handler.join();
                        if(!handler.error()) {
                            v.setVotacionActualAsGlobal();
                            result = "¡Listo!";
                        }else
                            result = "Servicio por el momento no disponible";
                    }else
                        result = "Servicio por el momento no disponible";
                }else
                    result = "Servicio por el momento no disponible";
            }else
                result = "Servicio por el momento no disponible";
        } catch (InterruptedException | JSONException e) {
            e.printStackTrace();
            result = "Servicio interrumpido";
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result){
        Toast.makeText(activity,result,Toast.LENGTH_SHORT).show();
        ((VotacionesConf)activity).quitaActividad();
    }
}