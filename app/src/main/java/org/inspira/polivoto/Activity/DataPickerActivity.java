package org.inspira.polivoto.Activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.inspira.polivoto.Adapter.PerfilesListAdapter;
import org.inspira.polivoto.Fragment.LocationPickerFragment;
import org.inspira.polivoto.Fragment.ProfilesPickerFragment;
import org.inspira.polivotoserver.R;

import java.util.LinkedList;
import java.util.ListIterator;

import DataBase.Votaciones;
import Shared.Opcion;
import Shared.Pregunta;

/**
 * Created by jcapiz on 28/10/15.
 */
public class DataPickerActivity extends AppCompatActivity {

    private static final int LIMMIT_OF_PLUS_ROWS = 10;
    private int numberOfAditionalRows = 1;
    private LinkedList<View> additionalRows;
    private String[] optionText;

    public int getAdditionalRows(){
        return additionalRows.size();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_picker_fragment);
        if(savedInstanceState == null){
            if(!new Votaciones(this).consultaEscuela())
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.main_container, new LocationPickerFragment())
                        .commit();
            else
            if(!new Votaciones(this).consultaPerfiles())
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.main_container, new ProfilesPickerFragment(), "Profiles")
                    .commit();
                else{
                    setResult(RESULT_OK);
                    finish();
                }
        }
    }

    public void aceptar2(View view){
        ProfilesPickerFragment mFragment = (ProfilesPickerFragment)getSupportFragmentManager()
                .findFragmentByTag("Profiles");
        String op1 = null;
        try {
            op1 = mFragment.getTitle_option().getText().toString();
        } catch (NullPointerException e) {
            op1 = "";
        }
        if (!op1.equals("")) {
            Votaciones db = new Votaciones(this);
            db.insertaPerfil(op1);
            LinkedList<View> lst = (LinkedList<View>)mFragment.getAdditionalRows().clone();
            int length = lst.size();
            for (int i = 0; i<length;i++) {
                Opcion opi = new Opcion();
                View vi = lst.get(i);
                opi.nombre = ((EditText) vi
                        .findViewById(R.id.set_title_option)).getText()
                        .toString();
                if(db.insertaPerfil(opi.nombre) == -1){
                    Toast.makeText(this,"No se vale repetir n.n",Toast.LENGTH_SHORT).show();
                }else{
                    mFragment.removeView(vi);
                }
            }
            if(mFragment.getAdditionalRows().size()==0){
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    public void aceptar(View view){
        View v = view.getRootView().findViewById(R.id.location_text);
        if(v != null ){
            String escuela = ((EditText)v).getText().toString();
            if(escuela != null){
                new Votaciones(this).insertaEscuela(escuela,null,null);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_container, new ProfilesPickerFragment(), "Profiles")
                        .commit();
            }else{
                Toast.makeText(this,"Escriba el nombre de su escuela", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
