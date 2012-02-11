package com.donn.envisalink.tpi;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.donn.envisalink.communication.PanelException;
import com.donn.envisalink.communication.PanelSession;

public class SecurityPanel {
	
	private PanelSession panelSession = PanelSession.getPanelSession();
	private Command command;
	private PanelTransaction panelTransaction;
	
	public String read() throws PanelException {
		panelSession = PanelSession.getPanelSession();
		
		try {
			return panelSession.read();
		}
		catch (Exception e) {
			throw new PanelException(e, "Panel read method aborted.");
		}
	}
	
	public boolean open(String server, int port, int timeout) throws PanelException {
		return panelSession.open(server, port, timeout);
	}

	
	public boolean close() throws PanelException {
		
		try {
			return panelSession.close();
		}
		catch (Exception e) {
			throw new PanelException(e, "Error in SecurityPanel.close()");
		}
	}
	
	public String poll() throws PanelException {
		
		command = new Command();
		command.setCommand("000");
		command.setData("");
			
		return runCommand(command);
	}
	
	public String statusReport() throws PanelException {

		command = new Command();
		command.setCommand("001");
		command.setData("");
			
		return runCommand(command);
	}
	
	public String networkLogin(String password) throws PanelException {
		if (password == null || password.length() < 1 || password.length() > 6) {
			throw new PanelException("Password is invalid, must be between 1 and 6 chars: " + password);
		}
		
		command = new Command();
		command.setCommand("005");
		command.setData(password);
		
		return runCommand(command);
	}
	
	public String setTimeAndDate(String hhmmMMDDYY) throws PanelException {
		DateFormat dateFormat = new SimpleDateFormat("hhmmMMDDYY");
		
		try {
			dateFormat.parse(hhmmMMDDYY);
		}
		catch (ParseException e) {
			throw new PanelException(e, "Could not format date: " + hhmmMMDDYY + " to hhmmMMDDYY format.");
		}
		
		command = new Command();
		command.setCommand("010");
		command.setData(hhmmMMDDYY);
		
		return runCommand(command);
	}
	
	public String commandOutputControl(String partition1to8, String output1to4) throws PanelException {
		if (partition1to8 == null || !partition1to8.matches("[1-8]")) {
			throw new PanelException("partition1to8 must be a 1 character string from 1 to 8, it was: " + 
					partition1to8);
		}
		if (output1to4 == null || !output1to4.matches("[1-4]")) {
			throw new PanelException("output1to4 must be a 1 character string from 1 to 4, it was: " + 
					output1to4);
		}
		
		command = new Command();
		command.setCommand("020");
		command.setData(partition1to8 + output1to4);
		
		return runCommand(command);
	}
	
	public String partitionArmAway(String partition1to8) throws PanelException {
		if (partition1to8 == null || !partition1to8.matches("[1-8]")) {
			throw new PanelException("partition1to8 must be a 1 character string from 1 to 8, it was: " + 
					partition1to8);
		}
		
		command = new Command();
		command.setCommand("030");
		command.setData("");
			
		return runCommand(command);
	}
	
	public String partitionArmStay(String partition1to8) throws PanelException {
		if (partition1to8 == null || !partition1to8.matches("[1-8]")) {
			throw new PanelException("partition1to8 must be a 1 character string from 1 to 8, it was: " + 
					partition1to8);
		}
		
		command = new Command();
		command.setCommand("031");
		command.setData("");
			
		return runCommand(command);
	}
	
	public String partitionArmStayZeroEntry(String partition1to8) throws PanelException {
		if (partition1to8 == null || !partition1to8.matches("[1-8]")) {
			throw new PanelException("partition1to8 must be a 1 character string from 1 to 8, it was: " + 
					partition1to8);
		}
		
		command = new Command();
		command.setCommand("032");
		command.setData("");
			
		return runCommand(command);
	}
	
	public String runRawCommand(String rawCommand) throws PanelException {
		
		try {
			command = new Command();
			command.setCommand(rawCommand.substring(0, 3));
			command.setData(rawCommand.substring(3));
		}
		catch (StringIndexOutOfBoundsException e) {
			throw new PanelException("Invalid command string: " + rawCommand);
		}
		
		return runCommand(command);
	}
	
	private String runCommand(Command command) throws PanelException {
		panelTransaction = new PanelTransaction();
		
		try {
			panelTransaction = panelSession.runCommand(command);
		}
		catch (Exception e) {
			throw new PanelException(e, "Error running command: " + 
					command.getCompleteCommand(), panelTransaction);
		}
		
		return panelTransaction.getResponse();
		
	}
	
}
