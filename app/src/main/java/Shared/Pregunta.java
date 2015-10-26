package Shared;

import java.io.Serializable;
import java.util.LinkedList;

public class Pregunta implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String titulo;
	public LinkedList<Opcion> opciones;
}
