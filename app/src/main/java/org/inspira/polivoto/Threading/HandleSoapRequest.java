package org.inspira.polivoto.Threading;

import android.util.Log;

import org.inspira.polivoto.Networking.soap.ServiceClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by jcapiz on 3/12/15.
 */
public class HandleSoapRequest extends Thread {

    private int action;
    private JSONObject json;
    private boolean error;

    public HandleSoapRequest(){
        error = false;
    }

    public void setAction(int action){
        this.action = action;
    }

    public void setJson(JSONObject json){
        this.json = json;
    }

    @Override
    public void run(){
        try {
            json.put("action",action);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ServiceClient ex = new ServiceClient();
        try {
            error = !"success".equals(ex.sendAction(json.toString()));
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
            error = true;
        }
    }

    public boolean error(){
        return error;
    }
}
