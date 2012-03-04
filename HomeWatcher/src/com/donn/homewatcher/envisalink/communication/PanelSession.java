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
	
	public boolean open(String server, int port, int timeout) throws PanelException {
		return panelConnection.open(server, port, timeout);
	}
	
	public String read() throws PanelException {
		return panelConnection.read();
	}
	
	public boolean close() throws PanelException {
		return panelConnection.close();
	}
	
	public void runCommand(Command panelCommand) throws PanelException {
		panelConnection.write(panelCommand.toString());
	}

}
