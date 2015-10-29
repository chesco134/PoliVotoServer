package org.inspira.polivoto.Networking;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class HelloWebService {

	private static final String URI = "http://189.143.16.144/"
			+ "FistVotingServiceBank/services/LogIn/validateUser";
	
	public Vector<String> saySomething(String arg1, String arg2, String nombreUsuario, String usrPsswd) {
		Vector<String> response = new Vector<String>();
		try {
			URL url = new URL(URI);
			HttpURLConnection httpURLConnection = (HttpURLConnection) url
					.openConnection();
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setDoOutput(true);
			OutputStreamWriter salida = new OutputStreamWriter(
					httpURLConnection.getOutputStream());
			salida.write("usrAdmin=");
			salida.write(URLEncoder.encode(arg1,"UTF-8"));
			salida.write("&psswdAdmin=");
			salida.write(URLEncoder.encode(arg2,"UTF-8"));
			salida.write("&usr=");
			salida.write(URLEncoder.encode(nombreUsuario,"UTF-8"));
			salida.write("&psswd=");
			salida.write(URLEncoder.encode(usrPsswd,"UTF-8"));
			salida.flush();
			if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				SAXParserFactory fabrica = SAXParserFactory.newInstance();
				SAXParser parser = fabrica.newSAXParser();
				XMLReader lector = parser.getXMLReader();
				ManejadorServicioWeb manejador = new ManejadorServicioWeb();
				lector.setContentHandler(manejador);
				lector.parse(new InputSource(httpURLConnection.getInputStream()));
				response = manejador.getLista();
			} else {
				response.add(httpURLConnection.getResponseMessage());
				Log.e("Ñe", httpURLConnection.getResponseMessage());
			}
		} catch (IOException | SAXException e) {
			e.printStackTrace();
			response.add(e.toString());
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			response.add(e.toString());
		}
		return response;
	}
	
	public Vector<String> loadPsswd(String arg1, String arg2, String escuela) {
		Vector<String> response = new Vector<String>();
		try {
			URL url = new URL(URI);
			HttpURLConnection httpURLConnection = (HttpURLConnection) url
					.openConnection();
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setDoOutput(true);
			OutputStreamWriter salida = new OutputStreamWriter(
					httpURLConnection.getOutputStream());
			salida.write("usrAdmin=");
			salida.write(URLEncoder.encode(arg1,"UTF-8"));
			salida.write("&psswdAdmin=");
			salida.write(URLEncoder.encode(arg2,"UTF-8"));
			salida.write("&psswd=");
			salida.write(URLEncoder.encode(escuela,"UTF-8"));
			salida.flush();
			if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				SAXParserFactory fabrica = SAXParserFactory.newInstance();
				SAXParser parser = fabrica.newSAXParser();
				XMLReader lector = parser.getXMLReader();
				ManejadorServicioWeb manejador = new ManejadorServicioWeb();
				lector.setContentHandler(manejador);
				lector.parse(new InputSource(httpURLConnection.getInputStream()));
				response = manejador.getLista();
			} else {
				response.add(httpURLConnection.getResponseMessage());
				Log.e("Ñe", httpURLConnection.getResponseMessage());
			}
		} catch (IOException | SAXException e) {
			e.printStackTrace();
			response.add(e.toString());
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			response.add(e.toString());
		}
		return response;
	}
	
	public Vector<String> checkPassword(String arg1, String arg2, String usrPsswd) {
		Vector<String> response = new Vector<String>();
		try {
			URL url = new URL(URI);
			HttpURLConnection httpURLConnection = (HttpURLConnection) url
					.openConnection();
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setDoOutput(true);
			OutputStreamWriter salida = new OutputStreamWriter(
					httpURLConnection.getOutputStream());
			salida.write("usrAdmin=");
			salida.write(URLEncoder.encode(arg1,"UTF-8"));
			salida.write("&psswdAdmin=");
			salida.write(URLEncoder.encode(arg2,"UTF-8"));
			salida.write("&psswd=");
			salida.write(URLEncoder.encode(usrPsswd,"UTF-8"));
			salida.flush();
			if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				SAXParserFactory fabrica = SAXParserFactory.newInstance();
				SAXParser parser = fabrica.newSAXParser();
				XMLReader lector = parser.getXMLReader();
				ManejadorServicioWeb manejador = new ManejadorServicioWeb();
				lector.setContentHandler(manejador);
				lector.parse(new InputSource(httpURLConnection.getInputStream()));
				response = manejador.getLista();
			} else {
				response.add(httpURLConnection.getResponseMessage());
				Log.e("Ñe", httpURLConnection.getResponseMessage());
			}
		} catch (IOException | SAXException e) {
			e.printStackTrace();
			response.add(e.toString());
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			response.add(e.toString());
		}
		return response;
	}

	class ManejadorServicioWeb extends DefaultHandler {
		private Vector<String> lista;
		private StringBuilder cadena;

		public Vector<String> getLista() {
			return lista;
		}

		@Override
		public void startDocument() throws SAXException {
			cadena = new StringBuilder();
			lista = new Vector<String>();
		}

		@Override
		public void characters(char ch[], int comienzo, int longitud) {
			cadena.append(ch, comienzo, longitud);
		}

		@Override
		public void endElement(String uri, String nombreLocal,
				String nombreCualif) throws SAXException {
			if (nombreLocal.equals("return")) {
				try {
					lista.add(URLDecoder.decode(cadena.toString(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			cadena.setLength(0);
		}
	}
}
