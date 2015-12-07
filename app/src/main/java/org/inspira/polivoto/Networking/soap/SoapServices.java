package org.inspira.polivoto.Networking.soap;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by jcapiz on 2/12/15.
 */
public class SoapServices {

    private static final String NAMESPACE = "http://votingservice.develops.capiz.org";
    private static final String MAIN_REQUEST_URL = "http://192.168.1.72:8080/FistVotingServiceBank/services/ServAvailableVoteProcesses.ServAvailableVoteProcessesHttpSoap11Endpoint/";

    public String getCelsiusConversion(String fValue) {
        String data = null;
        String methodname = "sayVerga";

        SoapObject request = new SoapObject(NAMESPACE, methodname);
//        request.addProperty("id", "11");
        try {
            JSONObject json = new JSONObject();
            json.put("elTul", URLEncoder.encode("{\"place\":\"upiita\",\"title\":\"Debo mejorar el modo de poner el título.\"}", "utf8"));
            request.addProperty("jstr", fValue.replace(" \\/","-"));//"{\"place\":\"upiicsa\",\"Title\":\"Debo mejorar el modo de poner el título.\"}");
        } catch (JSONException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        request.addProperty("operacion", "suma");

        SoapSerializationEnvelope envelope = getSoapSerializationEnvelope(request);

        HttpTransportSE ht = getHttpTransportSE();
        try {
            ht.call("urn:sayVerga", envelope);
            //SoapPrimitive resultsString = (SoapPrimitive)envelope.getResponse();
            //data = resultsString.toString();
            Object inti = envelope.getResponse();
            data = inti.toString();

        } catch (SocketTimeoutException t) {
            t.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (Exception q) {
            q.printStackTrace();
        }
        return data;
    }

    public String selectAvailableProcess() {
        String data = null;
        String methodname = "selectAvailableProcess";
        SoapObject request = new SoapObject(NAMESPACE, methodname);
//        request.addProperty("id", "11");
//        request.addProperty("txt", fValue);
//        request.addProperty("operacion", "suma");
        SoapSerializationEnvelope envelope = getSoapSerializationEnvelope(request);
        HttpTransportSE ht = getHttpTransportSE();
        try {
            ht.call("urn:selectAvailableProcess", envelope);
            //SoapPrimitive resultsString = (SoapPrimitive)envelope.getResponse();
            //data = resultsString.toString();
            Object inti = envelope.getResponse();
            data = inti.toString();
            JSONArray json = new JSONArray(data);
        } catch (SocketTimeoutException t) {
            t.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (Exception q) {
            q.printStackTrace();
        }
        return data;
    }

    public String publishVotingProcess(String jstr) { // Es el string de un json
        String data = null;
        String methodname = "publishVotingProcess";
        SoapObject request = new SoapObject(NAMESPACE, methodname);
//        request.addProperty("id", "11");
//        request.addProperty("txt", fValue);
        request.addProperty("jsrt", jstr);
        SoapSerializationEnvelope envelope = getSoapSerializationEnvelope(request);
        HttpTransportSE ht = getHttpTransportSE();
        try {
            ht.call("urn:publishVotingProcess", envelope);
            //SoapPrimitive resultsString = (SoapPrimitive)envelope.getResponse();
            //data = resultsString.toString();
            Object inti = envelope.getResponse();
            data = inti.toString();
        } catch (SocketTimeoutException t) {
            t.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (Exception q) {
            q.printStackTrace();
        }
        return data;
    }

    public void joinVotingProcess(String jstr) { // Es el string de un json
        String methodname = "joinVotingProcess";
        SoapObject request = new SoapObject(NAMESPACE, methodname);
//        request.addProperty("id", "11");
//        request.addProperty("txt", fValue);
        request.addProperty("jsrt", jstr);
        SoapSerializationEnvelope envelope = getSoapSerializationEnvelope(request);
        HttpTransportSE ht = getHttpTransportSE();
        try {
            ht.call("urn:joinVotingProcess", envelope);
            //SoapPrimitive resultsString = (SoapPrimitive)envelope.getResponse();
            //data = resultsString.toString();
            Object inti = envelope.getResponse();
        } catch (SocketTimeoutException t) {
            t.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (Exception q) {
            q.printStackTrace();
        }
    }

    public void setQuiz(String jstr) { // Es el string de un json
        String methodname = "setQuiz";
        SoapObject request = new SoapObject(NAMESPACE, methodname);
//        request.addProperty("id", "11");
//        request.addProperty("txt", fValue);
        request.addProperty("jsrt", jstr);
        SoapSerializationEnvelope envelope = getSoapSerializationEnvelope(request);
        HttpTransportSE ht = getHttpTransportSE();
        try {
            ht.call("urn:setQuiz", envelope);
            //SoapPrimitive resultsString = (SoapPrimitive)envelope.getResponse();
            //data = resultsString.toString();
            Object inti = envelope.getResponse();
        } catch (SocketTimeoutException t) {
            t.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (Exception q) {
            q.printStackTrace();
        }
    }

    public void registerVotingPlace(String jstr) { // Es el string de un json
        String methodname = "registerVotingPlace";
        SoapObject request = new SoapObject(NAMESPACE, methodname);
//        request.addProperty("id", "11");
//        request.addProperty("txt", fValue);
        request.addProperty("jsrt", jstr);
        SoapSerializationEnvelope envelope = getSoapSerializationEnvelope(request);
        HttpTransportSE ht = getHttpTransportSE();
        try {
            ht.call("urn:registerVotingPlace", envelope);
            //SoapPrimitive resultsString = (SoapPrimitive)envelope.getResponse();
            //data = resultsString.toString();
            Object inti = envelope.getResponse();
        } catch (SocketTimeoutException t) {
            t.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (Exception q) {
            q.printStackTrace();
        }
    }

    private final SoapSerializationEnvelope getSoapSerializationEnvelope(SoapObject request) {
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.implicitTypes = true;
        //envelope.setAddAdornments(false);
        envelope.setOutputSoapObject(request);
        return envelope;
    }

    private final HttpTransportSE getHttpTransportSE() {
        HttpTransportSE ht = new HttpTransportSE(MAIN_REQUEST_URL);//"http://"+grabServerURL()+":5001/FistVotingServiceBank/services/ServAvailableVoteProcesses.ServAvailableVoteProcessesHttpSoap11Endpoint/");
        ht.debug = true;
        ht.setXmlVersionTag("<!--?xml version=\"1.0\" encoding= \"UTF-8\" ?-->");
        return ht;
    }

    private String grabServerURL(){
        String url = "189.232.88.212";
        try{
            URL serverURL = new URL("http://votacionesipn.com/services/?tag=gimmeAddr");
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
            e.printStackTrace();
            Log.e("Mamushka", e.getMessage());
        }
        Log.d("El Zukam", url);
        return url;
    }
}