package org.inspira.polivoto.Fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.inspira.polivoto.Adapter.PerfilesListAdapter;
import org.inspira.polivotoserver.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jcapiz on 28/10/15.
 */
public class ProfilesPickerFragment extends Fragment {

    private static final int LIMMIT_OF_PLUS_ROWS = 5;
    private int numberOfAditionalRows = 1;
    private AppCompatActivity activity;
    private Button substract;
    private Button add;
    private LinearLayout optionSet;
    private LinkedList<View> additionalRows;
    private EditText title;
    private EditText title_option;
    private String[] optionText;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (AppCompatActivity) activity;
    }

    public LinkedList<View> getAdditionalRows() {
        return additionalRows;
    }

    public void removeView(View view){
        additionalRows.remove(view);
        optionSet.removeView(view);
    }

    public EditText getTitle() {
        return title;
    }

    public EditText getTitle_option() {
        return title_option;
    }

    public LinearLayout getOptionSet(){
        return optionSet;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && optionSet != null) {
            optionSet.removeAllViews();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.selector_perfiles, root, false);
        substract = (Button) rootView.findViewById(R.id.substract);
        add = (Button) rootView.findViewById(R.id.add);
        title = (EditText) rootView.findViewById(R.id.set_title);
        title_option = (EditText) rootView.findViewById(R.id.set_title_option);
        optionSet = (LinearLayout) rootView.findViewById(R.id.option_set);
        additionalRows = new LinkedList<View>();
        if (savedInstanceState != null){
            optionText = savedInstanceState.getStringArray("options");
            for(int i = 1; i<optionText.length;i++){
                View v = inflater.inflate(R.layout.pair_options, optionSet,false);
                additionalRows.add(v);
                optionSet.addView(v);
            }
        }
        numberOfAditionalRows = additionalRows.size() + 1;
        substract.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (numberOfAditionalRows > 1) {
                    optionSet.removeView(optionSet
                            .findViewById(numberOfAditionalRows));
                    numberOfAditionalRows--;
                    additionalRows.removeLast();
                    if (!add.isEnabled())
                        add.setEnabled(true);
                    if (numberOfAditionalRows == 1)
                        ((Button) view).setEnabled(false);
                } else {
                    ((Button) view).setEnabled(false);
                }
            }
        });
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (numberOfAditionalRows < LIMMIT_OF_PLUS_ROWS) {
                   View newRow = ((LayoutInflater) activity
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                            .inflate(R.layout.pair_options, optionSet, false);
                    ((TextView)newRow.findViewById(R.id.set_title_option_label)).setText(R.string.escriba_perfil);
                    ((TextView)newRow.findViewById(R.id.set_title_option)).requestFocus();
                    additionalRows.add(newRow);
                    numberOfAditionalRows++;
                    newRow.setId(numberOfAditionalRows);
                    optionSet.addView(newRow);
                    if (!substract.isEnabled())
                        substract.setEnabled(true);
                    if (numberOfAditionalRows == LIMMIT_OF_PLUS_ROWS)
                        ((Button) view).setEnabled(false);
                } else {
                    ((Button) view).setEnabled(false);
                }
            }
        });
        if( numberOfAditionalRows < LIMMIT_OF_PLUS_ROWS )
            add.setEnabled(true);
        else
            add.setEnabled(false);
        if( numberOfAditionalRows > 1 )
            substract.setEnabled(true);
        else
            substract.setEnabled(false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    };

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            optionText = savedInstanceState.getStringArray("options");
            title_option.setText(optionText[0]);
            for(int i = 0; i<additionalRows.size();i++){
                ((EditText)additionalRows.get(i).findViewById(R.id.set_title_option)).setText(optionText[i+1]);
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        optionText = new String[additionalRows.size() + 1];
        optionText[0] = title_option.getText().toString();
        for(int i=0;i<additionalRows.size();i++)
            optionText[i+1] = ((TextView)additionalRows.get(i).findViewById(R.id.set_title_option)).getText().toString();
        outState.putStringArray("options", optionText);
    }
}
