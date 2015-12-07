package org.inspira.polivoto.Activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.inspira.polivoto.Networking.soap.SoapServices;
import org.inspira.polivotoserver.R;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by jcapiz on 2/12/15.
 */
public class RemoteServerPickerActivity extends AppCompatActivity {

    private RadioGroup rg;
    private TextView nullMsg;
    private ProgressBar pb;
    private JSONArray jarr;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_server_picker);
        rg = (RadioGroup)findViewById(R.id.lista_votaciones_globales);
        nullMsg = (TextView)findViewById(R.id.null_message);
        pb = (ProgressBar)findViewById(R.id.progressBar);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Toast.makeText(RemoteServerPickerActivity.this,""+checkedId,Toast.LENGTH_SHORT).show();
                Intent i = new Intent();
                try {
                    i.putExtra("json",jarr.getJSONObject(checkedId).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                setResult(RESULT_OK,i);
                finish();
            }
        });
    }

    @Override
    protected void onPostResume(){
        super.onPostResume();
        if(jarr == null)
            new GlobalVotingListFiller().execute();
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString("jarr", jarr.toString());
        outState.putInt("nullMsg", nullMsg.getVisibility());
        outState.putInt("rg",rg.getVisibility());
        outState.putInt("pb", pb.getVisibility());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        try {
            jarr = new JSONArray(savedInstanceState.getString("jarr"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Intent i = new Intent();
        nullMsg.setVisibility(savedInstanceState.getInt("nullMsg"));
        rg.setVisibility(savedInstanceState.getInt("rg"));
        pb.setVisibility(savedInstanceState.getInt("pb"));

    }

    private class GlobalVotingListFiller extends AsyncTask<String,JSONArray,String> {

        @Override
        protected String doInBackground(String... args){
            return new SoapServices().selectAvailableProcess();
        }

        @Override
        protected void onPostExecute(String result){
            try {
                jarr = new JSONArray(result);
                if(jarr.length() == 0){
                    nullMsg.setVisibility(View.VISIBLE);
                    pb.setVisibility(View.INVISIBLE);
                    rg.setVisibility(View.INVISIBLE);
                }else{
                    nullMsg.setVisibility(View.INVISIBLE);
                    pb.setVisibility(View.INVISIBLE);
                    rg.setVisibility(View.VISIBLE);
                }
                RadioButton rb;
                for(int i=0; i<jarr.length();i++){
                    rb = new RadioButton(RemoteServerPickerActivity.this);
                    rb.setText(jarr.getJSONObject(i).getString("Title"));
                    rg.addView(rb);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
