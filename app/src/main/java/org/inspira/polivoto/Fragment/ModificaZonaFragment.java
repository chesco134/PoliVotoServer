package org.inspira.polivoto.Fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.inspira.polivotoserver.R;

/**
 * Created by jcapiz on 29/10/15.
 */
public class ModificaZonaFragment extends Fragment {

    EditText valorZona;

    public String getValorZona(){
        return valorZona.getText().toString();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.cambia_zona, parent, false);
        valorZona = (EditText)rootView.findViewById(R.id.valor_zona);
        if( savedInstanceState == null ) {
            Bundle args = getArguments();
            valorZona.setText(args.getString("last_escuela"));
        }
        return rootView;
    }
}
