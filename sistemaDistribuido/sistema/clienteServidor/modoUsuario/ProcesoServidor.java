package sistemaDistribuido.sistema.clienteServidor.modoUsuario;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import sistemaDistribuido.sistema.clienteServidor.modoMonitor.Nucleo;
import sistemaDistribuido.sistema.clienteServidor.modoUsuario.Proceso;
import sistemaDistribuido.util.Escribano;
import sistemaDistribuido.util.Pausador;

/**
 * Carlos Nicolás Sosa Chiunti
 * Sem. SOR
 * Ciclo 2021B
 * 07/noviembre/2021
 * Actividad de Cierre 2
 * 
 */

public class ProcesoServidor extends Proceso {

	
	private final int BUFFER_SIZE = 1024;
	
	private final int CREATE = 1;
	private final int DELETE = 2;
	private final int READ   = 3;
	private final int WRITE  = 4;
	

	private String response;
	
	public void createFile(String filename) throws IOException {
		File f = new File(filename);
		
		if (f.createNewFile()) {
			response = "¡Archivo " + filename + " creado exitosamente!";
		} else {
			response = "Error realizando operacion.";
		}
	}
	
	public void deleteFile(String filename) throws IOException {
		File f = new File(filename);
		
		if (f.isFile()) {
			if (f.delete()) {
				response = "¡Archivo " + filename + " eliminado exitosamente!";
			} else {
				response = "Error realizando operacion.";
			}
		} else {
			imprimeln("No se pudo realizar la operacion.");
		}
	}
	
	/**
	 * 
	 * @param s
	 * @throws IOException
	 * 
	 * Para escribir al archivo, se necesita el nombre del archivo, seguido del texto, separados por "-".
	 * Ejemplo: test.txt-Hola
	 * 
	 */
	public void writeToFile(String s) throws IOException {
		
		var aux = s.split("-");
		var filename = aux[0];
		var text = aux[1];
		
		FileWriter w = new FileWriter(filename, true);
		
		w.write(text + "\n");
		w.close();
	}
	
	/**
	 * 
	 * @param s
	 * @throws IOException
	 * 
	 * Para leer del archivo, se escribe el nombre de este y la linea a leer, separados por "-".
	 * Las líneas empiezan a contar desde 0.
	 * Ejemplo: test.txt-3
	 * 
	 */
	public void readFromFile(String s) throws IOException {
		var aux = s.split("-");
		var filename = aux[0];
		var targetLine = Integer.parseInt(aux[1]);
		
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		
		String line = reader.readLine();
		String output = null;
		int i = 0;
		
		while (line != null) {
			if (i++ == targetLine) {
				output = line;
			}
			
			line = reader.readLine();
		}
		
		reader.close();
		
		response = output;
	}
	
	private String cleanInput(byte[] s) {
		var req = new byte[1015];
		int idx = 0;
		
		for (int i = 9; i < s.length; i++) {
			if (s[i] > 0) {
				req[idx++] = s[i];
			}
		}
		
		String output = new String(req);
		
		return output.trim();
	}
	
	/**
	 * 
	 */
	public ProcesoServidor(Escribano esc){
		super(esc);
		start();
	}

	/**
	 * 
	 */
	public void run() {
		imprimeln("Proceso servidor en ejecucion.");
		
		byte[] solServidor = new byte[BUFFER_SIZE];
		byte[] respServidor;
		byte requestOpCode;
				
		while(continuar()) {
			Nucleo.receive(dameID(), solServidor);
			
			requestOpCode = solServidor[8];
			imprimeln("Petición del cliente: " + requestOpCode);
			
			String filename = cleanInput(solServidor);
			
			respServidor = new byte[BUFFER_SIZE];
			
			switch (requestOpCode) {
			case 1:
				imprimeln("Creando archivo " + filename);
				try {
					createFile(filename);
				} catch (IOException e) {
					imprimeln("No se pudo crear el archivo.");
				}
				break;
			case 2:
				imprimeln("Eliminando archivo " + filename);
				try {
					deleteFile(filename);
				} catch (IOException e) {
					imprimeln("No se pudo eliminar el archivo.");
				}
				break;
			case 3:
				imprimeln("Leyendo archivo " + filename);
				try {
					readFromFile(filename);
				} catch (IOException e1) {
					imprimeln("No pudo realizarse la operacion.");
					response = "Error realizando lectura.";
				}
				break;
			case 4:
				imprimeln("Escribiendo en archivo " + filename);
				try {
					writeToFile(filename);
				} catch (IOException e) {
					imprimeln("No pudo realizarse la operacion.");
					response = "Error realizando escritura.";
				}
				break;
			}
						
			var r = response.getBytes();
			System.arraycopy(r, 0, respServidor, 8, r.length);
			byte[] orig = new byte[4];
			System.arraycopy(solServidor, 0, orig, 0, 4);
			int origen = ByteBuffer.wrap(orig).getInt();
			
			Pausador.pausa(1000);  //sin esta l�nea es posible que Servidor solicite send antes que Cliente solicite receive
			imprimeln("Enviando respuesta al id " + origen);
			
			Nucleo.send(origen, respServidor);
		}
	}
}
