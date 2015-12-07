package org.inspira.polivoto.Security;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Hasher {
	public String makeCheckSum(String fileName){
		String hexString = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			FileInputStream fis = new FileInputStream(fileName);
			byte[] dataByte = new byte[1024];
			int nread = 0;
			while((nread = fis.read(dataByte)) != -1){
				md.update(dataByte, 0, nread);
			}
			byte[] mdBytes = md.digest();
			StringBuffer theHexString = new StringBuffer();
			for (int i = 0; i<mdBytes.length; i++){
				theHexString.append(Integer.toHexString(0xFF & mdBytes[i]));
			}
			hexString = theHexString.toString();
			fis.close();
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		}
		return hexString;
	}
	
	public String makeHashString(String psswd){
		String hashPsswd = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(psswd.getBytes());
			byte[] byteData = md.digest();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i<byteData.length; i++ ){
				sb.append(Integer.toString((byteData[i] & 0xff) + 0x100,16).substring(1));
			}
			hashPsswd = sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}		
		return hashPsswd;
	}

	public byte[] makeHash(String psswd){
		byte[] hashPsswd = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(psswd.getBytes());
			hashPsswd = md.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return hashPsswd;
	}

	public byte[] stringParse(String str){
		byte[] bRepresentation = null;

		return bRepresentation;
	}

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

	public static String bytesToString(byte[] byteData){
		// convert the byte to hex format method 1
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < byteData.length; i++) {
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		return sb.toString();
	}
}
