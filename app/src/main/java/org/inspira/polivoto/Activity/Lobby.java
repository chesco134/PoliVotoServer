package org.inspira.polivoto.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.inspira.polivotoserver.MiServicio;
import org.inspira.polivotoserver.R;

/**
 * Created by jcapiz on 22/12/15.
 */
public class Lobby extends AppCompatActivity {

    private MiServicio miServicio;
    private TextView timeLeft;
    private TextView amountOfParticipants;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lobby);
        timeLeft = (TextView)findViewById(R.id.lobby_upper);
        amountOfParticipants = (TextView)findViewById(R.id.lobby_center_label);
        timeLeft.setTypeface(Typeface.createFromAsset(getAssets(),"Roboto-Regular.ttf"));
        amountOfParticipants.setTypeface(Typeface.createFromAsset(getAssets(), "RobotoCondensed-Regular.ttf"));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString("amountOfParticipants", amountOfParticipants.getText().toString());
        outState.putString("timeLeft",timeLeft.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        timeLeft.setText(savedInstanceState.getString("timeLeft"));
        amountOfParticipants.setText(savedInstanceState.getString("amountOfParticipants"));
    }

    @Override
    protected void onStart(){
        super.onStart();
        doBindService();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        doUnbindService();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            miServicio = ((MiServicio.LocalBinder) service).getService();
            miServicio.setMainActivity(Lobby.this);
            if(mIsBound){}
                // Here you call a method to execute something in the service.
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            miServicio = null;
        }
    };

    private boolean mIsBound;

    void doBindService() {
        // Establish a connection with the service. We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        if(!mIsBound){
            bindService(new Intent(this, Lobby.class), mConnection,
                    Context.BIND_AUTO_CREATE);
            mIsBound = true;

        }
    }

    public void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    public void refreshRemainingTime(String label){
        timeLeft.setText(label);
    }

    public void updateCount(int amount){
        amountOfParticipants.setText("" + amount);
    }
}