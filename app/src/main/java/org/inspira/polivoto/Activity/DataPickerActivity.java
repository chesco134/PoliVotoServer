package org.inspira.polivoto.Activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.inspira.polivoto.Adapter.PerfilesListAdapter;
import org.inspira.polivoto.Fragment.LocationPickerFragment;
import org.inspira.polivotoserver.R;

import DataBase.Votaciones;

/**
 * Created by jcapiz on 28/10/15.
 */
public class DataPickerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(savedInstanceState == null){
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.pager, new LocationPickerFragment())
                    .commit();
        }
    }

    public void aceptar(View view){
        View v = view.getRootView().findViewById(R.id.location_text);
        if(v != null ){
            String escuela = ((EditText)v).getText().toString();
            if(escuela != null){
                new Votaciones(this).insertaEscuela(escuela,null,null);
                setResult(RESULT_OK);
                finish();
            }else{
                Toast.makeText(this,"Escriba el nombre de su escuela", Toast.LENGTH_SHORT).show();
            }
        }else{
            v = view.getRootView().findViewById(R.id.option_set);
            ListView optionSet = (ListView)v;
            PerfilesListAdapter adapter = ((PerfilesListAdapter)optionSet.getAdapter());
            Votaciones bd = new Votaciones(this);
            for(String str : adapter.getTexts())
                if("".equals(str))
                    adapter.removeElement(str);
                else
                    if(bd.insertaPerfil(str) == -1){
                        Toast.makeText(this,"No se vale repetir n.n",Toast.LENGTH_SHORT).show();
                    }else{
                        adapter.removeElement(str);
                    }
            if(adapter.getCount() == 0)
                setResult(RESULT_OK);
                finish();
        }
    }

    public void add(View view){
        View v = view.getRootView().findViewById(R.id.option_set);
        ListView optionSet = (ListView)v;
        PerfilesListAdapter adapter = ((PerfilesListAdapter)optionSet.getAdapter());
        adapter.addElement("");
        view.getRootView().findViewById(R.id.substract).setEnabled(true);
    }

    public void substract(View view) {
        View v = view.getRootView().findViewById(R.id.option_set);
        ListView optionSet = (ListView) v;
        PerfilesListAdapter adapter = ((PerfilesListAdapter) optionSet.getAdapter());
        if (adapter.getCount() == 1){
            view.setEnabled(false);
        }
        adapter.removeLastElement();
    }
}
