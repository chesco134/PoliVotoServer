package org.inspira.polivoto.Activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.inspira.polivoto.Fragment.EditaPerfilesFragment;
import org.inspira.polivoto.Fragment.ModificaZonaFragment;
import org.inspira.polivoto.Fragment.ProfilesPickerFragment;
import org.inspira.polivotoserver.R;

import java.util.LinkedList;

import DataBase.Votaciones;
import Shared.Opcion;

/**
 * Created by jcapiz on 29/10/15.
 */
public class CambiaZonaPerfilesActivity extends AppCompatActivity {

    ModificaZonaFragment zonaFragment;
    EditaPerfilesFragment perfilesFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_picker_fragment);
        if(savedInstanceState == null){
            Bundle extras = getIntent().getExtras();
            switch( extras.getInt("action") ) {
                case MainActivity.ZONA_FRAGMENT_ACTION:
                    zonaFragment = new ModificaZonaFragment();
                Bundle args = new Bundle();
                args.putString("last_escuela", new Votaciones(this).obtenerUltimaEscuela());
                zonaFragment.setArguments(args);
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.main_container, zonaFragment, "ZonaFragment")
                        .commit();
                    break;
                case MainActivity.PERFILES_ACTION:
                    perfilesFragment = new EditaPerfilesFragment();
                    Bundle args2 = new Bundle();
                    args2.putStringArray("options", new Votaciones(this).obtenerPerfiles());
                    perfilesFragment.setArguments(args2);
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.main_container,perfilesFragment,"EditaPerfiles")
                            .commit();
                    break;
                default:
                    finish();
            }
        }
    }

    public void aceptar(View view){
        zonaFragment = (ModificaZonaFragment)getSupportFragmentManager()
                .findFragmentByTag("ZonaFragment");
        new Votaciones(this).insertaEscuela(zonaFragment.getValorZona(),null,null);
        setResult(RESULT_OK);
        finish();
    }

    public void aceptar2(View view){
        EditaPerfilesFragment mFragment = (EditaPerfilesFragment)getSupportFragmentManager()
                .findFragmentByTag("EditaPerfiles");
        Votaciones db = new Votaciones(this);
        for(String str : mFragment.getDeletedValues()){
            db.borraPerfil(str);
        }
        String op1 = null;
        try {
            op1 = mFragment.getTitle_option().getText().toString();
        } catch (NullPointerException e) {
            op1 = "";
        }
        if (!op1.equals("")) {
            db.insertaPerfil(op1);
            LinkedList<View> lst = mFragment.getAdditionalRows();
            int length = lst.size();
            for (int i = 0; i<length;i++) {
                Opcion opi = new Opcion();
                View vi = lst.get(i);
                opi.nombre = ((EditText) vi
                        .findViewById(R.id.set_title_option)).getText()
                        .toString();
                db.insertaPerfil(opi.nombre);
            }
            setResult(RESULT_OK);
            finish();
        }
    }
}
