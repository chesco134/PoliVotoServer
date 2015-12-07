package org.inspira.polivotoserver.tul;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import org.inspira.polivotoserver.R;

public class Preferences extends PreferenceActivity {

	private static final String TUL = "org.inspira.polivotoserver.tul.TUL";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_tul);
    }
    
    public void TUL(){
    	
    }
}