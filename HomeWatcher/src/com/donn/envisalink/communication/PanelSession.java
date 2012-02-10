package com.donn.envisalink.communication;

import com.donn.envisalink.tpi.Command;
import com.donn.envisalink.tpi.PanelTransaction;

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
	
	public String read() throws Exception {
		return panelConnection.read();
	}
	
	public boolean close() throws Exception {
		return panelConnection.close();
	}
	
	public PanelTransaction runCommand(Command panelCommand) throws Exception {
		PanelTransaction panelTransaction = new PanelTransaction();
		panelTransaction.setRequest(panelCommand);
		
		panelConnection.write(panelCommand.toString());
		
		panelTransaction.setResponse("Socket Write Complete");
		
		return panelTransaction;
	}

}
