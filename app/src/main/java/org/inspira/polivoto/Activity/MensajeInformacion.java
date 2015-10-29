package org.inspira.polivoto.Activity;

import org.inspira.polivotoserver.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MensajeInformacion extends Activity{

	private TextView hint;
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mensaje_informacion);
		hint = (TextView)findViewById(R.id.hint);
		hint.setText(getIntent().getExtras().getString("hint"));
	}
}
