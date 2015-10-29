package org.inspira.polivoto.Fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Created by jcapiz on 29/10/15.
 */
public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

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
        Bundle args = getArguments();
        Intent i = new Intent();
        i.putExtra("hourOfDay",hourOfDay);
        i.putExtra("minute",minute);
        i.putExtra("year", args.getInt("year"));
        i.putExtra("month", args.getInt("month"));
        i.putExtra("day", args.getInt("day"));
        i.putExtra("response",args.getString("response"));
        getActivity().setResult(Activity.RESULT_OK,i);
        getActivity().finish();
    }
}
