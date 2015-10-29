package org.inspira.polivoto.Fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.inspira.polivotoserver.R;

/**
 * Created by jcapiz on 28/10/15.
 */
public class LocationPickerFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInsntaceState){
        View rootView = inflater.inflate(R.layout.especificar_zona_votacion,parent,false);
        return rootView;
    }
}
