package com.donn.envisalink.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.InputStreamReader;

public class PanelConnection {

	private Socket socket = null;
	private OutputStreamWriter out;
	private BufferedReader in;
	
	public void write(String stringToWrite) throws Exception {
		out.write(stringToWrite);
		out.flush();
	}
	
	public String readLine() throws Exception {
		return in.readLine();
	}

	public String read() throws Exception {
		String line = "";
	
		try {
			line = readLine();
			return line;
		}
		catch (SocketException e) {
			if (e.getMessage().equalsIgnoreCase("socket closed")) {
				throw new PanelException("Socket closed... shutting down continuous read: " + e.getMessage());
			}
			else {
				e.printStackTrace();
				throw new PanelException(e, "Continuous read of socket in new thread failed: " + e.getMessage());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new PanelException(e, "Continuous read of socket in new thread failed: " + e.getMessage());
		}
	}
	
	//TODO: pass these values in from properties
	boolean open(String server, int port, int timeout) throws PanelException {
		try {
			socket = new Socket();
			SocketAddress socketAddress = new InetSocketAddress(server, port);
			socket.connect(socketAddress, timeout);
			out = new OutputStreamWriter(socket.getOutputStream(), "US-ASCII");
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}
		catch (UnknownHostException e) {
			throw new PanelException(e, "Could not open connection. Unknown Host: " + e.getMessage());
		}
		catch (IOException e) {
			throw new PanelException(e, "Could not open connection. IO Exception: " + e.getMessage());
		}
		
		return true;
	}
	
	public boolean close() throws Exception {
		try {
			socket.close();
			out.close();
			in.close();
		}
		catch (Exception e) {
			throw new PanelException(e, "Could not close connection: " + e.getMessage());
		}
		return true;
	}
	
}
