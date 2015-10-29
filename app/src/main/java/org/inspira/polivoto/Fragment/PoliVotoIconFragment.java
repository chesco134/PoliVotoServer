package org.inspira.polivoto.Fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.inspira.polivotoserver.R;

public final class PoliVotoIconFragment extends Fragment{

	public PoliVotoIconFragment(){	
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState){
		View rootView = inflater.inflate(R.layout.polivoto_fragment_logo, root, false);
		return rootView;
	}
}
