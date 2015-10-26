package org.inspira.polivoto.activities;

import org.inspira.polivotoserver.R;

import android.app.Activity;
import android.os.Bundle;

public class SplashActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		Thread t = new Thread(){
			@Override
			public void run(){
				try{
					sleep(3000);
					finish();
				}catch(InterruptedException e){
					
				}
			}
		};
		t.start();
	}
}
