package sistemaDistribuido.sistema.clienteServidor.modoUsuario;

import sistemaDistribuido.sistema.clienteServidor.modoMonitor.Nucleo;
import sistemaDistribuido.sistema.clienteServidor.modoUsuario.Proceso;
import sistemaDistribuido.util.Escribano;

/**
 * Carlos Nicolás Sosa Chiunti
 * Sem. SOR
 * Ciclo 2021B
 * 07/noviembre/2021
 * Actividad de cierre 2
 * 
 */

public class ProcesoCliente extends Proceso {
	
	private final int BUFFER_SIZE = 1024;
	
	private final int CREATE = 1;
	private final int DELETE = 2;
	private final int READ   = 3;
	private final int WRITE  = 4;
	
	private String request;
	private int codOp;
	
	
	public void setRequest(String r) {
		request = r;
	}
	
	public void setCodOp(String cod) {
		if (cod.equalsIgnoreCase("crear")) {
			codOp = CREATE;
		} else if (cod.equalsIgnoreCase("eliminar")) {
			codOp = DELETE;
		} else if (cod.equalsIgnoreCase("leer")) {
			codOp = READ;
		} else if (cod.equalsIgnoreCase("escribir")) {
			codOp = WRITE;
		}
	}
	
	private String cleanInput(byte[] s) {
		var req = new byte[1015];
		int idx = 0;
		
		for (int i = 8; i < s.length; i++) {
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
	public ProcesoCliente(Escribano esc) {
		super(esc);
		start();
	}
	

	/**
	 * 
	 */
	public void run() {
		imprimeln("Proceso cliente en ejecucion.");
		imprimeln("Esperando datos para continuar.");
		
		Nucleo.suspenderProceso();
		
		byte[] solCliente  = new byte[BUFFER_SIZE];
		byte[] respCliente = new byte[BUFFER_SIZE];
		
		var reqInfo = request.getBytes();

		solCliente[8] = (byte) codOp;
		System.arraycopy(reqInfo, 0, solCliente, 9, reqInfo.length);
		
		Nucleo.send(248, solCliente);
		
		Nucleo.receive(dameID(),respCliente);
		
		var resp = cleanInput(respCliente);
		imprimeln("Respuesta del servidor: " + resp);
	}
}
