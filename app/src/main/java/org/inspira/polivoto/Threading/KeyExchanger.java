package org.inspira.polivoto.Threading;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by jcapiz on 29/11/15.
 */
public class KeyExchanger extends Thread{

    private DataInputStream entrada;
    private DataOutputStream salida;

    public KeyExchanger(DataInputStream entrada, DataOutputStream salida) {
        this.entrada = entrada;
        this.salida = salida;
    }

    @Override
    public void run(){
        try{
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.genKeyPair();
            Key publicKey = kp.getPublic();
            Key privateKey = kp.getPrivate();
            salida.write(publicKey.getEncoded());
            salida.flush(); //** Successfuly sent public key **//
            byte[] cipheredAESKey = new byte[128];
            entrada.read(cipheredAESKey);
            byte[] encodedAESKey;
            Cipher cip = Cipher.getInstance("RSA");
            cip.init(Cipher.DECRYPT_MODE, privateKey);
            encodedAESKey = cip.doFinal(cipheredAESKey);
            SecretKeySpec skp = new SecretKeySpec(encodedAESKey,"AES");
        }catch(IOException e){
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }
}
