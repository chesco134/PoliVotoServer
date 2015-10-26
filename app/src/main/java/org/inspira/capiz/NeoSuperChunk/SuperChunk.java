package org.inspira.capiz.NeoSuperChunk;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import Shared.Pregunta;

public class SuperChunk implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4726381152444082703L;
	private LinkedList<Pregunta> preguntas;
	private Map<String,Pregunta> mapaPreguntas;
	
	public SuperChunk(LinkedList<Pregunta> preguntas){
		this.preguntas = preguntas;
		mapaPreguntas = new TreeMap<String,Pregunta>();
		ListIterator<Pregunta> pregs = this.preguntas.listIterator();
		while(pregs.hasNext()){
			Pregunta currentPregunta = pregs.next();
			String g = currentPregunta.titulo;
			mapaPreguntas.put(g,currentPregunta);
		}
	}
	
	public Pregunta getPregunta(String titulo){
		return mapaPreguntas.get(titulo);
	}
	
	public LinkedList<Pregunta> getPreguntas(){
		return preguntas;
	}
}
