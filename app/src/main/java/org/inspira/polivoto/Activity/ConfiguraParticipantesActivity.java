package org.inspira.polivoto.Activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;

import org.inspira.polivotoserver.R;

/**
 * Created by jcapiz on 1/12/15.
 */
public class ConfiguraParticipantesActivity extends PreferenceActivity {

    public static final String USAR_MATRICULA_KEY = "usar_matricula_pref_key";
    public static final String NOMBRE_ARCHIVO_MATRICULA_KEY = "matricula_file_name_pref_key";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

    }
}
