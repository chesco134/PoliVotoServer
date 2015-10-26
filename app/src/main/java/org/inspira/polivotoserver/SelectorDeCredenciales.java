package org.inspira.polivotoserver;

import java.util.LinkedList;
import java.util.ListIterator;

import DataBase.Votaciones;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

public class SelectorDeCredenciales extends AppCompatActivity implements
		ActionBar.TabListener {

	public static final String[] USUARIOS = {
		"Administrador",
		"Capturista",
		"Consultor",
		"Participante"
	};
	private boolean isChangePasswords;
	private int counter;
	private String zona;
	private MyFragmentStatePagerAdapter adapter;
	private static LinkedList<CredencialesFragment> fragmentitos;
	
	protected boolean compruebaCredencial(String usuario){
		boolean isIn = false;
		isIn = new Votaciones(this).revisaExistenciaDeCredencial(usuario);
		return isIn;
	}
	
	protected String[] checkProgress(){
		String[] credencialesFaltantes = null;
		if(!isChangePasswords){
			ListIterator<CredencialesFragment> credencialesFragmentIterator = fragmentitos.listIterator();
			counter = 0;
			while(credencialesFragmentIterator.hasNext()){
				CredencialesFragment currentFragment = credencialesFragmentIterator.next();
				if(!currentFragment.hasSucceded()){
				}else{
					counter++;
				}
			}
			if(counter == fragmentitos.size()){
				setResult(RESULT_OK);
				finish();
			}
		}
		return credencialesFaltantes;
	}
	
	protected boolean guardarCredencial(String usuario, String password){
		boolean success = false;
		SHAExample hasher = new SHAExample();
		success = new Votaciones(this).insertKey(hasher.makeHash(password),usuario, zona);
		return success;
	}
	
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		try{
			zona = getIntent().getExtras().getString("zona");
		}catch(NullPointerException e){
			zona = "Local";
		}
		if (savedInstanceState == null) {
			fragmentitos = new LinkedList<CredencialesFragment>();
			int inicio = 1;
			isChangePasswords = false;
			Bundle extras = getIntent().getExtras();
			if( extras != null ){
				isChangePasswords = extras.getBoolean("isChangePasswords");
			}
			if(isChangePasswords)
				inicio = 0;
			for (int arg0 = inicio; arg0 < 4; arg0++) {
				CredencialesFragment newMainFragment = new CredencialesFragment();
				Bundle args = new Bundle();
				args.putString("usuario", USUARIOS[arg0]);
				args.putBoolean("isChangePasswords", isChangePasswords);
				newMainFragment.setArguments(args);
				fragmentitos.add(newMainFragment);
			}
		}
		
		// Set up the action bar.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		adapter = new MyFragmentStatePagerAdapter(getSupportFragmentManager());
		adapter.setCredencialesForm(fragmentitos);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(adapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < adapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(adapter.getPageTitle(i))
					.setTabListener(this));
		}
	}
	
	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}
	
	public int getCurrentTabPosition(){
		return mViewPager.getCurrentItem();
	}
	
	public void changeTab(int position){
		mViewPager.setCurrentItem(position);
	}
}
