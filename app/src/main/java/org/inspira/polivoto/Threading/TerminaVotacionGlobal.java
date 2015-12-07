package org.inspira.polivoto.Threading;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.inspira.polivoto.Activity.VotacionesConf;
import org.inspira.polivoto.Networking.soap.ServiceClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import DataBase.Votaciones;

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
                response = v.terminaUltimaVotacion() ? "Hecho" : ":/";
                Log.d("Finisher Task", "Votación terminada");
            } else {
                Log.d("Finisher Task", "Servicio no disponible");
                response = ("Servicio no disponible\n" + response);
            }
        } catch (JSONException | IOException | XmlPullParserException e) {
            e.printStackTrace();
            response = ("Servicio por el momento no disponible");
        }
        return response;
    }

    @Override
    protected void onPostExecute(String arg){
        Toast.makeText(context, arg, Toast.LENGTH_LONG).show();
        ((VotacionesConf)context).quitaActividad();
    }
}
