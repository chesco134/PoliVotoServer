package org.inspira.polivoto.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import org.inspira.polivoto.Networking.soap.ServiceClient;
import org.inspira.polivotoserver.MiServicio;
import org.inspira.polivotoserver.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import DataBase.Votaciones;

/**
 * Created by jcapiz on 1/12/15.
 */
public class ConfiguraParticipantesActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    public static final String USAR_MATRICULA_KEY = "usar_matricula_pref_key";
    public static final String NOMBRE_ARCHIVO_MATRICULA_KEY = "matricula_file_name_pref_key";

    private ListPreference votacionesDisponibles;
    private JSONArray jarr;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        votacionesDisponibles = (ListPreference) getPreferenceScreen()
                .findPreference(getResources().getString(R.string.servidor_global));
        votacionesDisponibles.setEntries(new CharSequence[0]);
        votacionesDisponibles.setEntryValues(new CharSequence[0]);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                           String key) {
        Votaciones v = new Votaciones(this);
        if (key.equals(USAR_MATRICULA_KEY)) {
            if(!v.existeLoginAttemptAdmin()) {
                int id = v.insertaLoginAttempt("Administrador", "localhost");
                v.insertaAttemptSucceded(id,new byte[]{(byte)(1&0xFF)});
            }
            v.insertaUserAction(v.grabAdminLoginAttempt(), key + " turned to " + sharedPreferences.getBoolean(key, false));
        }else if(key.equals(NOMBRE_ARCHIVO_MATRICULA_KEY)){
            Preference connectionPref = findPreference(key);
            // Set summary to be the user-description for the selected value
            connectionPref.setSummary(sharedPreferences.getString(key, ""));
        }else if(key.equals(getResources().getString(R.string.servidor_global))){
            new VotacionDownloader(sharedPreferences).execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this, "rqst: " + requestCode + ", rslt: " + resultCode, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPostResume(){
        super.onPostResume();
        if(jarr == null)
            new GlobalVotingListFiller().execute();
        else
            votacionesDisponibles.setTitle("Cargando...");
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString("jarr", jarr.toString());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        try {
            jarr = new JSONArray(savedInstanceState.getString("jarr"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void reloadList(){
        new GlobalVotingListFiller().execute();
    }

    private class GlobalVotingListFiller extends AsyncTask<String,JSONArray,String> {

        @Override
        protected String doInBackground(String... args){
            String result = null;
            try {
                 result = new ServiceClient().sendAction("{\"action\":\"5\"}");
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result){
            try {
                jarr = new JSONArray(result);
                CharSequence[] entries = new CharSequence[jarr.length()];
                String tituloActual = new Votaciones(ConfiguraParticipantesActivity.this).obtenerTituloVotacionActual();
                if(tituloActual == null)
                    tituloActual = "";
                for(int i=0; i<jarr.length();i++){
                    if(!tituloActual.equals(jarr.getString(i))) {
                        entries[i] = jarr.getJSONObject(i).getString("Title");
                        Log.d("GlobalVotingListFiller", entries[i] + " added");
                    }
                }
                votacionesDisponibles.setEntries(entries);
                votacionesDisponibles.setEntryValues(entries);
            } catch (NullPointerException e) {
                new Timer().schedule(new TimerTask(){
                    @Override
                    public void run(){
                        reloadList();
                        Log.d("GlobalVotingListFiller", "Reconnecting...");
                    }
                },3000);
                e.printStackTrace();
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    private class VotacionDownloader extends AsyncTask<String,String,String>{

        private SharedPreferences sharedPreferences;

        public VotacionDownloader(SharedPreferences sp){
            sharedPreferences = sp;
        }

        protected String doInBackground(String... args){
            String result;
            JSONObject json = new JSONObject();
            Votaciones v = new Votaciones(ConfiguraParticipantesActivity.this);
            try {
                json.put("action", 7);
                json.put("title", sharedPreferences.getString(getResources().getString(R.string.servidor_global), null));
                Log.d("SPL",json.getString("title"));
                ServiceClient sc = new ServiceClient();
                json = new JSONObject(sc.sendAction(json.toString()));
                JSONArray jarr = json.getJSONArray("quiz");
                String title = json.getString("title");
                v.insertaVotacion(title, json.getString("StartDate"), null);
                v.setVotacionActualAsGlobal();
                v.conservaFechaFinVotacionActual(title,json.getString("FinishDate"));
                for(int i=0;i<jarr.length();i++){
                    JSONObject js = jarr.getJSONObject(i);
                    String pregunta = js.getString("pregunta");
                    JSONArray opciones = js.getJSONArray("opciones");
                    v.insertaPregunta(pregunta,title);
                    for(int j=0; j<opciones.length();j++){
                        v.insertaOpcion(opciones.getString(i));
                        v.insertaPreguntaOpcion(pregunta,opciones.getString(i));
                    }
                }
                json = new JSONObject();
                json.put("action", 2);
                json.put("Name", v.obtenerUltimaEscuela());
                sc.sendAction(json.toString());
                if(startService(new Intent(ConfiguraParticipantesActivity.this, MiServicio.class)) != null )
                    result = "¡Listo!";
                else
                    result = "Error al iniciar el servicio";
            } catch (JSONException | XmlPullParserException | IOException e) {
                e.printStackTrace();
                result = "Servicio temporalmente no disponible";
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result){
            Toast.makeText(ConfiguraParticipantesActivity.this,result,Toast.LENGTH_SHORT).show();
            if("¡Listo!".equals(result))
                finish();
        }
    }
}