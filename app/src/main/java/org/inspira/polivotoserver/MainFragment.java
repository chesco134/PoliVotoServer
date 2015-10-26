package org.inspira.polivotoserver;

import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainFragment extends Fragment {
	
	private static final int LIMMIT_OF_PLUS_ROWS = 10;
	private int numberOfAditionalRows = 2;
	private AppCompatActivity activity;
	private Button substract;
	private Button add;
	private TextView totalOptions;
	private TextView titleLabel;
	private LinearLayout optionSet;
	private LinkedList<View> additionalRows;
	private EditText title;
	private EditText title_option;
	private EditText title_option_2;
	private String[] optionText;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = (AppCompatActivity) activity;
	}

	public LinkedList<View> getAdditionalRows() {
		return additionalRows;
	}

	public EditText getTitle() {
		return title;
	}

	public EditText getTitle_option() {
		return title_option;
	}

	public EditText getTitle_option_2() {
		return title_option_2;
	}

	public TextView getTitleLabel() {
		return titleLabel;
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
		View rootView = inflater.inflate(R.layout.question_form, root, false);
		substract = (Button) rootView.findViewById(R.id.substract);
		add = (Button) rootView.findViewById(R.id.add);
		title = (EditText) rootView.findViewById(R.id.set_title);
		titleLabel = (TextView) rootView.findViewById(R.id.set_title_label);
		titleLabel.append(getArguments().getString("header"));
		title_option = (EditText) rootView.findViewById(R.id.set_title_option);
		title_option_2 = (EditText) rootView
				.findViewById(R.id.set_title_option_2);
		totalOptions = (TextView) rootView.findViewById(R.id.total_options);
		optionSet = (LinearLayout) rootView.findViewById(R.id.option_set);
		additionalRows = new LinkedList<View>();
		if (savedInstanceState != null){
			optionText = savedInstanceState.getStringArray("options");
			for(int i = 2; i<optionText.length;i++){
				View v = inflater.inflate(R.layout.pair_options, optionSet,false);
				additionalRows.add(v);
				optionSet.addView(v);
			}
		}
		numberOfAditionalRows = additionalRows.size() + 2;
		totalOptions.setText(Integer.valueOf(numberOfAditionalRows).toString());
		substract.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (numberOfAditionalRows > 2) {
					optionSet.removeView(optionSet
							.findViewById(numberOfAditionalRows));
					numberOfAditionalRows--;
					additionalRows.removeLast();
					totalOptions.setText(Integer.valueOf(numberOfAditionalRows)
							.toString());
					if (!add.isEnabled())
						add.setEnabled(true);
					if (numberOfAditionalRows == 2)
						((Button) view).setEnabled(false);
				} else {
					((Button) view).setEnabled(false);
				}
			}
		});
		add.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (numberOfAditionalRows < LIMMIT_OF_PLUS_ROWS) {
					View newRow = ((LayoutInflater) activity
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
							.inflate(R.layout.pair_options, optionSet, false);
					additionalRows.add(newRow);
					numberOfAditionalRows++;
					newRow.setId(numberOfAditionalRows);
					optionSet.addView(newRow);
					totalOptions.setText(Integer.valueOf(numberOfAditionalRows)
							.toString());
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
		if( numberOfAditionalRows > 2 )
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
			title_option_2.setText(optionText[1]);
			for(int i = 0; i<additionalRows.size();i++){
				((EditText)additionalRows.get(i).findViewById(R.id.set_title_option)).setText(optionText[i+2]);
			}
		}
	};

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		optionText = new String[additionalRows.size() + 2];
		optionText[0] = title_option.getText().toString();
		optionText[1] = title_option_2.getText().toString();
		for(int i=0;i<additionalRows.size();i++)
			optionText[i+2] = ((TextView)additionalRows.get(i).findViewById(R.id.set_title_option)).getText().toString();
		outState.putStringArray("options", optionText);
	}
}