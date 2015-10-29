package org.inspira.polivoto.Fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import org.inspira.polivoto.Adapter.PerfilesListAdapter;
import org.inspira.polivotoserver.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jcapiz on 28/10/15.
 */
public class ProfilesPickerFragment extends Fragment {

    ListView textos;
    LinkedList<String> texts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.selector_perfiles,parent,false);
        textos = (ListView)rootView.findViewById(R.id.option_set);
        if (savedInstanceState == null ){
            texts = new LinkedList<String>();
            texts.add("");
        }else{
            texts = (LinkedList<String>)savedInstanceState.getSerializable("texts");
            if(texts.size() == 1){
                rootView.findViewById(R.id.substract).setEnabled(false);
            }
        }
        textos.setAdapter(new PerfilesListAdapter(getActivity(),texts));
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        outState.putSerializable("texts", texts);
        super.onSaveInstanceState(outState);
    }
}
