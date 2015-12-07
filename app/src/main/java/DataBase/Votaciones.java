package DataBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.inspira.polivoto.Activity.ConfiguraParticipantesActivity;
import org.inspira.polivoto.Security.Hasher;
import org.inspira.polivoto.Security.MD5Hash;
import org.json.JSONArray;

import Shared.Pregunta;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class Votaciones extends SQLiteOpenHelper{

    private String FILE_NAME = Environment.getExternalStorageDirectory().getAbsolutePath();
	private Context ctx;
	public Votaciones(Context context){
        super(context, "PoliVoto Electrónico", null, 1);
		ctx = context;
        FILE_NAME = FILE_NAME.concat("/"+PreferenceManager.getDefaultSharedPreferences(ctx).getString(ConfiguraParticipantesActivity.NOMBRE_ARCHIVO_MATRICULA_KEY, "/chu"));
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		if (!db.isReadOnly()) {
			// Enable foreign key constraints
			db.execSQL("PRAGMA foreign_keys=ON;");
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	@Override
	public void onCreate(SQLiteDatabase dataBase) {
        dataBase.execSQL("create table Perfil(idPerfil INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, perfil text not null)");
        dataBase.execSQL("create table SubPerfil(idSubPerfil INTEGER NOT NULL, idPerfil INTEGER NOT NULL, SubPerfil text not null, primary key(idSubPerfil), foreign key(idPerfil) references Perfil(idPerfil))");
		dataBase.execSQL("create table Escuela(idEscuela INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, Nombre text not null, latitud real, longitud real)");
		dataBase.execSQL("create table Participante(Boleta TEXT, idPerfil INTEGER NOT NULL, idEscuela INTEGER NOT NULL, Fecha_Registro TEXT, BoletaHash BLOB not null, PRIMARY KEY(Boleta), foreign key(idPerfil) references Perfil(idPerfil), foreign key(idEscuela) references Escuela(idEscuela));");
        dataBase.execSQL("create table NombreParticipante(Boleta TEXT NOT NULL, Nombre TEXT NOT NULL, ApPaterno TEXT NOT NULL, ApMaterno TEXT NOT NULL, primary key(Boleta), foreign key(Boleta) references Participante(Boleta))");

        dataBase.execSQL("create table Votacion(idVotacion INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, Titulo text not null, Fecha_Inicio DATE NOT NULL, Fecha_Fin DATE, isGlobal INTEGER DEFAULT 0)");
        dataBase.execSQL("create table VotacionFechaFin(idVotacion INTEGER NOT NULL PRIMARY KEY, Fecha_Fin DATE not null, foreign key(idVotacion) references Votacion(idVotacion));");
		// Debo preguntar acerca de tener un idPregunta como entero. ¿Podría sólo dejar como pk a Pregunta y hacer que idVotacion forme parte de la pk? (Relación identificadora)
        dataBase.execSQL("create table Pregunta(idPregunta INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, Pregunta TEXT not null, idVotacion INTEGER NOT NULL, FOREIGN KEY(idVotacion) REFERENCES Votacion(idVotacion));");
		dataBase.execSQL("create table Opcion(idOpcion INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, Reactivo TEXT not null);");
		dataBase.execSQL("create table Pregunta_Opcion(idPregunta INTEGER NOT NULL, idOpcion INTEGER NOT NULL, PRIMARY KEY(idPregunta,idOpcion), FOREIGN KEY(idPregunta) REFERENCES Pregunta(idPregunta), FOREIGN KEY(idOpcion) REFERENCES Opcion(idOpcion));");

		dataBase.execSQL("create table Usuario(idUsuario INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, Name text not null, Psswd blob not null)");
		dataBase.execSQL("create table LoginAttempt(idLoginAttempt INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, idUsuario INTEGER NOT NULL, Attempt_Timestamp text not null, Host text not null, foreign key(idUsuario) references Usuario(idUsuario))");
		dataBase.execSQL("create table AttemptSucceded(idLoginAttempt not null, secretKey BLOB not null, primary key(idLoginAttempt), foreign key(idLoginAttempt) references LoginAttempt(idLoginAttempt))");
		dataBase.execSQL("create table UserAction(idUserAction INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, idLoginAttempt INTEGER NOT NULL, Action text not null, Action_Timestamp text not null, foreign key(idLoginAttempt) references AttemptSucceded(idLoginAttempt))");
        dataBase.execSQL("create table ActiveUsers(idLoginAttempt INTEGER NOT NULL,  text not null, Action_Timestamp text not null, foreign key(idLoginAttempt) references AttemptSucceded(idLoginAttempt))");

        // Recuerda que el "null hack" son tres guiones. Sólo insertas registros de quienes son capturados al momento de validación.
		dataBase.execSQL("create table Participante_Pregunta(Boleta TEXT not null, idPregunta INTEGER NOT NULL, Hora_Registro text not null, Hora_Participacion text, PRIMARY KEY(Boleta,idPregunta), FOREIGN KEY(Boleta) REFERENCES Participante(Boleta), FOREIGN KEY(idPregunta) REFERENCES Pregunta(idPregunta));");
		dataBase.execSQL("create table Voto(idVoto blob not null, idVotacion INTEGER NOT NULL, idPerfil INTEGER NOT NULL, Voto text not null, idLoginAttempt INTEGER NOT NULL, idPregunta INTEGER NOT NULL, primary key(idVoto), FOREIGN KEY(idVotacion) REFERENCES Votacion(idVotacion), foreign key(idPerfil) references Perfil(idPerfil), foreign key(idLoginAttempt) references AttemptSucceded(idLoginAttempt), foreign key(idPregunta) references Pregunta(idPregunta));");

        // Vista que sirve para tener a la mano las preguntas totales de cada votación.
        dataBase.execSQL("create view if not exists Preguntas_Votacion as select idVotacion,count(*) as Preguntas from Pregunta group by idVotacion");
    }

    public void setVotacionActualAsGlobal(){
        ContentValues values = new ContentValues();
        values.put("isGlobal", 1);
        if(getReadableDatabase().update("Votacion",values,"Fecha_Fin is null or Fecha_Fin = '---'", null) > 0)
            Log.d("Megashinka","There are rows");
        else
            Log.d("Megashinka", "There are NOT rows");
        close();
    }

    public boolean isVotacionActualGlobal(){
        boolean isGlobal = false;
        Cursor c = getReadableDatabase().rawQuery("select isGlobal from Votacion where Fecha_Fin is null or Fecha_Fin = '---'", null);
        if(c.moveToNext())
            isGlobal = c.getInt(c.getColumnIndex("isGlobal")) == 1;
        c.close();
        close();
        return isGlobal;
    }

    public String grabHostForUserLoginAttempt(int idLoginAttempt){ // Usado para registar una votación global.
        Cursor c = getReadableDatabase().rawQuery("select Host from LoginAttempt where idLoginAttempt = CAST(? as INTEGER)",new String[]{String.valueOf(idLoginAttempt)});
        String host = null;
        if(c.moveToFirst())
            host = c.getString(c.getColumnIndex("Host"));
        c.close();
        close();
        return host;
    }

    public int grabLastUserIdAttmptSucceded(String usuario){
        int idAttempt = -1;
        Cursor c = getReadableDatabase().rawQuery("select idLoginAttempt,Attempt_Timestamp from Usuario join (select idUsuario,AttemptSucceded.idLoginAttempt,Attempt_Timestamp from LoginAttempt join AttemptSucceded using(idLoginAttempt)) r using(idUsuario) where Name = ? order by Attempt_Timestamp",new String[]{usuario});
        if(c.moveToLast()){
            idAttempt = c.getInt(c.getColumnIndex("idLoginAttempt"));
            c.moveToFirst();
            Log.d("Jirachi","Consultaron: " + c.getString(c.getColumnIndex("Attempt_Timestamp")));
            while(c.moveToNext())
                Log.d("Jirachi",c.getInt(c.getColumnIndex("idLoginAttempt")) + "Consultaron: " + c.getString(c.getColumnIndex("Attempt_Timestamp")));
        }else{
            Log.d("Jirachi", "NO USERS??");
        }
        c.close();
        close();
        return idAttempt;
    }

    public int grabAdminLoginAttempt(){
        Cursor c = getReadableDatabase().rawQuery("select idLoginAttempt from LoginAttempt join Usuario on Usuario.idUsuario = LoginAttempt.idUsuario where Name = 'Administrador'",null);
        int id = -2;
        if(c.moveToNext())
            id = c.getInt(c.getColumnIndex("idLoginAttempt"));
        c.close();
        close();
        return id;
    }

    public boolean existeLoginAttemptAdmin(){
        Cursor c = getReadableDatabase().rawQuery("select * from LoginAttempt where idLoginAttempt = -1",null);
        boolean exists = (c.getCount() > 0);
        c.close();
        close();
        return exists;
    }

    public String[] consultaParticipantePreguntasTUL(String boleta, String tituloVotacion){
        Cursor c0 = getReadableDatabase().rawQuery("select idVotacion from Votacion where Titulo = ?",new String[]{String.valueOf(tituloVotacion)});
        String[] preguntasFaltantes = null;
        if(c0.moveToNext()){
            int idVotacion = c0.getInt(c0.getColumnIndex("idVotacion"));
            String args[] = {boleta,String.valueOf(idVotacion)};
            Cursor c = getReadableDatabase().rawQuery("select Pregunta,Hora_Registro,Hora_Participacion from (Pregunta join (select idPregunta,Hora_Registro,Hora_Participacion from Participante_Pregunta where Boleta = ? ) t on Pregunta.idPregunta = t.idPregunta) where Pregunta.idVotacion = CAST(? as INTEGER)",args);
            preguntasFaltantes = new String[c.getCount()];
            int i = 0;
            while(c.moveToNext())
                preguntasFaltantes[i++] = c.getString(c.getColumnIndex("Pregunta")) + ", HR: " + c.getString(c.getColumnIndex("Hora_Registro")) + ", HP " + c.getString(c.getColumnIndex("Hora_Participacion"));
            c.close();
        }
        c0.close();
        close();
        return preguntasFaltantes;
    }

    public void revertLastVotacion(){
        getWritableDatabase().delete("Votacion", "Fecha_Fin is null or Fecha_Fin = '---'", null);
        close();
    }

    public String[] consultaParticipantePreguntas(String boleta, String tituloVotacion){
        Cursor c0 = getReadableDatabase().rawQuery("select idVotacion from Votacion where Titulo = ?",new String[]{String.valueOf(tituloVotacion)});
        String[] preguntasFaltantes = null;
        if(c0.moveToNext()){
            int idVotacion = c0.getInt(c0.getColumnIndex("idVotacion"));
            String args[] = {boleta};
            Cursor c = getReadableDatabase().rawQuery("select Pregunta from (Pregunta join (select * from Participante_Pregunta where Boleta = ? and (Hora_Participacion is null or Hora_Participacion = '---')) t using(idPregunta))",args);
            preguntasFaltantes = new String[c.getCount()];
            int i = 0;
            Log.d("Lala la lala la", "We encountered " + c.getCount() + " preguntas for " + boleta);
            while(c.moveToNext()) {
                preguntasFaltantes[i++] = c.getString(c.getColumnIndex("Pregunta"));
                Log.d("DIGITAMER","--->" + c.getString(c.getColumnIndex("Pregunta")));
            }
            c.close();
        }
        c0.close();
        close();
        return preguntasFaltantes;
    }

    public String consultaParticipanteHoraParticipacion(String boleta){
        String[] args = {boleta};
        Cursor c = getReadableDatabase().rawQuery("select Hora_Participacion from Participante_Pregunta where Boleta = ?",args);
        String result = null;
        if(c.moveToFirst())
            result = c.getString(c.getColumnIndex("Hora_Participacion"));
        close();
        return result;
    }

    public String[] quienesHanParticipado(String titulo){
        Cursor c0 = getReadableDatabase().rawQuery("select idVotacion from Votacion where Titulo = ?",new String[]{titulo});
        String[] participantes = null;
        if( c0.moveToFirst()) {
            int idVotacion = c0.getInt(c0.getColumnIndex("idVotacion"));
            Cursor c = getReadableDatabase().rawQuery("select Boleta, Hora_Registro, Hora_Participacion from (select * from Preguntas_Votacion where idVotacion = CAST(? as INTEGER)) a join (select Boleta,Hora_Registro,Hora_Participacion,count(*) cuenta from Participante_Pregunta join (select idPregunta from Pregunta where idVotacion = CAST(? as INTEGER)) r on Participante_Pregunta.idPregunta = r.idPregunta where Hora_Participacion is not null or Hora_Participacion != '---' group by Boleta) s on a.Preguntas = s.cuenta", new String[]{String.valueOf(idVotacion),String.valueOf(idVotacion)});
            participantes = new String[c.getCount()];
            int count = 0;
            while (c.moveToNext()) {
                participantes[count++] = c.getString(c.getColumnIndex("Boleta"))+", HR: "+c.getString(c.getColumnIndex("Hora_Registro"))+", HP: "+c.getString(c.getColumnIndex("Hora_Participacion"));
            }
            c.close();
        }
        c0.close();
        close();
        return participantes;
    }

    //public boolean agregaParticipante()

    public long insertaPerfil(String perfil){
        String[] args = {perfil};
        Cursor c = getReadableDatabase().rawQuery("select idPerfil from Perfil where perfil = ?",args);
        Integer idPerfil = c.moveToFirst() ? c.getInt(c.getColumnIndex("idPerfil")) : null;
        ContentValues values = new ContentValues();
        values.put("perfil",perfil);
        if( idPerfil != null )
            values.put("idPerfil", idPerfil);
        long result = getWritableDatabase().insert("Perfil","---",values);
        close();
        return result;
    }

    public long insertaSubPerfil(String perfil, String subPerfil){
        String[] args = {perfil};
        Cursor c = getReadableDatabase().rawQuery("select idPerfil from Perfil where perfil=?", args);
        long result = -1;
        if(c.moveToFirst()){
            ContentValues values = new ContentValues();
            values.put("idPerfil",c.getInt(c.getColumnIndex("idPerfil")));
            values.put("SubPerfil",subPerfil);
            result = getWritableDatabase().insert("SubPerfil","---",values);
        }
        close();
        return result;
    }

    public long insertaEscuela(String nombre, Double latitud, Double longitud){
        ContentValues values = new ContentValues();
        values.put("Nombre",nombre);
        values.put("latitud", latitud != null ? latitud.doubleValue() : null);
        values.put("longitud", latitud != null ? latitud.doubleValue() : null);
        long id = getReadableDatabase().insert("Escuela", "", values);
        close();
        return id;
    }

    public int insertaParticipante(String boleta, String perfil, String escuela){
        int id = -1;
        String[] args = {perfil};
        Cursor c = getReadableDatabase().rawQuery("select idPerfil from Perfil where " +
                "perfil = ?",args);
        if(c.moveToFirst()){
            args[0] = escuela;
            Cursor c2 = getReadableDatabase().rawQuery("select idEscuela from Escuela where " +
                    "Nombre = ?",args);
            if(c2.moveToFirst()){
                ContentValues values = new ContentValues();
                values.put("Boleta",boleta);
                values.put("idPerfil",c.getInt(c.getColumnIndex("idPerfil")));
                values.put("idEscuela",c2.getInt(c2.getColumnIndex("idEscuela")));
                values.put("Fecha_Registro", new SimpleDateFormat("dd/MM/yyyy hh:mm:ssss").format(new Date()));
                values.put("BoletaHash", new MD5Hash().makeHashForSomeBytes(boleta));
                try {
                    id = getWritableDatabase().insert("Participante", "---", values) == -1 ? -1 : 1;
                }catch(SQLiteException e){
                    Log.e("InsParticipante",e.getMessage());
                }
            }
            c2.close();
        }
        c.close();
        close();
        return id;
    }

    public long insertaNombreParticipante(String boleta, String nombre, String apPaterno, String apMaterno){
        ContentValues values = new ContentValues();
        values.put("Boleta",boleta);
        values.put("Nombre",nombre);
        values.put("ApPaterno",apPaterno);
        values.put("ApMaterno", apMaterno);
        long id = getWritableDatabase().insert("NombreParticipante", "---", values);
        close();
        return id;
    }

    public String obtenerFechaInicioVotacionActual(){
        Cursor c = getReadableDatabase().rawQuery("select Fecha_Inicio from Votacion where Fecha_Fin is null or Fecha_Fin = '---'",null);
        String fechaInicio = null;
        int length = c.getCount();
        if(c.moveToNext())
            fechaInicio = c.getString(c.getColumnIndex("Fecha_Inicio"));
        c.close();
        close();
        return fechaInicio;
    }

    public String obtenerTituloVotacionActual(){
        Cursor c = getReadableDatabase().rawQuery("select Titulo from Votacion where Fecha_Fin is null or Fecha_Fin = '---'", null);
        String titulo = null;
        if(c.moveToNext())
            titulo = c.getString(c.getColumnIndex("Titulo"));
        c.close();
        close();
        return titulo;
    }

    public void conservaFechaFinVotacionActual(String tituloVotacion, String fechaFin){
        Cursor c = getReadableDatabase().rawQuery("select idVotacion from Votacion where Titulo = ?",new String[]{tituloVotacion});
        if(c.moveToFirst()) {
            int idVotacion = c.getInt(c.getColumnIndex("idVotacion"));
            ContentValues values = new ContentValues();
            values.put("idVotacion", idVotacion);
            values.put("Fecha_Fin", fechaFin);
            getWritableDatabase().insert("VotacionFechaFin", "---", values);
        }
    }

    public String obtenerFechaFinVotacionActual(){
        String fechaFin = null;
        Cursor c = getReadableDatabase().rawQuery("select Fecha_Fin from VotacionFechaFin join (select idVotacion from Votacion where Fecha_Fin is null or Fecha_Fin = '---') r on r.idVotacion = VotacionFechaFin.idVotacion", null);
        if(c.moveToNext()){
            fechaFin = c.getString(0);
        }
        c.close();
        close();
        return fechaFin;
    }

    public boolean terminaUltimaVotacion(){
        ContentValues values = new ContentValues();
        values.put("Fecha_Fin", new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(new Date()));
        int tul = getWritableDatabase().update("Votacion", values, "Fecha_Fin is null or Fecha_Fin = '---'", null);
        boolean success = false;
        if( tul != 0 )
            success = true;
        close();
        return success;
    }

    public void actualizaFechaInicioVotacionActual(String fechaInicio){
        ContentValues values = new ContentValues();
        values.put("Fecha_Inicio",fechaInicio);
        getWritableDatabase().update("Votacion", values, "Fecha_Fin is null or Fecha_Fin = '---'", null);
        close();
    }

    public long insertaVotacion(String tituloVotacion,String fechaInicio, String fechaFin){
        ContentValues values = new ContentValues();
        values.put("Fecha_Inicio",fechaInicio);
        values.put("Fecha_Fin", fechaFin);
        values.put("Titulo", tituloVotacion);
        long id = getWritableDatabase().insert("Votacion", "---", values);
        close();
        return id;
    }

    public long insertaPregunta(String pregunta, String votacion){
        long id = -1;
        String[] args = {votacion};
        Cursor c = getReadableDatabase().rawQuery("select idVotacion from Votacion where Titulo = ?", args);
        if(c.moveToNext()){
            ContentValues values = new ContentValues();
            values.put("Pregunta",pregunta);
            values.put("idVotacion",c.getInt(c.getColumnIndex("idVotacion")));
            id = getWritableDatabase().insert("Pregunta","---",values);
        }
        close();
        return id;
    }

    public long insertaOpcion(String reactivo){
        ContentValues values = new ContentValues();
        values.put("Reactivo", reactivo);
        long id = getWritableDatabase().insert("Opcion", "---", values);
        close();
        return id;
    }

    public long insertaPreguntaOpcion(String pregunta, String opcion){
        long id = -1;
        String[] args = {pregunta};
        Cursor c = getReadableDatabase().rawQuery("select idPregunta from Pregunta where Pregunta = ?", args);
        if(c.moveToFirst()){
            args[0] = opcion;
            Cursor c2 = getReadableDatabase().rawQuery("select idOpcion from Opcion where reactivo = ?",args);
            if(c2.moveToFirst()){
                ContentValues values = new ContentValues();
                values.put("idPregunta",c.getInt(c.getColumnIndex("idPregunta")));
                values.put("idOpcion",c2.getInt(c2.getColumnIndex("idOpcion")));
                id = getWritableDatabase().insert("Pregunta_Opcion","---",values);
            }
        }
        close();
        return id;
    }

    public long insertaUsuario(String usrName, byte[] psswd){
		ContentValues values = new ContentValues();
        values.put("Name", usrName);
        values.put("Psswd", psswd);
        long result = getWritableDatabase().insert("Usuario", "---", values);
        close();
        return result;
    }

    public int insertaLoginAttempt(String usr, String host){
        String[] columns = {"idUsuario"};
        String selection = "Name = ?";
        String selArgs[] = {usr};
        Cursor c = getReadableDatabase().query("Usuario", columns, selection,selArgs,null,null,null);
        int loginAttempt = -1;
        if( c.moveToFirst() ){
            ContentValues values = new ContentValues();
            values.put("idUsuario", c.getInt(c.getColumnIndex("idUsuario")));
            values.put("Host",host);
            values.put("Attempt_Timestamp", new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(new Date()));
            getWritableDatabase().insert("LoginAttempt", "---", values);
            Cursor c2 = getReadableDatabase().rawQuery("SELECT last_insert_rowid()",null);
            c2.moveToFirst();
            loginAttempt = c2.getInt(0);
            c2.close();
        }
        c.close();
        close();
        return loginAttempt;
    }

    public long insertaAttemptSucceded(int idLoginAttempt, byte[] secretKey){
        ContentValues values = new ContentValues();
        values.put("idLoginAttempt", idLoginAttempt);
        values.put("secretKey", secretKey);
        long result = getWritableDatabase().insert("AttemptSucceded", "---", values);
        close();
        return result;
    }

    public void insertaUserAction(int idLoginAttempt, String action){
        ContentValues values = new ContentValues();
        values.put("idLoginAttempt",idLoginAttempt);
        values.put("Action", action);
        values.put("Action_Timestamp",new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(new Date()));
        getWritableDatabase().insert("UserAction", "---", values);
        close();
    }

    public long insertaParticipantePregunta(String boleta, String pregunta){
        long id = -1;
        String[] args = {pregunta};
        Cursor c = getReadableDatabase().rawQuery("select idPregunta from Pregunta where Pregunta = ?", args);
        Log.d("Marakeru","We've been asked to insert: " + pregunta + " for " + boleta);
        if(c.moveToFirst()){
            ContentValues values = new ContentValues();
            values.put("Boleta",boleta);
            values.put("idPregunta", c.getInt(c.getColumnIndex("idPregunta")));
            values.put("Hora_Registro", new SimpleDateFormat("dd/MM/yyyy hh:mm:ssss").format(new Date()));
            id = getWritableDatabase().insert("Participante_Pregunta","---",values);
            if(id != -1)
                Log.d("Inserta Participante","Participante insertado exitosamente.");
        }else
            Log.d("Marakeru","We were not able to register " + pregunta + " for " + boleta);
        c.close();
        close();
        return id;
    }

    public boolean actualizaParticipantePregunta(String boleta, String pregunta){
        boolean success = false;
        Cursor c = getReadableDatabase().rawQuery("select idPregunta from Pregunta where Pregunta = ?", new String[]{pregunta});
        if(c.moveToFirst()) {
            int idPregunta = c.getInt(c.getColumnIndex("idPregunta"));
            ContentValues values = new ContentValues();
            values.put("Hora_Participacion", new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(new Date()));
            if(getWritableDatabase().update("Participante_Pregunta",values,"Boleta = ? and idPregunta = CAST(? as INTEGER)",new String[]{boleta,String.valueOf(idPregunta)}) != -1)
                success = true;
        }
        return success;
    }

    public int obtenerIdVotacionFromPregunta(String pregunta){
        String args[] = {pregunta};
        Cursor c = getReadableDatabase().rawQuery("select idVotacion from Pregunta where Pregunta = ?",args);
        int idVotacion = c.moveToFirst() ? c.getInt(c.getColumnIndex("idVotacion")) : -1;
        c.close();
        close();
        return idVotacion;
    }

    public byte[] obtenerSKeyEncoded(int idAttempt){
        byte[] encodedSKey = null;
        Cursor c = getReadableDatabase().rawQuery("SELECT secretKey from AttemptSucceded where idLoginAttempt = CAST(? as INTEGER)",new String[]{String.valueOf(idAttempt)});
        if(c.moveToFirst()){
            encodedSKey = c.getBlob(c.getColumnIndex("secretKey"));
            c.close();
        }
        close();
        return encodedSKey;
    }

    public void dummySelect(){
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM AttemptSucceded",null);
        Cursor c2 = getReadableDatabase().rawQuery("SELECT secretKey FROM AttemptSucceded WHERE idLoginAttempt = CAST(? as INTEGER)",new String[]{String.valueOf(9)});
        if(c2.moveToNext())
            Log.e("Riot Mode",Arrays.toString(c2.getBlob(c2.getColumnIndex("secretKey"))));
        else {
            Log.e("Termination", "Sucks");
            c2 = getReadableDatabase().query("AttemptSucceded", null, "idLoginAttempt=?",
                    new String[]{"13"}, null, null, null);
            if(c2.moveToNext())
                Log.i("Intrinsic",Arrays.toString(c2.getBlob(c2.getColumnIndex("secretKey"))));
            else
                Log.i("Intrinsic","It keeps sucking");
        }
        c2.close();
        while(c.moveToNext())
            Log.d("STRIKER",c.getInt(c.getColumnIndex("idLoginAttempt"))+Arrays.toString(c.getBlob(c.getColumnIndex("secretKey"))));
        close();
    }

    public long insertaVoto(byte[] idVoto, int idVotacion, String perfil, String voto, int idLoginAttempt, int idPregunta){
        long id = -1;
        String args[] = {perfil};
        Cursor c2 = getReadableDatabase().rawQuery("select idPerfil from Perfil where perfil = ?",args);
        if(c2.moveToFirst()){
            ContentValues values = new ContentValues();
            values.put("idVoto",idVoto);
            values.put("idVotacion",idVotacion);
            values.put("idPerfil",c2.getInt(c2.getColumnIndex("idPerfil")));
            values.put("Voto",voto);
            values.put("idLoginAttempt",idLoginAttempt);
            values.put("idPregunta", idPregunta);
            id = getWritableDatabase().insert("Voto", "---", values);
        }
        c2.close();
        close();
        return id;
    }

	public boolean consultaUsuario(String usrName, byte[] psswd){
		boolean result = false;
		String[] args = {usrName};
        Cursor c = getReadableDatabase().rawQuery("select Psswd from Usuario where Name = ?", args);
        if (c.moveToFirst()) {
            Log.d("From consulta usuario", "Comparing the arrays...");
            result = Arrays.equals(c.getBlob(c.getColumnIndex("Psswd")), psswd);
        }
		close();
		return result;
	}

    public boolean consultaEscuela(){
        boolean result = getReadableDatabase().rawQuery("select * from Escuela", null).getCount()
                > 0;
        close();
        return result;
    }

    public boolean consultaPerfiles(){
        boolean result = getReadableDatabase().rawQuery("select * from Perfil", null).getCount()
                > 0;
        close();
        return result;
    }

    public boolean actualizaUsuarioPsswd(String usrName, byte[] psswd){
        ContentValues values = new ContentValues();
        values.put("Psswd", psswd);
        String[] selArgs = {usrName};
        boolean result = getWritableDatabase().update("Usuario", values, "Name=?", selArgs) > 0;
        close();
        return result;
    }

    public String obtenerUltimaEscuela(){
        Cursor c = getReadableDatabase().rawQuery("select Nombre from Escuela",null);
        c.moveToLast();
        String result = c.getString(c.getColumnIndex("Nombre"));
        c.close();
        close();
        return result;
    }

    public void borraPerfil(String perfil){
        String[] args = {perfil};
        getWritableDatabase().delete("Perfil", "perfil = ?", args);
        close();
    }

    public String[] obtenerPerfiles(){
        Cursor c = getReadableDatabase().rawQuery("select perfil from Perfil",null);
        String[] perfiles = new String[c.getCount()];
        int counter = 0;
        while(c.moveToNext())
            perfiles[counter++] = c.getString(c.getColumnIndex("perfil"));
        c.close();
        return perfiles;
    }

    public String obtenerUsuarioPorIdAttempt(int idAttempt){
        String uName = null;
        Cursor c = getReadableDatabase().rawQuery("Select Name from (Usuario join LoginAttempt on " +
                "Usuario.idUsuario = LoginAttempt.idUsuario) t where  t.idLoginAttempt = ?",new String[]{String.valueOf(idAttempt)});
        if(c.moveToFirst()){
            uName = c.getString(c.getColumnIndex("Name"));
        }
        c.close();
        close();
        return uName;
    }

    public boolean consultaExistenciaBoleta(String boleta, String votacion){
        boolean existe = false;
        Cursor c = getReadableDatabase().rawQuery("select * from Participante where Boleta = ?", new String[]{boleta});
        if(c.getCount() > 0){
            Cursor c2 = getReadableDatabase().rawQuery("select idVotacion from Votacion where Titulo = ?",new String[]{votacion});
            if(c2.moveToFirst()){
                Cursor c3 = getReadableDatabase().rawQuery("select * from Participante_Pregunta join (select * from Pregunta where Pregunta.idVotacion = CAST(? as INTEGER)) r on r.idPregunta = Participante_Pregunta.idPregunta where Boleta = ?", new String[]{String.valueOf(c2.getInt(c2.getColumnIndex("idVotacion"))),boleta});
                existe = c3.getCount() > 0;
                c3.close();
            }
            c2.close();
        }
        c.close();
        close();
        return existe;
    }

    public boolean consultaRemotaExistenciaBoleta(String boleta, String votacion){
        boolean existe = false;
        Cursor c = getReadableDatabase().rawQuery("select * from Participante where BoletaHash = ?", new String[]{boleta});
        if(c.getCount() > 0){
            Cursor c2 = getReadableDatabase().rawQuery("select idVotacion from Votacion where Titulo = ?",new String[]{votacion});
            if(c2.moveToFirst()){
                Cursor c3 = getReadableDatabase().rawQuery("select * from Participante_Pregunta join (select * from Pregunta where Pregunta.idVotacion = CAST(? as INTEGER)) r on r.idPregunta = Participante_Pregunta.idPregunta where Boleta = ?", new String[]{String.valueOf(c2.getInt(c2.getColumnIndex("idVotacion"))),c.getString(c.getColumnIndex("Boleta"))});
                existe = c3.getCount() > 0;
                c3.close();
            }
            c2.close();
        }
        c.close();
        close();
        return existe;
    }

    public int obtenerIdPregunta(String pregunta){
        int idPregunta = -1;
        Cursor c = getReadableDatabase().rawQuery("select idPregunta from Pregunta where Pregunta = ?", new String[]{pregunta});
        if(c.moveToFirst()){
            idPregunta = c.getInt(c.getColumnIndex("idPregunta"));
        }
        c.close();
        close();
        return idPregunta;
    }

    public LinkedList<String> obtenerResultadosPorPregunta(String pregunta, int idVotacion){
        LinkedList<String> resultados = new LinkedList<>();
        Log.d("Mayunia","Quering over " + pregunta);
        //Cursor c = getReadableDatabase().rawQuery("select voto,c from Pregunta join (select voto,c,idPregunta from Pregunta_Opcion join (select voto,idOpcion,count(*) c from Opcion join Voto on Opcion.Reactivo = Voto.voto where idVotacion = CAST(? as INTEGER) group by voto) r using(idOpcion)) s using(idPregunta) where Pregunta = ?", new String[]{pregunta, String.valueOf(idVotacion)});
        Cursor c = getReadableDatabase().rawQuery(
                "select Pregunta,Reactivo, count(*) c from (select idVotacion,Pregunta,Reactivo from (select idVotacion,idOpcion, Pregunta from (select * from Pregunta where Pregunta = ?) r join Pregunta_Opcion using(idPregunta)) s join Opcion using(idOpcion)) t join Voto on voto = Reactivo group by Reactivo"
                ,new String[]{pregunta});
        while(c.moveToNext()){
            Log.d("Mayunia",c.getString(c.getColumnIndex("Pregunta"))+","+c.getString(c.getColumnIndex("Reactivo"))+","+c.getInt(c.getColumnIndex("c")));
            resultados.add(c.getString(c.getColumnIndex("Reactivo")) + "@" + c.getInt(c.getColumnIndex("c")));
            //Log.d("Mayunia",c.getString(c.getColumnIndex("voto"))+"@"+c.getInt(c.getColumnIndex("c")));

        }
        c.close();
        close();
        return resultados;
    }

    public String[] obtenerPreguntasVotacion(String titulo){
        String[] preguntas;
        Cursor c = getReadableDatabase().rawQuery("select Pregunta from Pregunta join (select idVotacion from Votacion where Titulo = ?) r on Pregunta.idVotacion = r.idVotacion", new String[]{titulo});
        preguntas = new String[c.getCount()];
        int counter = 0;
        while(c.moveToNext()){
            preguntas[counter++] = c.getString(c.getColumnIndex("Pregunta"));
        }
        c.close();
        close();
        return preguntas;
    }

    public JSONArray obtenerOpcionesPregunta(String pregunta){
        Cursor c = getReadableDatabase().rawQuery("SELECT Reactivo from Opcion join (select idOpcion from (select idPregunta from " +
                "Pregunta where Pregunta = ?) r join Pregunta_Opcion on r.idPregunta = Pregunta_Opcion.idPregunta) s " +
                "on Opcion.idOpcion = s.idOpcion", new String[]{pregunta});
        JSONArray opciones = new JSONArray();
        while(c.moveToNext()){
            opciones.put(c.getString(c.getColumnIndex("Reactivo")));
        }
        return opciones;
    }

    public byte[] obtenerKeyByHost(String host){
        Cursor c = getReadableDatabase().rawQuery("select secretKey from (select idLoginAttempt,Usuario.idUsuario,secretkey,Host,Name from ( LoginAttempt join (select idLoginAttempt, secretKey from AttemptSucceded) r on r.idLoginAttempt = LoginAttempt.idLoginAttempt) s join Usuario on s.idUsuario = Usuario.idUsuario) t where Host like ? and Name = 'Participante'", new String[]{"%" + host + "%"});
        byte[] key = null;
        if(c.moveToLast()){
            key = c.getBlob(c.getColumnIndex("secretKey"));
        }
        c.close();
        close();
        return key;
    }

    public boolean isCurrentVotingProcessGlobal(){
        Cursor c = getReadableDatabase().rawQuery("select isGlobal from Votacion where Fecha_Fin is null or Fecha_Fin = '---'",null);
        boolean result = false;
        if(c.moveToFirst()) {
            result = c.getInt(c.getColumnIndex("isGlobal")) == 1;
            Log.d("Dragin Tammer", "Situacion actual: " + (result ? "Global" : "No global"));
        }else{
            Log.d("Dragin Tammer", "?????????????????????");
        }
        c.close();
        close();
        return result;
    }

	public int consultaPAAE(){
		SQLiteDatabase db = getReadableDatabase();
		String table = "Participantin";
		String columns[] = {"Boleta"};
		String selection = "Perfil=?";
		String[] selectionArgs = {"PAAE"};
		String groupBy = null;
		String having = null;
		String orderBy = null;
		Cursor c = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
		int count = c.getCount();
		db.close();
		return count;
	}
	
	public int consultaDOCENTE(){
		SQLiteDatabase db = getReadableDatabase();
		String table = "Participantin";
		String columns[] = {"Boleta"};
		String selection = "Perfil=?";
		String[] selectionArgs = {"DOCENTE"};
		String groupBy = null;
		String having = null;
		String orderBy = null;
		Cursor c = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
		int count = c.getCount();
		db.close();
		return count;
	}
	
	public int consultaAlumno(){
		SQLiteDatabase db = getReadableDatabase();
		String table = "Participantin";
		String columns[] = {"Boleta"};
		String selection = "Perfil!=? and Perfil!=?";
		String[] selectionArgs = {"PAAE","DOCENTE"};
		String groupBy = null;
		String having = null;
		String orderBy = null;
		Cursor c = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
		int count = c.getCount();
		db.close();
		return count;
	}

	public long insertaRegistroUnico(String nombre, String boleta, String perfil, LinkedList<Pregunta> preguntas){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("Boleta", boleta);
		values.put("Nombre", nombre);
		values.put("Perfil", perfil);
		values.put("Hora_Registro",new SimpleDateFormat("hh:mm:ss").format(new Date()));
		long id = db.insert("Participantin", "---", values);
		for(Pregunta pregunta : preguntas){
			values = new ContentValues();
			values.put("Boleta", boleta);
			values.put("Pregunta", pregunta.titulo);
			if( id != -1 )
				if( "PAAE".equals(perfil) )
					db.insert("VotandoPAAE", "---", values);
				else if( "DOCENTE".equals(perfil) )
					db.insert("VotandoDOCENTE","---",values);
				else if("Alumno".equals(perfil))
					db.insert("VotandoAlumno","---",values);
				else
					db.insert("VotandoLibre", "---", values);
		}
		db.close();
		return id;
	}
	
	public boolean checkMatricula(){
		boolean haveMatricula = false;
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query("Participantin", null, null, null, null, null, null);
		if( c.getCount() > 0 )
			haveMatricula = true;
		db.close();
		return haveMatricula;
	}
	
	/*********
	 * 
	 * 
	 * KEY HANDLER
	 * 
	 * 
	 * *****************/

	public boolean revisaExistenciaDeCredencial(String nombre){
		String[] args = {nombre};
		boolean success = getReadableDatabase().rawQuery("select idUsuario from Usuario where Name = ?",args).getCount() > 0 ? true : false;
		close();
		return success;
	}
	
	public boolean consultKey(String id, String nombre){ // Debe recibir el id "hasheado"
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = null;
		String selectionArgs[] = {id,nombre};
		Cursor c = db.query("Key", columns, "VALUE=? and NOMBRE=?", selectionArgs, null, null, null);
		c.moveToNext();
		boolean success = false;
		if(c.getCount() > 0)
			success = true;
		db.close();
		return success;
	}

	public boolean insertKey(String key, String user, String zona){ // Recibe el valor key "hasheado"
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("VALUE", key);
		values.put("NOMBRE", user);
		values.put("Zona", new Hasher().makeHash(zona));
		long result = db.insert("Key", "---", values);
		db.close();
        return result != -1;
	}
	
	public boolean updateKey(String oldKey, String newKey, String user, String zone){// Valores key deven venir "hasheados"
		SQLiteDatabase db = getWritableDatabase();
		String[] whereArgs = {user, oldKey, zone};
		ContentValues values = new ContentValues();
		values.put("VALUE", newKey);
		int rowsAffected = db.update("Key", values, "NOMBRE=? and VALUE=? and Zona=?", whereArgs);
		db.close();
        return rowsAffected != 0;
	}
	
	/*********
	 * 
	 * 
	 * OPCION
	 * 
	 * 
	 * *****************/

	public boolean altaOpcion(String texto){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		Hasher hasher = new Hasher();
		values.put("ID", hasher.makeHash(texto));
		values.put("Texto", texto);
		long result = db.insert("Opcion", "---", values);
		db.close();
        return result != -1;
	}

	public boolean quitarOpcion(String texto){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String[] whereArgs = {new Hasher().makeHashString(texto)};
		int count = db.delete("Opcion", whereClause, whereArgs);
		db.close();
        return count > 0;
	}

	public boolean cambiaOpcion(String idOpcion, String nTexto){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String whereArgs[] = {idOpcion};
		ContentValues values = new ContentValues();
		values.put("ID",new Hasher().makeHash(nTexto));
		values.put("Texto", nTexto);
        return db.update("Opcion", values, whereClause, whereArgs) > 0;
	}

	public String[] obtenerOpcion(){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"ID","Texto"};
		String selectionArgs[] = null;
		String whereClause = null;
		Cursor c = db.query("Opcion", columns, whereClause, selectionArgs, null, null, null);
		List<String> direcciones = new ArrayList<String>();
		while(c.moveToNext())
			direcciones.add(c.getString(c.getColumnIndex("ID")) + "," + c.getString(c.getColumnIndex("Texto")));
		db.close();
		String[] resultado = new String[1];
		return direcciones.toArray(resultado);
	}
	
	public String obtenerOpcionID(String texto){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"ID"};
		String selectionArgs[] = null;
		String whereClause = null;
		Cursor c = db.query("Opcion", columns, whereClause, selectionArgs, null, null, null);
		String id = null;
		while(c.moveToNext())
			id = c.getString(c.getColumnIndex("ID"));
		db.close();
		return id;
	}
	
	/*********
	 * 
	 * 
	 * OTHER STUFF
	 * 
	 * 
	 * *****************/

	public void insertaBoleta(String boleta){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("Boleta", boleta);
		values.put("Hora_Registro",new SimpleDateFormat("hh:mm:ss").format(new Date()));
		db.insert("Participantin", "---", values);
		db.close();
	}

	public long insertaRegistro(String escuela, String[] preguntas){
		SQLiteDatabase db = getWritableDatabase();
		long id = -1;
        Cursor c = getReadableDatabase().rawQuery("select idEscuela from Escuela where Nombre = ?",new String[]{escuela});
        if(c.moveToFirst())
		try{
            int count = 1;
			BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(new File(FILE_NAME))));
			String row;
			String elements[];
			ContentValues values;
			while((row = bf.readLine()) != null){
				elements = row.split(",");
				try{
                    values = new ContentValues();
                    values.put("Boleta", elements[0]);
                    values.put("idEscuela",c.getInt(c.getColumnIndex("idEscuela")));
                    Cursor x = getReadableDatabase().rawQuery("select idPerfil from Perfil where perfil = ?", new String[]{elements[4]});
                    if(!x.moveToFirst()) {
                        ContentValues vl = new ContentValues();
                        vl.put("perfil",elements[4]);
                        getWritableDatabase().insert("Perfil", "---", vl);
                    }
                    x = getReadableDatabase().rawQuery("select idPerfil from Perfil where perfil = ?", new String[]{elements[4]});
                    x.moveToNext();
                    values.put("idPerfil", x.getInt(x.getColumnIndex("idPerfil")));
                    values.put("Fecha_Registro",new SimpleDateFormat("hh:mm:ss").format(new Date()));
                    db.insert("Participante","---",values);

					values = new ContentValues();
                    values.put("Boleta", elements[0]);
					values.put("Nombre",elements[3]);
                    values.put("ApPaterno",elements[1]);
                    values.put("ApMaterno",elements[2]);
					id = db.insert("NombreParticipante", "---", values);
					for(String pregunta : preguntas){
						values = new ContentValues();
						values.put("Boleta", elements[0]);
						values.put("Pregunta", pregunta);
                        db.insert("Participante_Pregunta","---",values);
					}
                    Log.d("EL MAN","" + count++);
				}catch(SQLException e){
					if(e.toString().contains("primary")){
						Log.d("CAPIZ:","There was a bad input field: " + elements[0]);
					}
					e.printStackTrace();
				}
			}
			bf.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		db.close();
		return id;
	}

	public long insertaBoleta(String boleta, List<String> preguntas){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("Boleta", boleta);
		values.put("Hora_Registro",new SimpleDateFormat("hh:mm:ss").format(new Date()));
		long id = db.insert("Participantin", "---", values);
		for(String pregunta : preguntas){
			values = new ContentValues();
			values.put("Boleta", boleta);
			values.put("Pregunta", pregunta);
			if( id != -1 )
				db.insert("Votando", "---", values);
		}
		db.close();
		return id;
	}
	
	public void selectPendingVotes(){
		SQLiteDatabase db = getReadableDatabase();
		String[] selectionArgs2 = {"1"};
		Cursor c2 = db.rawQuery("SELECT Boleta from (SELECT count(*) as cuenta,Boleta from (select Boleta from VotandoLibre where Respondida = ?) v group by Boleta) where cuenta < 2 and cuenta > 0", selectionArgs2);
		while(c2.moveToNext()){
			Log.d("PONCHO2",c2.getString(c2.getColumnIndex("Boleta")));
		}
		
		db.close();
	}
	
	public String actualizaVotando(String boleta, String pregunta, String voto){
		int flag = 0; // means a normal update.
		SQLiteDatabase db = getWritableDatabase();
		SQLiteDatabase dbReadable = getReadableDatabase();
		String[] columnas = {"Pregunta"};
		String selectionArgs[] = {boleta,"0"};
		String[] whereArgs = {boleta,pregunta,"0"};
		String[] columns = {"Perfil"};
		String[] columnaNombre = {"Nombre"};
		String[] argumentos = {boleta};
		String nombre;
		Cursor c = null;
		Cursor cursor = dbReadable.query("Participantin", columns, "Boleta=?", argumentos, null, null, null, null);
		cursor.moveToFirst();
		String perfil = cursor.getString(cursor.getColumnIndex("Perfil"));
		Cursor cursor2 = dbReadable.query("Participantin", columnaNombre, "Boleta=?", argumentos, null, null, null, null);
		cursor2.moveToFirst();
		nombre = cursor2.getString(cursor2.getColumnIndex("Nombre"));
		int rowsUpdated;
		ContentValues values;
		values = new ContentValues();
		values.put("Respondida", 1);
		if (perfil.equals("PAAE")){
			rowsUpdated = db.update("VotandoPAAE", values, "Boleta=? and Pregunta=? and Respondida=?", whereArgs);
			c = dbReadable.query("VotandoPAAE", columnas, "Boleta=? and Respondida=?", selectionArgs, null,null,null);
		}else if( perfil.equals("DOCENTE")){
			rowsUpdated = db.update("VotandoDOCENTE", values, "Boleta=? and Pregunta=? and Respondida=?", whereArgs);
			c = dbReadable.query("VotandoDOCENTE", columnas, "Boleta=? and Respondida=?", selectionArgs, null,null,null);
		}else if("Alumno".equals(perfil)){
			rowsUpdated = db.update("VotandoAlumno", values, "Boleta=? and Pregunta=? and Respondida=?", whereArgs);
			c = dbReadable.query("VotandoAlumno", columnas, "Boleta=? and Respondida=?", selectionArgs, null,null,null);
		}else{
			rowsUpdated = db.update("VotandoLibre", values, "Boleta=? and Pregunta=? and Respondida=?", whereArgs);
			c = dbReadable.query("VotandoLibre", columnas, "Boleta=? and Respondida=?", selectionArgs, null,null,null);
		}
		if(c.getCount() == 0){
			values = new ContentValues();
			String[] otherWhereArgs = {boleta};
			values.put("Hora_Voto", new SimpleDateFormat("hh:mm:ss").format(new Date()));
			db.update("Participantin", values, "Boleta=?", otherWhereArgs);//("Participante", "---", values);
			flag = 1; // Indica que ha terminado correctamente.
		}
		values = new ContentValues();
		values.put("Voto", voto);
		values.put("Pregunta", pregunta);
		if( rowsUpdated > 0 )
			if( perfil.equals("PAAE") )
				db.insert("VotoPAAE", "---", values);
			else if( perfil.equals("DOCENTE") )
				db.insert("VotoDOCENTE", "---", values);
			else if( "Alumno".equals(perfil) )
				db.insert("VotoAlumno", "---", values);
			else
				db.insert("VotoLibre","---",values);
		dbReadable.close();
		db.close();
		if( flag == 1 )
			return nombre;
		else
			return null;
	}
    // table Participante_Pregunta(Boleta TEXT not null, idPregunta INTEGER NOT NULL, Hora_Registro text not null, Hora_Participacion text
    public String[] consultaTusVotos(String titVotacion){
        String[] args = {titVotacion};
        String sql = "Select voto, count(*) as votox from (Voto join (select Reactivo from ( Opcion join (select idOpcion from (Pregunta_Opcion join (select idPregunta from (Pregunta join (select idVotacion " +
                "from Votacion where Titulo = ?) t on Pregunta.idVotacion = t.idVotacion)) r on " +
                "Pregunta_Opcion.idPregunta = r.idPregunta)) z on Opcion.idOpcion = z.idOpcion group by Reactivo)) w on Voto.voto = w.Reactivo group by voto";
        Cursor c = getReadableDatabase().rawQuery(sql,args);
        String[] votos = new String[c.getCount()];
        int i = 0;
        while(c.moveToNext()){
            votos[i] = c.getString(c.getColumnIndex("voto")) + "@" + c.getInt(c.getColumnIndex("votox"));
        }
        close();
        c.close();
        return votos;
    }
	
	public int consultaVotos(String opcion, String pregunta, int urna){
		SQLiteDatabase db = getReadableDatabase();
		String table = new String();
		switch(urna){
		case 0:
			table = "VotoPAAE";
			break;
		case 1:
			table = "VotoDOCENTE";
			break;
		case 2:
			table = "VotoAlumno";
			break;
		default:
				table = "VotoLibre";
		}
		String columns[] = {"Voto"};
		String selection = "Voto=? and Pregunta=?";
		String[] selectionArgs = {opcion,pregunta};
		String groupBy = null;
		String having = null;
		String orderBy = null;
		Cursor c = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
		int count = c.getCount();
		db.close();
		return count;
	}
	
	public List<String> consultaPapeletasFaltantes(String boleta){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"Pregunta"};
		String selection = "Boleta=? and Respondida=?";
		String[] selectionArgs = {boleta,"0"};
		String groupBy = null;
		String having = null;
		String orderBy = null;
		String[] algoColumnas = {"Perfil"};
		String[] tacos = {boleta};
		Cursor cursor = db.query("Participantin", algoColumnas, "Boleta=?", tacos, null,null,null);
		cursor.moveToFirst();
		String perfil = cursor.getString(cursor.getColumnIndex("Perfil"));
		Cursor c;
		if( "PAAE".equals(perfil) )
			c = db.query("VotandoPAAE", columns, selection, selectionArgs, groupBy, having, orderBy);
		else if( "DOCENTE".equals(perfil))
			c = db.query("VotandoDOCENTE", columns, selection, selectionArgs, groupBy, having, orderBy);
		else if("Alumno".equals(perfil))
			c = db.query("VotandoAlumno", columns, selection, selectionArgs, groupBy, having, orderBy);
		else	
			c = db.query("VotandoLibre",columns,selection,selectionArgs,groupBy,orderBy,having);
		List<String> preguntasFaltantes = new ArrayList<String>();
		while(c.moveToNext()){
			preguntasFaltantes.add(c.getString(c.getColumnIndex("Pregunta")));
		}
		db.close();
		return preguntasFaltantes;
	}
	
	public boolean consultaParticipanteHoraVoto(String boleta){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"Hora_Voto"};
		String selection = "Boleta=?";
		String[] selectionArgs = {boleta};
		Cursor c = db.query("Participantin", columns, selection, selectionArgs, null, null, null);		
		boolean finishedVoting;
		c.moveToNext();
        finishedVoting = c.getString(c.getColumnIndex("Hora_Voto")) != null;
		db.close();
		return finishedVoting;
	}
	
	public String[] consultaParticipantes(){
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query("Participantin", null, null, null, null, null, null);
		String rows[] = new String[c.getCount()];
		int index = 0;
		while(c.moveToNext())
			rows[index++] = c.getString(c.getColumnIndex("Boleta")) + ", " + c.getString(c.getColumnIndex("Hora_Registro")) + ", " + c.getString(c.getColumnIndex("Hora_Voto"));
		db.close();
		return rows;
	}
	
	public String[] consultaVotando(){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"null"};
		String selection = "Hora_Voto!=?";
		String selArgs[] = {"null"};
		Cursor c = db.query("Participantin", null, selection, selArgs, null, null, "Hora_Voto");
		String rows[] = new String[c.getCount()];
		int index = 0;
		while(c.moveToNext())
			rows[index++] = c.getString(c.getColumnIndex("Boleta")) + ", Hora Registro: " + c.getString(c.getColumnIndex("Hora_Registro")) + ", Hora Voto: " + c.getString(c.getColumnIndex("Hora_Voto"));
		db.close(); 
		return rows;
	}
	
	public String[] consultaVoto(){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"Voto"};
		Cursor c = db.query("VotoLibre", columns, null, null, null, null, null);
		String rows[] = new String[c.getCount()];
		int index = 0;
		while(c.moveToNext())
			rows[index++] = c.getString(c.getColumnIndex("Voto"));
		db.close();
		return rows;
	}
	
	public void registraVotacion(byte[] TEXT, String nombre, String fechaIni, String fechaFin){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("ID", TEXT);
		values.put("NOMBRE", nombre);
		values.put("Fecha_Inicio", fechaIni);
		values.put("Fecha_Fin", fechaFin);
		db.insert("Participante", "---", values);
		db.close();
	}
	
	public int[] terminarVotaciones(){
		SQLiteDatabase db = getWritableDatabase();
		int votosCount = db.delete("VotoLibre", "1", null);
		int limboCount = db.delete("VotandoLibre", "1", null);
		int participantesCount = db.delete("Participantin", "1", null);
		int results[] = {votosCount, participantesCount, limboCount};
		db.close();
		return results;
	}
	
	/*********
	 * 
	 * 
	 * PARTICIPANTE
	 * 
	 * 
	 * *****************/

	public long altaParticipante(String boleta, String nombre, String perfil){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("Boleta", boleta);
		values.put("Nombre", nombre);
		values.put("Perfil",perfil);
		values.put("Fecha_Registro", new SimpleDateFormat("dd/mm/yyyy hh:MM:ss").format(new Date()));
		values.put("Fecha_Ultima_Modificacion", new SimpleDateFormat("dd/mm/yyyy hh:MM:ss").format(new Date()));
		long result = -1;
		try{
			result = db.insertOrThrow("Participante","---",values);
		}catch(Exception e){
			Log.d("Cuchele","Se ha repetido la boleta: " + boleta + ". "+e.toString());
		}
		db.close();
		return result;
	}

	public boolean quitarParticipante(String boleta){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Boleta=?";
		String[] whereArgs = {boleta};
		int count = db.delete("Participante",whereClause,whereArgs);
		db.close();
        return count > 0;
	}

	public boolean cambiaParticipante(String boleta, String nuevaBoleta, String nuevoNombre, String nuevoPerfil){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Boleta=?";
		String whereArgs[] = {boleta};
		ContentValues values = new ContentValues();
		values.put("Boleta", nuevaBoleta);
		values.put("Nombre", nuevoNombre);
		values.put("Perfil",nuevoPerfil);
		values.put("Fecha_Ultima_Modificacion", new SimpleDateFormat("dd/mm/yyyy hh:MM:ss").format(new Date()));
        return db.update("Participante", values, whereClause, whereArgs) > 0;
	}

	public String[] obtenerParticipantes(){
        Cursor c = getReadableDatabase().rawQuery("SELECT * from Usuario",null);
        String[] participantes = new String[c.getCount()];
        int count = 0;
        while(c.moveToNext())
            participantes[count++] = c.getString(c.getColumnIndex("Name"));
		return participantes;
	}
	
	/*********
	 * 
	 * 
	 * PARTICIPANTE_PERFIL
	 * 
	 * 
	 * *****************/

	public boolean agregarParticipantePerfil(String idParticipante, String idPerfil){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("Participante", idParticipante);
		values.put("Perfil", idPerfil);
		long result = db.insert("Participante_Perfil", "---", values);
		db.close();
        return result != -1;
	}

	public boolean quitarParticipantePerfil(String idParticipante, String idPerfil){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Participante=? and Perfil=?";
		String[] whereArgs = {idParticipante, idPerfil};
		int count = db.delete("Participante_Perfil", whereClause, whereArgs);
		db.close();
        return count > 0;
	}
	
	/*********
	 * 
	 * 
	 * PARTICIPANTE_PREGUNTA
	 * 
	 * 
	 * *****************/

	public boolean agregarParticipantePregunta(String idParticipante, String idPregunta){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("Participante", idParticipante);
		values.put("Pregunta", idPregunta);
		long result = db.insert("Participante_Pregunta", "---", values);
		db.close();
        return result != -1;
	}

	public boolean quitarParticipantePregunta(String idParticipante, String idPregunta){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Participante=? and Pregunta=?";
		String[] whereArgs = {idParticipante, idPregunta};
		int count = db.delete("Participante_Pregunta", whereClause, whereArgs);
		db.close();
        return count > 0;
	}
	
	/*********
	 * 
	 * 
	 * PERFIL
	 * 
	 * 
	 * *****************/

	public boolean altaPerfil(String nombre){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("Nombre", nombre);
		long result = db.insert("Perfil", "---", values);
		db.close();
        return result != -1;
	}

	public boolean quitarPerfil(String nombre){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Nombre=?";
		String[] whereArgs = {nombre};
		int count = db.delete("Perfil",whereClause,whereArgs);
		db.close();
        return count > 0;
	}

	public boolean cambiaPerfil(String nombre){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Nombre=?";
		String whereArgs[] = {nombre};
		ContentValues values = new ContentValues();
		values.put("Nombre", nombre);
        return db.update("Perfil", values, whereClause, whereArgs) > 0;
	}
	
	public boolean existePerfil(String perfil){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"Nombre"};
		String selectionArgs[] = {perfil};
		String whereClause = "Nombre=?";
		Cursor c = db.query("Perfil", columns, whereClause, selectionArgs, null, null, null);
		boolean success = false;
		if( c.getCount() > 0 )
			success = true;
		db.close();
		return success;
	}
	
	/*********
	 * 
	 * 
	 * PREGUNTA
	 * 
	 * 
	 * *****************/

	public boolean altaPregunta(String titulo, String votacion){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		Hasher hasher = new Hasher();
		values.put("ID", hasher.makeHash(titulo));
		values.put("Titulo", titulo);
		values.put("Votacion", hasher.makeHash(votacion));
		long result = db.insert("Pregunta","---",values);
		db.close();
        return result != -1;
	}

	public boolean quitarPregunta(String titulo){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String[] whereArgs = {new Hasher().makeHashString(titulo)};
		int count = db.delete("Pregunta",whereClause,whereArgs);
		db.close();
        return count > 0;
	}

	public boolean cambiaPregunta(String idPregunta, String nTitulo, String nVotacion){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String whereArgs[] = {idPregunta};
		ContentValues values = new ContentValues();
		values.put("ID",new Hasher().makeHash(nTitulo));
		values.put("Titulo",nTitulo);
		values.put("Votacion",nVotacion);
        return db.update("Pregunta", values, whereClause, whereArgs) > 0;
	}

	public String[] obtenerPregunta(){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"ID","Titulo", "Votacion"};
		String selectionArgs[] = null;
		String whereClause = null;
		Cursor c = db.query("Pregunta", columns, whereClause, selectionArgs, null, null, null);
		List<String> direcciones = new ArrayList<String>();
		while(c.moveToNext())
			direcciones.add(c.getString(c.getColumnIndex("ID")) + "," + c.getString(c.getColumnIndex("Titulo")) + "," + c.getString(c.getColumnIndex("Votacion")));
		db.close();
		String[] resultado = new String[1];
		return direcciones.toArray(resultado);
	}
	
	public String obtenerID(String titulo){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"ID"};
		String selectionArgs[] = null;
		String whereClause = null;
		Cursor c = db.query("Pregunta", columns, whereClause, selectionArgs, null, null, null);
		String id = null;
		while(c.moveToNext())
			id = c.getString(c.getColumnIndex("ID"));
		db.close();
		return id;
	}
	
	/*********
	 * 
	 * 
	 * PREGUNTA OPCION
	 * 
	 * 
	 * *****************/

	public boolean agregarOpcionPregunta(String idPregunta, String idOpcion){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("Pregunta", idPregunta);
		values.put("Opcion", idOpcion);
		long result = db.insert("Pregunta_Opcion","---",values);
		db.close();
        return result != -1;
	}

	public boolean quitarPreguntaOpcion(String idPregunta, String idOpcion){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Pregunta=? and Opcion=?";
		String[] whereArgs = {idPregunta, idOpcion};
		int count = db.delete("Pregunta_Opcion",whereClause,whereArgs);
		db.close();
        return count > 0;
	}
	
	/*********
	 * 
	 * 
	 * URNA CONTENIDO
	 * 
	 * 
	 * *****************/

	public boolean aceptaVoto(int idUrna, String voto){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("Voto", voto);
		values.put("Urna", idUrna);
		long result = db.insert("UrnaContenido","---",values);
		db.close();
        return result != -1;
	}

	public String[] obtenerVotos(){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"Voto"};
		String selectionArgs[] = null;
		String whereClause = null;
		Cursor c = db.query("UrnaContenido", columns, whereClause, selectionArgs, null, null, null);
		List<String> direcciones = new ArrayList<String>();
		while(c.moveToNext())
			direcciones.add(c.getString(c.getColumnIndex("Voto")));
		db.close();
		String[] resultado = new String[1];
		return direcciones.toArray(resultado);
	}
	
	/*********
	 * 
	 * 
	 * URNA HANDLER
	 * 
	 * 
	 * *****************/

	public boolean altaUrna(String votacion, String tag){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("Votacion", new Hasher().makeHash(votacion));
		values.put("Tag", tag);
		long result = db.insert("Urna","---",values);
		db.close();
        return result != -1;
	}

	public boolean quitarUrna(int id){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String[] whereArgs = {Integer.valueOf(id).toString()};
		int count = db.delete("Urna",whereClause,whereArgs);
		db.close();
        return count > 0;
	}

	public boolean cambiaUrna(int id, String nuevoTag, String nuevaVotacionReferida){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String whereArgs[] = {Integer.valueOf(id).toString()};
		ContentValues values = new ContentValues();
		values.put("Votacion",nuevaVotacionReferida);
		values.put("Tag",nuevoTag);
        return db.update("Urna", values, whereClause, whereArgs) > 0;
	}

	public String[] obtenerUrnas(){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"ID","Votacion","Tag"};
		String selectionArgs[] = null;
		String whereClause = null;
		Cursor c = db.query("Urna", columns, whereClause, selectionArgs, null, null, null);
		List<String> direcciones = new ArrayList<String>();
		while(c.moveToNext())
			direcciones.add(c.getString(c.getColumnIndex("ID")) + "," + c.getString(c.getColumnIndex("Votacion")) + "," + c.getString(c.getColumnIndex("Tag")));
		db.close();
		String[] resultado = new String[1];
		return direcciones.toArray(resultado);
	}
	
	/*********
	 * 
	 * 
	 * VOTACION
	 * 
	 * 
	 * *****************/


	public boolean altaVotacion(String titulo, String fechaInicio, String fechaFin){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("ID", new Hasher().makeHash(titulo));
		values.put("Titulo", titulo);
		values.put("Fecha_Inicio", fechaInicio);
		values.put("Fecha_Fin", fechaFin);
		long result = db.insert("Votacion","---",values);
		db.close();
        return result != -1;
	}

	public boolean quitarVotacion(String titulo){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String[] whereArgs = {new Hasher().makeHashString(titulo)};
		int count = db.delete("Votacion",whereClause,whereArgs);
		db.close();
        return count > 0;
	}

	public boolean cambiaVotacion(String idVotacion, String nTitulo, String nFechaInicio, String nFechaFin){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String whereArgs[] = {idVotacion};
		ContentValues values = new ContentValues();
		values.put("Titulo",nTitulo);
		values.put("Fecha_Inicio",nFechaInicio);
		values.put("Fecha_Fin",nFechaFin);
        return db.update("Votacion", values, whereClause, whereArgs) > 0;
	}

	public String obteneVotacion(String titulo){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"ID","Titulo", "Fecha_Inicio", "Fecha_Fin"};
		String selectionArgs[] = {titulo};
		String whereClause = "Titulo=?";
		Cursor c = db.query("Votacion", columns, whereClause, selectionArgs, null, null, null);
		List<String> direcciones = new ArrayList<String>();
		while(c.moveToNext())
			direcciones.add(c.getString(c.getColumnIndex("ID")) + "," + c.getString(c.getColumnIndex("Titulo")) + "," + c.getString(c.getColumnIndex("Fecha_Inicio")) + "," + c.getString(c.getColumnIndex("Fecha_Fin")));
		db.close();
		return direcciones.get(0);
	}

	public String[] obteneVotaciones(){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"ID","Titulo", "Fecha_Inicio", "Fecha_Fin"};
		String selectionArgs[] = null;
		String whereClause = null;
		Cursor c = db.query("Votacion", columns, whereClause, selectionArgs, null, null, null);
		List<String> direcciones = new ArrayList<String>();
		while(c.moveToNext())
			direcciones.add(c.getString(c.getColumnIndex("ID")) + "," + c.getString(c.getColumnIndex("Titulo")) + "," + c.getString(c.getColumnIndex("Fecha_Inicio")) + "," + c.getString(c.getColumnIndex("Fecha_Fin")));
		db.close();
		String[] resultado = new String[1];
		return direcciones.toArray(resultado);
	}
	
	/*********
	 * 
	 * 
	 * ZONA VOTACION
	 * 
	 * 
	 * *****************/


	public boolean altaZonaVoto(String direccion, float latitud, float longitud){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("ID", new Hasher().makeHash(direccion));
		values.put("Direccion", direccion);
		values.put("Latitud", latitud);
		values.put("Longitud",longitud);
		long result = db.insert("Zona","---",values);
		db.close();
        return result != -1;
	}
	
	public boolean quitarZona(String direccion){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String[] whereArgs = {new Hasher().makeHashString(direccion)};
		int count = db.delete("Zona",whereClause,whereArgs);
		db.close();
        return count > 0;
	}
	
	public boolean cambiaZona(String antiguaDireccion, String nuevaDireccion, float latitud, float longitud){
		SQLiteDatabase db = getWritableDatabase();
		Hasher hasher = new Hasher();
		String antiguoID = hasher.makeHashString(antiguaDireccion);
		String nuevoID = hasher.makeHashString(antiguaDireccion);
		String whereClause = "ID=?";
		String whereArgs[] = {antiguoID};
		ContentValues values = new ContentValues();
		values.put("ID", nuevoID);
		values.put("Direccion",nuevaDireccion);
		values.put("Latitud",latitud);
		values.put("Longitud",longitud);
        return db.update("Zona", values, whereClause, whereArgs) > 0;
	}
	
	public String[] obtenerZonas(){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"Direccion"};
		String selectionArgs[] = null;
		String whereClause = null;
		Cursor c = db.query("Zona", columns, whereClause, selectionArgs, null, null, null);
		List<String> direcciones = new ArrayList<String>();
		while(c.moveToNext())
			direcciones.add(c.getString(c.getColumnIndex("Direccion")));
		db.close();
		String[] resultado = new String[1];
		return direcciones.toArray(resultado);
	}
}