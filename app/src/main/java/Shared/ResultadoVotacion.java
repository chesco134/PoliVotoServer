package Shared;

import java.io.Serializable;

public class ResultadoVotacion implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private byte[][] cipheredStrings;
	
	public ResultadoVotacion(byte[][] cipheredStrings){
		this.cipheredStrings = cipheredStrings;
	}
	
	public byte[][] getVotes(){
		return cipheredStrings;
	}
}
