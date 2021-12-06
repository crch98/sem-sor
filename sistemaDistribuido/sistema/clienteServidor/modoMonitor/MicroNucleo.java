package sistemaDistribuido.sistema.clienteServidor.modoMonitor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Hashtable;

import sistemaDistribuido.sistema.clienteServidor.modoUsuario.Proceso;

/**
 * Carlos Nicolás Sosa Chiunti
 * Sem. SOR
 * Ciclo 2021B
 * 07/noviembre/2021
 * Actividad de cierre 2
 * 
 */

public final class MicroNucleo extends MicroNucleoBase{
	Hashtable<Integer, ParMaquinaProceso> te = new Hashtable<Integer, ParMaquinaProceso>();
	Hashtable<Integer, byte[]> tr = new Hashtable<Integer, byte[]>();
	
	// Implementación de ParMaquinaProceso
	class MaquinaProceso implements ParMaquinaProceso {
		private int id;
		private String ip;
		
		MaquinaProceso(int id, String ip) {
			this.id = id;
			this.ip = ip;
		}
			
		public String dameIP() {
			return ip;
		}
			
		public int dameID() {
			return id;
		}
	}
	
	private static MicroNucleo nucleo=new MicroNucleo();

	/**
	 * 
	 */
	private MicroNucleo(){
	}

	/**
	 * 
	 */
	public final static MicroNucleo obtenerMicroNucleo(){
		return nucleo;
	}

	/*---Metodos para probar el paso de mensajes entre los procesos cliente y servidor en ausencia de datagramas.
    Esta es una forma incorrecta de programacion "por uso de variables globales" (en este caso atributos de clase)
    ya que, para empezar, no se usan ambos parametros en los metodos y fallaria si dos procesos invocaran
    simultaneamente a receiveFalso() al reescriir el atributo mensaje---*/
	byte[] mensaje;

	public void sendFalso(int dest,byte[] message){
		System.arraycopy(message,0,mensaje,0,message.length);
		notificarHilos();  //Reanuda la ejecucion del proceso que haya invocado a receiveFalso()
	}

	public void receiveFalso(int addr,byte[] message){
		mensaje=message;
		suspenderProceso();
	}
	/*---------------------------------------------------------*/

	/**
	 * 
	 */
	protected boolean iniciarModulos(){
		return true;
	}

	/**
	 * 
	 */
	protected void sendVerdadero(int dest,byte[] message){
		//sendFalso(dest,message);
		imprimeln("El proceso invocante es el "+super.dameIdProceso());
		imprimeln("Buscando al destinatario...");
		
		ParMaquinaProceso pmp = te.get(dest);
		
		if (pmp == null) { pmp = dameDestinatarioDesdeInterfaz(); }
		
		imprimeln("Destino encontrado - Empaquetando encabezados");
		imprimeln("Guardando origen");
		int origen = this.dameIdProceso();
		ByteBuffer bfOrigen = ByteBuffer.allocate(4);
		bfOrigen.putInt(origen);
		byte[] orig = bfOrigen.array();
		System.arraycopy(orig, 0, message, 0, orig.length);
		
		imprimeln("Guardando destino");
		int destino = pmp.dameID();
		ByteBuffer bfDestino = ByteBuffer.allocate(4);
		bfDestino.putInt(destino);
		byte[] des = bfDestino.array();
		System.arraycopy(des, 0, message, 4, des.length);
		
		imprimeln("Preparando mensaje a ser enviado...");
		try {
			DatagramPacket dp = new DatagramPacket(message, message.length,
					InetAddress.getByName(pmp.dameIP()), damePuertoRecepcion());
			DatagramSocket ds = dameSocketEmision();
			ds.send(dp);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	protected void receiveVerdadero(int addr,byte[] message){
		//receiveFalso(addr,message);
		imprimeln("Registrando datos en la tabla de recepción");
		tr.put(addr, message);
		
		imprimeln("Suspendiendo proceso en espera de paquetes.");
		//el siguiente aplica para la pr�ctica #2
		suspenderProceso();
	}

	/**
	 * Para el(la) encargad@ de direccionamiento por servidor de nombres en pr�ctica 5  
	 */
	protected void sendVerdadero(String dest,byte[] message){
	}

	/**
	 * Para el(la) encargad@ de primitivas sin bloqueo en pr�ctica 5
	 */
	protected void sendNBVerdadero(int dest,byte[] message){
	}

	/**
	 * Para el(la) encargad@ de primitivas sin bloqueo en pr�ctica 5
	 */
	protected void receiveNBVerdadero(int addr,byte[] message){
	}

	/**
	 * 
	 */
	public void run(){
		imprimeln("Preparándose para recibir paquetes de la red");
		byte[] buffer = new byte[1024];
		DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
		DatagramSocket sr = dameSocketRecepcion();

		while(seguirEsperandoDatagramas()){
			imprimeln("Invocando a receive en el socket para el arribo de mensajes");
			try {
				sr.receive(dp);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			imprimeln("Se ha recibido un paquete");
			
			imprimeln("Averiguando origen...");
			byte[] orig = new byte[4];
			System.arraycopy(buffer, 0, orig, 0, 4);
			int origen = ByteBuffer.wrap(orig).getInt();
			
			imprimeln("Averiguando destino...");
			byte[] dest = new byte[4];
			System.arraycopy(buffer, 4, dest, 0, 4);
			int destino = ByteBuffer.wrap(dest).getInt();
			
			imprimeln("Averiguando IP origen");
			String ip = dp.getAddress().getHostAddress();
			
			imprimeln("Origen: " + origen);
			imprimeln("Destino: " + destino);
			imprimeln("IP origen: " + ip);
			
			imprimeln("Buscando si existe algún proceso destino");
			Proceso p = dameProcesoLocal(destino);
			
			if (buffer[1023] == -1) {
				imprimeln("El paquete recibido es AU");
				
				imprimeln("Despertando al cliente del receive");
				Proceso p2 = dameProcesoLocal(origen);
				reanudarProceso(p2);
			} else {
				if (p != null) {
					imprimeln("Buscando si el proceso destino está en espera");
					byte[] arr = tr.get(destino);
					
					if (arr != null) {
						imprimeln("Quitando al destino de la tabla");
						tr.remove(destino);
						
						imprimeln("Guardando datos del origen");
						MaquinaProceso mp = new MaquinaProceso(origen, ip);
						te.put(origen, mp);
						
						imprimeln("Copiando datos al espacio del proceso destino");
						System.arraycopy(buffer, 0, arr, 0, arr.length);
						
						imprimeln("Reanudando proceso destino");
						reanudarProceso(p);
					} else {} 
				} else {
					imprimeln("Creando paquete AU");
					buffer[1023] = -1;
					try {
						DatagramPacket dp2 = new DatagramPacket(buffer, buffer.length,
								InetAddress.getByName(ip), damePuertoRecepcion());
						DatagramSocket ds = dameSocketEmision();
						ds.send(dp2);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
}
