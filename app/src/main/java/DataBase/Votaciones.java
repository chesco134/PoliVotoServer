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

import org.inspira.polivoto.Security.Hasher;

import Shared.Pregunta;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class Votaciones extends SQLiteOpenHelper{
	
	private static final String FILE_NAME = Environment.getExternalStorageDirectory().getAbsolutePath() + "/chu";
	
	public Votaciones(Context context){
		super(context, "PoliVoto Electrónico", null, 1);
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
		dataBase.execSQL("create table Participante(Boleta TEXT, idPerfil INTEGER NOT NULL, idEscuela INTEGER NOT NULL, Fecha_Registro TEXT, PRIMARY KEY(Boleta), foreign key(idPerfil) references Perfil(idPerfil), foreign key(idEscuela) references Escuela(idEscuela));");
        dataBase.execSQL("create table NombreParticipante(Boleta TEXT NOT NULL, Nombre TEXT NOT NULL, ApPaterno TEXT NOT NULL, ApMaterno TEXT NOT NULL, primary key(Boleta), foreign key(Boleta) references Participante(Boleta))");

        dataBase.execSQL("create table Votacion(idVotacion INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, Fecha_Inicio DATE NOT NULL, Fecha_Fin DATE NOT NULL );");
		// Debo preguntar acerca de tener un idPregunta como entero. ¿Podría sólo dejar como pk a Pregunta y hacer que idVotacion forme parte de la pk? (Relación identificadora)
        dataBase.execSQL("create table Pregunta(idPregunta INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, Pregunta TEXT not null, idVotacion INTEGER NOT NULL, FOREIGN KEY(idVotacion) REFERENCES Votacion(idVotacion));");
		dataBase.execSQL("create table Opcion(idOpcion INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, Reactivo TEXT not null);");
		dataBase.execSQL("create table Pregunta_Opcion(idPregunta INTEGER NOT NULL, idOpcion INTEGER NOT NULL, PRIMARY KEY(idPregunta,idOpcion), FOREIGN KEY(idPregunta) REFERENCES Pregunta(idPregunta), FOREIGN KEY(idOpcion) REFERENCES Opcion(idOpcion));");

		dataBase.execSQL("create table Usuario(idUsuario INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, Name text not null, Psswd blob not null)");
		dataBase.execSQL("create table LoginAttempt(idLoginAttempt INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, idUsuario INTEGER NOT NULL, Attempt_Timestamp text not null, Host text not null, MacAddr text not null, foreign key(idUsuario) references Usuario(idUsuario))");
		dataBase.execSQL("create table AttemptSucceded(idLoginAttempt not null, primary key(idLoginAttempt), foreign key(idLoginAttempt) references LoginAttempt(idLoginAttempt))");
		dataBase.execSQL("create table UserAction(idUserAction INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, idLoginAttempt INTEGER NOT NULL, Action text not null, Action_Timestamp text not null, foreign key(idLoginAttempt) references AttemptSucceded(idLoginAttempt))");

        // Recuerda que el "null hack" son tres guiones. Sólo insertas registros de quienes son capturados al momento de validación.
		dataBase.execSQL("create table Participante_Pregunta(Boleta TEXT not null, idPregunta INTEGER NOT NULL, Hora_Registro text not null, Hora_Participacion text, PRIMARY KEY(Boleta,idPregunta), FOREIGN KEY(Boleta) REFERENCES Participante(Boleta), FOREIGN KEY(idPregunta) REFERENCES Pregunta(idPregunta));");
		dataBase.execSQL("create table Voto(idVoto blob not null, idVotacion INTEGER NOT NULL, idPerfil INTEGER NOT NULL, Voto blob not null, idLoginAttempt INTEGER NOT NULL, primary key(idVoto), FOREIGN KEY(idVotacion) REFERENCES Votacion(idVotacion), foreign key(idPerfil) references Perfil(idPerfil), foreign key(idLoginAttempt) references AttemptSucceded(idLoginAttempt));");

        // Vista que sirve para tener a la mano las preguntas totales de cada votación.
        dataBase.execSQL("create view if not exists Pregntas_Votacion as select idVotacion,count(*) as Preguntas from Pregunta group by idVotacion");
    }

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

    public long insertaParticipante(String boleta, String perfil, String escuela){
        long id = -1;
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
                id = getWritableDatabase().insert("Participante","---",values);
            }
            c2.close();
        }
        c.close();
        close();
        return id;
    }

    public long insertaNombreParticipante(String boleta, String apPaterno, String apMaterno){
        ContentValues values = new ContentValues();
        values.put("Boleta",boleta);
        values.put("ApPaterno",apPaterno);
        values.put("ApMaterno", apMaterno);
        long id = getWritableDatabase().insert("NombreParticipante","---",values);
        close();
        return id;
    }

    public long insertaVotacion(String titulo, String fechaInicio, String fechaFin){
        ContentValues values = new ContentValues();
        values.put("Titulo",titulo);
        values.put("Fecha_Inicio",fechaInicio);
        values.put("Fecha_Fin", fechaFin);
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

    public long insertaLoginAttempt(String usr, String host, String macAddr){
        String[] columns = {"idUsuario"};
        String selection = "Name = ?";
        String selArgs[] = {usr};
        Cursor c = getReadableDatabase().query("Usuario", columns, selection,selArgs,null,null,null);
        long usrId = -1;
        if( c.moveToFirst() ){
            ContentValues values = new ContentValues();
            values.put("idUsuario", c.getInt(c.getColumnIndex("idUsuario")));
            values.put("Host",host);
            values.put("MacAddr",macAddr);
            values.put("Attempt_Timestamp",new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(new Date()));
            usrId = getWritableDatabase().insert("LoginAttempt", "---", values);
        }
        c.close();
        close();
        return usrId;
    }

    public long insertaAttemptSucceded(String usrName){
        long result = -1;
        String columns[] = {"idUsuario"};
        String selection = "Name = ?";
        String selArgs[] = {usrName};
        Cursor c = getReadableDatabase().query("Usuario", columns, selection, selArgs, null, null, null);
        if( c.moveToFirst() ){
            ContentValues values = new ContentValues();
            values.put("idUsuario", c.getInt(c.getColumnIndex("idUsuario")));
            result = getWritableDatabase().insert("AttemptSucceded", "---", values);
        }
        c.close();
        close();
        return result;
    }

    public long insertUserAction(String usrName,int idLoginAttempt, String action){
        long result = -1;
        String[] args = {String.valueOf(idLoginAttempt),usrName};
        Cursor c = getReadableDatabase().rawQuery("select Name from (select idUsuario, " +
                "from (LoginAttempt left join (select idLoginAttempt from AttemptSucceded " +
                "where idLoginAttempt = ?) t on " +
                "LoginAttempt.idLoginAttempt = t.idLoginAttempt) ) t1 where t1.Name = ?", args);
        if(c.getCount() > 0){
            ContentValues values = new ContentValues();
            values.put("idLoginAttempt",idLoginAttempt);
            values.put("Action", action);
            result = getWritableDatabase().insert("UserAction", "---", values);
        }
        c.close();
        close();
        return result;
    }

    public long insertaParticipantePregunta(String boleta, String pregunta){
        long id = -1;
        String[] args = {pregunta};
        Cursor c = getReadableDatabase().rawQuery("select idPregunta from Pregunta where Pregunta = ?", args);
        if(c.moveToFirst()){
            ContentValues values = new ContentValues();
            values.put("Boleta",boleta);
            values.put("idPregunta", c.getInt(c.getColumnIndex("idPregunta")));
            values.put("Hora_Registro", new SimpleDateFormat("dd/MM/yyyy hh:mm:ssss").format(new Date()));
            id = getWritableDatabase().insert("Participante_Pregunta","---",values);
        }
        c.close();
        close();
        return id;
    }

    public long insertaVoto(byte[] idVoto, String tituloVotacion, String perfil, byte[] voto, int idLoginAttempt){
        long id = -1;
        String[] args = {tituloVotacion};
        Cursor c = getReadableDatabase().rawQuery("select idVotacion from Votacion where Titulo = ?",args);
        if(c.moveToFirst()){
            args[0] = perfil;
            Cursor c2 = getReadableDatabase().rawQuery("select idPerfil from Perfil where perfil = ?",args);
            if(c2.moveToFirst()){
                ContentValues values = new ContentValues();
                values.put("idVoto",idVoto);
                values.put("idVotacion",c.getInt(c.getColumnIndex("idVotacion")));
                values.put("idPerfil",c2.getInt(c2.getColumnIndex("idPerfil")));
                values.put("Voto",voto);
                values.put("idLoginAttempt",idLoginAttempt);
                id = getWritableDatabase().insert("Voto","---",values);
            }
            c2.close();
        }
        c.close();
        close();
        return id;
    }

	public boolean consultaUsuario(String usrName, byte[] psswd){
		boolean result = false;
		String[] args = {usrName};
		Cursor c = getReadableDatabase().rawQuery("select Psswd from Usuario where Name = ?",args);
		if(c.moveToFirst()){
			if (Arrays.equals(c.getBlob(c.getColumnIndex("Psswd")), psswd))
				result = true;
		}
		close();
		return result;
	}

    public boolean consultaEscuela(){
        boolean result = getReadableDatabase().rawQuery("select * from Escuela",null).getCount()
                > 0 ? true : false;
        close();
        return result;
    }

    public boolean consultaPerfiles(){
        boolean result = getReadableDatabase().rawQuery("select * from Perfil",null).getCount()
                > 0 ? true : false;
        close();
        return result;
    }

    public boolean actualizaUsuarioPsswd(String usrName, byte[] psswd){
        ContentValues values = new ContentValues();
        values.put("Psswd",psswd);
        String[] selArgs = {usrName};
        boolean result = getWritableDatabase().update("Usuario", values, "Name=?", selArgs) > 0 ? true : false;
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
        getWritableDatabase().delete("Perfil","perfil = ?",args);
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
		if (result != -1)
			return true;
		else
			return false;
	}
	
	public boolean updateKey(String oldKey, String newKey, String user, String zone){// Valores key deven venir "hasheados"
		SQLiteDatabase db = getWritableDatabase();
		String[] whereArgs = {user, oldKey, zone};
		ContentValues values = new ContentValues();
		values.put("VALUE", newKey);
		int rowsAffected = db.update("Key", values, "NOMBRE=? and VALUE=? and Zona=?", whereArgs);
		db.close();
		if( rowsAffected == 0){
			return false;
		}else{
			return true;
		}
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
		long result = db.insert("Opcion","---",values);
		db.close();
		if( result != -1 )
			return true;
		else
			return false;
	}

	public boolean quitarOpcion(String texto){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String[] whereArgs = {new Hasher().makeHashString(texto)};
		int count = db.delete("Opcion",whereClause,whereArgs);
		db.close();
		if( count > 0)
			return true;
		else
			return false;
	}

	public boolean cambiaOpcion(String idOpcion, String nTexto){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String whereArgs[] = {idOpcion};
		ContentValues values = new ContentValues();
		values.put("ID",new Hasher().makeHash(nTexto));
		values.put("Texto",nTexto);
		if( db.update("Opcion",values,whereClause,whereArgs) > 0)
			return true;
		else
			return false;
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

	public long insertaRegistro(String[] preguntas){
		SQLiteDatabase db = getWritableDatabase();
		long id = -1;
		try{
			Log.d("Chale", "MMMMMMMMMMMMMMMMMMM");
			BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(new File(FILE_NAME))));
			String row;
			String elements[];
			ContentValues values;
			while((row = bf.readLine()) != null){
				Log.d("Chale", row);
				elements = row.split(",");
				try{
					values = new ContentValues();
					values.put("Nombre", elements[1] + " " + elements[2] + " " + elements[3]);
					values.put("Boleta", elements[0]);
					values.put("Perfil", elements[4]);
					values.put("Hora_Registro",new SimpleDateFormat("hh:mm:ss").format(new Date()));
					id = db.insert("Participantin", "---", values);
					for(String pregunta : preguntas){
						values = new ContentValues();
						values.put("Boleta", elements[0]);
						values.put("Pregunta", pregunta);
						if( id != -1 )
							if( "PAAE".equals(elements[4]) )
								id = db.insert("VotandoPAAE", "---", values);
							else if( "DOCENTE".equals(elements[4]) )
								id = db.insert("VotandoDOCENTE","---",values);
							else if("Alumno".equals(elements[4]))
								id = db.insert("VotandoAlumno","---",values);
							else
								id = db.insert("VotandoLibre", "---", values);
						if(id!=-1)
							Log.d("El ZUKAM!!", "Inserción de registro correcto");
						else
							Log.d("El ZUKAM!!","fallo de inserción. ");
					}
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
	
	public boolean consultaParticipante(String boleta){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"Boleta"};
		String selection = "Boleta=?";
		String[] selectionArgs = {boleta};
		Cursor c = db.query("Participantin", columns, selection, selectionArgs, null, null, null);
		boolean isIn;
		if(c.getCount() > 0)		
			isIn = true;
		else
			isIn = false;
		db.close();
		return isIn;
	}
	
	public boolean consultaParticipanteHoraVoto(String boleta){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"Hora_Voto"};
		String selection = "Boleta=?";
		String[] selectionArgs = {boleta};
		Cursor c = db.query("Participantin", columns, selection, selectionArgs, null, null, null);		
		boolean finishedVoting;
		c.moveToNext();
		if(c.getString(c.getColumnIndex("Hora_Voto")) != null)
			finishedVoting = true;
		else
			finishedVoting = false;
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
		if( count > 0)
			return true;
		else
			return false;
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
		if( db.update("Participante",values,whereClause,whereArgs) > 0)
			return true;
		else
			return false;
	}

	public String[] obtenerParticipantes(){
		SQLiteDatabase db = getReadableDatabase();
		String[] columns = {"Boleta","Nombre","Perfil","Fecha_Registro","Fecha_Ultima_Modificacion"};
		String selectionArgs[] = null;
		String whereClause = null;
		Cursor c = db.query("Participante", columns, whereClause, selectionArgs, null, null, null);
		List<String> direcciones = new ArrayList<String>();
		while(c.moveToNext())
			direcciones.add(c.getString(c.getColumnIndex("Boleta")) + "," + c.getString(c.getColumnIndex("Nombre")) + "," + c.getString(c.getColumnIndex("Perfil")) + "," + c.getString(c.getColumnIndex("Fecha_Registro")) + "," + c.getString(c.getColumnIndex("Fecha_Ultima_Modificacion")));
		db.close();
		String[] resultado = new String[1];
		return direcciones.toArray(resultado);
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
		long result = db.insert("Participante_Perfil","---",values);
		db.close();
		if( result != -1 )
			return true;
		else
			return false;
	}

	public boolean quitarParticipantePerfil(String idParticipante, String idPerfil){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Participante=? and Perfil=?";
		String[] whereArgs = {idParticipante, idPerfil};
		int count = db.delete("Participante_Perfil",whereClause,whereArgs);
		db.close();
		if( count > 0)
			return true;
		else
			return false;
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
		long result = db.insert("Participante_Pregunta","---",values);
		db.close();
		if( result != -1 )
			return true;
		else
			return false;
	}

	public boolean quitarParticipantePregunta(String idParticipante, String idPregunta){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Participante=? and Pregunta=?";
		String[] whereArgs = {idParticipante, idPregunta};
		int count = db.delete("Participante_Pregunta",whereClause,whereArgs);
		db.close();
		if( count > 0)
			return true;
		else
			return false;
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
		long result = db.insert("Perfil","---",values);
		db.close();
		if( result != -1 )
			return true;
		else
			return false;
	}

	public boolean quitarPerfil(String nombre){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Nombre=?";
		String[] whereArgs = {nombre};
		int count = db.delete("Perfil",whereClause,whereArgs);
		db.close();
		if( count > 0)
			return true;
		else
			return false;
	}

	public boolean cambiaPerfil(String nombre){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Nombre=?";
		String whereArgs[] = {nombre};
		ContentValues values = new ContentValues();
		values.put("Nombre",nombre);
		if( db.update("Perfil",values,whereClause,whereArgs) > 0)
			return true;
		else
			return false;
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
		if( result != -1 )
			return true;
		else
			return false;
	}

	public boolean quitarPregunta(String titulo){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String[] whereArgs = {new Hasher().makeHashString(titulo)};
		int count = db.delete("Pregunta",whereClause,whereArgs);
		db.close();
		if( count > 0)
			return true;
		else
			return false;
	}

	public boolean cambiaPregunta(String idPregunta, String nTitulo, String nVotacion){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String whereArgs[] = {idPregunta};
		ContentValues values = new ContentValues();
		values.put("ID",new Hasher().makeHash(nTitulo));
		values.put("Titulo",nTitulo);
		values.put("Votacion",nVotacion);
		if( db.update("Pregunta",values,whereClause,whereArgs) > 0)
			return true;
		else
			return false;
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
		if( result != -1 )
			return true;
		else
			return false;
	}

	public boolean quitarPreguntaOpcion(String idPregunta, String idOpcion){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "Pregunta=? and Opcion=?";
		String[] whereArgs = {idPregunta, idOpcion};
		int count = db.delete("Pregunta_Opcion",whereClause,whereArgs);
		db.close();
		if( count > 0)
			return true;
		else
			return false;
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
		if( result != -1 )
			return true;
		else
			return false;
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
		if( result != -1 )
			return true;
		else
			return false;
	}

	public boolean quitarUrna(int id){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String[] whereArgs = {Integer.valueOf(id).toString()};
		int count = db.delete("Urna",whereClause,whereArgs);
		db.close();
		if( count > 0)
			return true;
		else
			return false;
	}

	public boolean cambiaUrna(int id, String nuevoTag, String nuevaVotacionReferida){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String whereArgs[] = {Integer.valueOf(id).toString()};
		ContentValues values = new ContentValues();
		values.put("Votacion",nuevaVotacionReferida);
		values.put("Tag",nuevoTag);
		if( db.update("Urna",values,whereClause,whereArgs) > 0)
			return true;
		else
			return false;
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
		if( result != -1 )
			return true;
		else
			return false;
	}

	public boolean quitarVotacion(String titulo){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String[] whereArgs = {new Hasher().makeHashString(titulo)};
		int count = db.delete("Votacion",whereClause,whereArgs);
		db.close();
		if( count > 0)
			return true;
		else
			return false;
	}

	public boolean cambiaVotacion(String idVotacion, String nTitulo, String nFechaInicio, String nFechaFin){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String whereArgs[] = {idVotacion};
		ContentValues values = new ContentValues();
		values.put("Titulo",nTitulo);
		values.put("Fecha_Inicio",nFechaInicio);
		values.put("Fecha_Fin",nFechaFin);
		if( db.update("Votacion",values,whereClause,whereArgs) > 0)
			return true;
		else
			return false;
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
		if( result != -1 )
			return true;
		else
			return false;
	}
	
	public boolean quitarZona(String direccion){
		SQLiteDatabase db = getWritableDatabase();
		String whereClause = "ID=?";
		String[] whereArgs = {new Hasher().makeHashString(direccion)};
		int count = db.delete("Zona",whereClause,whereArgs);
		db.close();
		if( count > 0)
			return true;
		else
			return false;
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
		if( db.update("Zona",values,whereClause,whereArgs) > 0)
			return true;
		else
			return false;
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