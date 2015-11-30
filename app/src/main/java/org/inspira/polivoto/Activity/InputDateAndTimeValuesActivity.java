package org.inspira.polivoto.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;

import org.inspira.polivoto.Fragment.DatePickerFragment;
import org.inspira.polivotoserver.R;

/**
 * Created by jcapiz on 29/10/15.
 */
public class InputDateAndTimeValuesActivity extends AppCompatActivity {

    private static final int GET_VOTACIONES_TITULO = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.horas_de_votacion);
        if(getIntent().getExtras().getBoolean("requestTitle")) {
            Intent i = new Intent(this, Mensaje.class);
            i.putExtra("msj", "Escriba título de la Votación");
            i.putExtra("isChoice", false);
            i.putExtra("isInputMethod", true);
            startActivityForResult(i, GET_VOTACIONES_TITULO);
        }else{
            setTitle(getIntent().getExtras().getString("title"));
            DialogFragment newFragment2 = new DatePickerFragment();
            Bundle args = new Bundle();
            newFragment2.setArguments(args);
            args.putString("failMessage", "No podemos dar menos de 2 horas");
            newFragment2.show(getSupportFragmentManager(), "datePicker");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != Activity.RESULT_OK){
            finish();
        }else {
            setTitle(getIntent().getExtras().getString("title"));
            DialogFragment newFragment2 = new DatePickerFragment();
            Bundle args = new Bundle();
            args.putString("titulo", data.getExtras().getString("response"));
            args.putString("failMessage", "No podemos viajar al pasado");
            newFragment2.setArguments(args);
            newFragment2.show(getSupportFragmentManager(), "datePicker");
        }
    }
}