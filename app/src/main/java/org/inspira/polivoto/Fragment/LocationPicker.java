package org.inspira.polivoto.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.inspira.polivotoserver.R;

/**
 * Created by jcapiz on 25/10/15.
 */
public class LocationPicker extends Fragment {

    Activity activity;
    Button confirmar;
    Button gpsLauncher;
    EditText locationText;

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.especificar_zona_votacion,root,false);
        confirmar = (Button)rootView.findViewById(R.id.confirmar);
        locationText = (EditText)rootView.findViewById(R.id.location_text);
        return rootView;
    }
}
