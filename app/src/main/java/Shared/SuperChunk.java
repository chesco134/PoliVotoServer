package Shared;

import java.io.Serializable;
import java.util.LinkedList;

public class SuperChunk implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4711096948148013008L;
	private LinkedList<Pregunta> preguntas;
	
	public SuperChunk(LinkedList<Pregunta> preguntas){
		this.preguntas = preguntas;
	}
	
	public LinkedList<Pregunta> getPreguntas(){
		return preguntas;
	}
}
