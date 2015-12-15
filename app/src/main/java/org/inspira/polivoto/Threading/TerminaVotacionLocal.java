package org.inspira.polivoto.Threading;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.inspira.polivoto.Activity.VotacionesConf;
import org.inspira.polivoto.Security.Cifrado;
import org.inspira.polivoto.Security.MD5Hash;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import DataBase.Votaciones;
import Shared.ResultadoVotacion;

/**
 * Created by jcapiz on 7/12/15.
 */
public class TerminaVotacionLocal extends AsyncTask<String,String,String> {

    private Activity ctx;

    public TerminaVotacionLocal(Activity ctx){
        this.ctx = ctx;
    }

    @Override
    protected String doInBackground(String... args){
        String result;
        Votaciones v = new Votaciones(ctx);
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
            v.terminaUltimaVotacion();
            v.terminarProceso();
            result = "Éxito al finalizar la votación!";
        }catch(IOException ex){
            result = "Error al finalizar la votación:\n" + ex.toString();
        }
        v.close();
        return result;
    }

    @Override
    protected void onPostExecute(String result){
        Toast.makeText(ctx, result, Toast.LENGTH_LONG).show();
        ((VotacionesConf)ctx).quitaActividad();
        //((VotacionesConf)ctx).detenServicio();
    }
}
