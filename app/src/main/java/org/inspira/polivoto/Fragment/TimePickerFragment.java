package org.inspira.polivoto.Fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import DataBase.Votaciones;

/**
 * Created by jcapiz on 29/10/15.
 */
public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

    private Bundle args;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Do something with the time chosen by the user
        args = getArguments();
        args.putInt("hourOfDay", hourOfDay);
        args.putInt("minute", minute);
        args.putString("titulo",args.getString("response"));
    }

    @Override
    public void onDestroy(){
        Calendar c = Calendar.getInstance();
        c.set(args.getInt("year"), args.getInt("month"), args.getInt("day"), args.getInt("hourOfDay"), args.getInt("minute"));
        Log.d("Dates smell bad", c.getTime().toString());
        if("No podemos viajar al pasado".equals(getArguments().getString("failMessage"))) {
            if (c.getTime().compareTo(new Date()) >= 0) { // INICIAR SERVICIO
                Votaciones db = new Votaciones(getContext());
                try {
                    if (c.getTime().compareTo(new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(db.obtenerFechaInicioVotacionActual())) >= 0)
                        db.actualizaFechaInicioVotacionActual(new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(c.getTime()));
                }catch(NullPointerException | ParseException e){
                    e.printStackTrace();
                    db.insertaVotacion(args.getString("response"),new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(c.getTime()),null);
                }
                Intent i = new Intent();
                i.putExtra("hourOfDay", args.getInt("hourOfDay"));
                i.putExtra("minute", args.getInt("minute"));
                i.putExtra("year", args.getInt("year"));
                i.putExtra("month", args.getInt("month"));
                i.putExtra("day", args.getInt("day"));
                i.putExtra("response", args.getString("response"));
                getActivity().setResult(Activity.RESULT_OK, i);
                getActivity().finish();
            } else {
                DialogFragment newFragment2 = new DatePickerFragment();
                newFragment2.setArguments(args);
                getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
                newFragment2.show(getActivity().getSupportFragmentManager(), "datePicker");
                Toast.makeText(getContext(), args.getString("failMessage"), Toast.LENGTH_SHORT).show();
            }
        }else if("No podemos dar menos de 2 horas".equals(getArguments().getString("failMessage"))){
            Votaciones db = new Votaciones(getContext());
            Date d = null;
            try{
                d = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse(db.obtenerFechaInicioVotacionActual());
            }catch(ParseException ex){
                ex.printStackTrace();
            }
            Calendar pd = Calendar.getInstance();
            pd.setTime(d);
            pd.set(Calendar.HOUR, c.get(Calendar.HOUR) + 2);
            Log.d("Shuckle", pd.getTime().toString() + "  ## VS ## " + c.getTime().toString() + " :: " +
                    pd.getTime().compareTo(c.getTime()));
            if(pd.getTime().compareTo(c.getTime()) < 0){
                Intent i = new Intent();
                i.putExtra("hourOfDay",args.getInt("hourOfDay"));
                i.putExtra("minute",args.getInt("minute"));
                i.putExtra("year", args.getInt("year"));
                i.putExtra("month", args.getInt("month"));
                i.putExtra("day", args.getInt("day"));
                i.putExtra("response", getArguments().getString("response"));
                db.conservaFechaFinVotacionActual(db.obtenerTituloVotacionActual(),new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(c.getTime()));
                getActivity().setResult(Activity.RESULT_OK, i);
                getActivity().finish();
            }else{
                DialogFragment newFragment2 = new DatePickerFragment();
                newFragment2.setArguments(args);
                newFragment2.show(getActivity().getSupportFragmentManager(), "datePicker");
                Toast.makeText(getContext(),args.getString("failMessage"),Toast.LENGTH_SHORT).show();
            }
        }
        super.onDestroy();
    }
}
