package org.inspira.polivoto.Networking;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by jcapiz on 14/12/15.
 */
public class IOHandler {

    private DataInputStream entrada;
    private DataOutputStream salida;
    private byte[] chunk;
    private int rate;

    public IOHandler(DataInputStream entrada, DataOutputStream salida){
        this.entrada = entrada;
        this.salida = salida;
        rate = 64;
    }

    public byte[] handleIncommingMessage() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int toRead;
        byte[] block = new byte[64];
        ByteArrayOutputStream temp;
        // Anticipa cuantos bytes mandará el emisor, para luego leer por bloques antes de
        // esperar a que la nueva trama llegue.
        while ((toRead = readInt()) > 0) {
            temp = new ByteArrayOutputStream();
            Log.d("Stranger", "Laugh " + toRead);
            int times = toRead/block.length;
            for ( int i=0; i<times; i++ ) {
                entrada.read(block);
                temp.write(block,0,block.length);
            }
            int remaining = toRead - times*block.length;
            if( remaining > 0 ){
                entrada.read(block);
                temp.write(block, 0, remaining);
            }
            baos.write(temp.toByteArray(), 0, temp.size());
            Log.d("Aaahaha", "Llevamos: " + baos.size() + " bytes.");
            writeInt(1);
        }
        Log.d("Greeza","Done reading " + baos.size() + " bytes");
        chunk = baos.toByteArray();
        baos.close();
        return chunk;
    }

    public void sendMessage(byte[] message) throws IOException {
        int times = message.length/rate;
        Log.d("Cyndy","Writing " + times + " blocks from " + message.length + " bytes.");
        for( int i=0; i<times; i++ ){
            writeInt(rate);
            salida.write(message, i * rate, rate);
            Log.d("Cyndi", "Sent something");
            readInt();
            Log.d("Cyndi","Confirmed");
        }
        int remaining = message.length - times*rate;
        if(remaining > 0){
            writeInt(remaining);
            salida.write(message,times*rate,remaining);
            Log.d("Cyndia", "Sent " + remaining + " bytes");
            readInt();
            Log.d("Cyndia", "Confirmed");
        }
        writeInt(0);
    }

    public int readInt() throws IOException{
        return entrada.readInt();
    }

    public void writeInt(int i) throws IOException{
        salida.writeInt(i);
        salida.flush();
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public void close() throws IOException{
        entrada.close();
        salida.close();
    }
}