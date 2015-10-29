package org.inspira.polivoto.Activity;

import java.text.DateFormat;
import java.util.Date;

import org.inspira.polivotoserver.FetchAddressIntentService;
import org.inspira.polivotoserver.FetchAddressIntentService.Constants;
import org.inspira.polivotoserver.R;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import DataBase.Votaciones;
import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class RetrieveZoneActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener{

	private EditText direccion;
	private Button confirmar;	
	
	private GoogleApiClient mGoogleApiClient;
	private String mLatitude;
	private String mLongitude;
	private LocationRequest mLocationRequest;
	private boolean mRequestingLocationUpdates;
	private Location mCurrentLocation;
	private String mLastUpdateTime;
    protected Location mLastLocation;
    private AddressResultReceiver mResultReceiver;
	private boolean mAddressRequested;
	public String mAddressOutput;

	protected synchronized void buildGoogleApiClient() {
	    mGoogleApiClient = new GoogleApiClient.Builder(this)
	        .addConnectionCallbacks(this)
	        .addOnConnectionFailedListener((OnConnectionFailedListener) this)
	        .addApi(LocationServices.API)
	        .build();
	}

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
            }

        }
    }
    
    private void displayAddressOutput(){
    	direccion.setText(mAddressOutput);
    }

    private void showToast(String message){
    	Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.especificar_zona_votacion);
		buildGoogleApiClient();
		direccion = (EditText)findViewById(R.id.psswd1);
		confirmar = (Button)findViewById(R.id.confirmar);
		confirmar.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				String dir = direccion.getText().toString();
				if(!dir.equals("")){
					new Votaciones(RetrieveZoneActivity.this).altaZonaVoto(dir, Float.parseFloat(mLatitude), Float.parseFloat(mLongitude));
				}
			}
		});
	}

	/**
	 * Implementación de la interface de GoogleApiClient 
	 *
	 ******************************************************/
	
	protected void createLocationRequest() {
		    LocationRequest mLocationRequest = new LocationRequest();
		    mLocationRequest.setInterval(10000);
		    mLocationRequest.setFastestInterval(5000);
		    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		}
/*
	protected void startLocationUpdates() {
	    LocationServices.FusedLocationApi.requestLocationUpdates(
	            mGoogleApiClient, mLocationRequest, this);
	}

    private void updateUI() {
    	
        mLatitudeTextView.setText(String.valueOf(mCurrentLocation.getLatitude()));
        mLongitudeTextView.setText(String.valueOf(mCurrentLocation.getLongitude()));
        mLastUpdateTimeTextView.setText(mLastUpdateTime);
        
    }
	// Esto va a usarse en el @Override {onPause}
	protected void stopLocationUpdates() {
	    LocationServices.FusedLocationApi.removeLocationUpdates(
	            mGoogleApiClient, this);
	}
	
	// El siguiente código va en el @Override {onResume}
    // if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
    //     startLocationUpdates();
    // }
	 // Guardar el estado de la ubicación
	 	public void onSaveInstanceState(Bundle savedInstanceState) {
		    savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
		            mRequestingLocationUpdates);
		    savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
		    savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
		    super.onSaveInstanceState(savedInstanceState);
		}
		
		// Se ejecuta en el onCreate
		private void updateValuesFromBundle(Bundle savedInstanceState) {
		    if (savedInstanceState != null) {
		        // Update the value of mRequestingLocationUpdates from the Bundle, and
		        // make sure that the Start Updates and Stop Updates buttons are
		        // correctly enabled or disabled.
		        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
		            mRequestingLocationUpdates = savedInstanceState.getBoolean(
		                    REQUESTING_LOCATION_UPDATES_KEY);
		            setButtonsEnabledState();
		        }
		
		        // Update the value of mCurrentLocation from the Bundle and update the
		        // UI to show the correct latitude and longitude.
		        if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
		            // Since LOCATION_KEY was found in the Bundle, we can be sure that
		            // mCurrentLocationis not null.
		            mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
		        }
		
		        // Update the value of mLastUpdateTime from the Bundle and update the UI.
		        if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
		            mLastUpdateTime = savedInstanceState.getString(
		                    LAST_UPDATED_TIME_STRING_KEY);
		        }
		        updateUI();
		    }
		}
*/
	public void fetchAddressButtonHandler(View view) {
	    // Only start the service to fetch the address if GoogleApiClient is
	    // connected.
	    if (mGoogleApiClient.isConnected() && mLastLocation != null) {
	        startIntentService();
	    }
	    // If GoogleApiClient isn't connected, process the user's request by
	    // setting mAddressRequested to true. Later, when GoogleApiClient connects,
	    // launch the service to fetch the address. As far as the user is
	    // concerned, pressing the Fetch Address button
	    // immediately kicks off the process of getting the address.
	    mAddressRequested = true;
	    //updateUIWidgets();
	}
	 
	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.no_geocoder_available,
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (mAddressRequested) {
                startIntentService();
            }

            mLatitude = (String.valueOf(mLastLocation.getLatitude()));
            mLongitude = (String.valueOf(mLastLocation.getLongitude()));
        }
        
        if (mRequestingLocationUpdates) {
            //startLocationUpdates(); // Usarse en caso de requerir actualizaciones de ubicación periódica
        }

		
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        //updateUI();

		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}
}
