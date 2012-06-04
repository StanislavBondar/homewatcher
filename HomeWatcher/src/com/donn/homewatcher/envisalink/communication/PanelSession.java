package com.donn.homewatcher.envisalink.communication;

import com.donn.homewatcher.envisalink.tpi.Command;

public class PanelSession {
	
	private static PanelSession panelSession = null;
	private static PanelConnection panelConnection = null;
	
	public static PanelSession getPanelSession() {
		if (panelSession == null) {
			panelSession = new PanelSession();
			panelConnection = new PanelConnection();
		}
		return panelSession;
	}
	
	public void open(String server, int port, int timeout) throws PanelException {
		panelConnection.open(server, port, timeout);
	}
	
	public String read() throws PanelException {
		return panelConnection.read();
	}
	
	public void close() throws PanelException {
		panelConnection.close();
	}
	
	public void runCommand(Command panelCommand) throws PanelException {
		panelConnection.write(panelCommand.toString());
	}

}
