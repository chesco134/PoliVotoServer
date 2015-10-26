package org.inspira.polivotoserver;

import java.util.LinkedList;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class MyFragmentStatePagerAdapter extends FragmentStatePagerAdapter{

	public MyFragmentStatePagerAdapter(FragmentManager fm) {
		super(fm);
		// TODO Auto-generated constructor stub
	}

	LinkedList<MainFragment> preguntas;
	LinkedList<CredencialesFragment> credenciales;
	
	public LinkedList<MainFragment> getPreguntas(){
		return preguntas;
	}
	
	public void setPreguntas(LinkedList<MainFragment> preguntas){
		this.preguntas = preguntas;
	}
	
	public void setCredencialesForm(LinkedList<CredencialesFragment> credenciales){
		this.credenciales = credenciales;
	}

	@Override
	public Fragment getItem(int arg0) {
		if( preguntas != null )
			return preguntas.get(arg0);
		else
			return credenciales.get(arg0);
	}

	@Override
	public int getCount() {
		if( preguntas != null )
			return preguntas.size();
		else
			return credenciales.size();
	}

	@Override
	public CharSequence getPageTitle(int position) {
		if( preguntas != null )
			return "PREGUNTA " + (position + 1);
		else if(credenciales.size() == 4) 
				if( position == 0 )
				return "Admin";
			else
				return credenciales.get(position).getArguments().getString("usuario");
		else return credenciales.get(position).getArguments().getString("usuario");
	}
}
