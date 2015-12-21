package org.inspira.polivoto.Networking.soap;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

/**
 * Created by Alfonso 7 on 21/09/2015.
 */
public class ServiceClient {
    private static final String NAMESPACE = "http://votingservice.develops.capiz.org";
    private static final String SOAP_ACTION = "urn:serviceChooser";
    private static final String MAIN_REQUEST_URL = "http://192.168.1.72:8080/FistVotingServiceBank/services/ServAvailableVoteProcesses.ServAvailableVoteProcessesHttpSoap11Endpoint/";


    public String sendAction(String fValue) throws IOException, XmlPullParserException {
        String data = null;
        String methodname = "serviceChooser";

        SoapObject request = new SoapObject(NAMESPACE, methodname);
//        request.addProperty("id", "11");
        request.addProperty("json", fValue);

//        request.addProperty("operacion", "suma");

        SoapSerializationEnvelope envelope = getSoapSerializationEnvelope(request);

        HttpTransportSE ht = getHttpTransportSE();
        ht.call(SOAP_ACTION, envelope);
        //SoapPrimitive resultsString = (SoapPrimitive)envelope.getResponse();
        //data = resultsString.toString();
        Object inti = envelope.getResponse();
        data = inti.toString();
        return data;
    }


    private final SoapSerializationEnvelope getSoapSerializationEnvelope(SoapObject request) {
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.implicitTypes = true;
        //envelope.setAddAdornments(false);
        envelope.setOutputSoapObject(request);

        return envelope;
    }

    private final HttpTransportSE getHttpTransportSE() {
        HttpTransportSE ht = new HttpTransportSE("http://"+grabServerURL()+":5001/PTServer/services/ServicioWeb.ServicioWebHttpSoap11Endpoint/");
        ht.debug = true;
        ht.setXmlVersionTag("<!--?xml version=\"1.0\" encoding= \"UTF-8\" ?-->");
        return ht;
    }

    private String grabServerURL(){
        String url = "189.232.88.212";
        try{
            URL serverURL = new URL("http://votacionesipn.com/services/?tag=rmtAddr");
            HttpURLConnection con = (HttpURLConnection)serverURL.openConnection();
            DataInputStream entrada = new DataInputStream(con.getInputStream());
            byte[] bytesChunk = new byte[512];
            int bytesLeidos;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while((bytesLeidos = entrada.read(bytesChunk))!=-1)
                baos.write(bytesChunk, 0, bytesLeidos);
            JSONObject json = new JSONObject(baos.toString());
            con.disconnect();
            url = json.getString("content");
        }catch(IOException e){
            e.printStackTrace();
            Log.e("Mamushka", e.getMessage());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e("Mamushka", e.getMessage());
        }
        Log.d("El Zukam", url);
        return url;
    }

}