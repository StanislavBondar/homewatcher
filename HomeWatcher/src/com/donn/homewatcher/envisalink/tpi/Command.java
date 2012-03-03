package com.donn.homewatcher.envisalink.tpi;

public class Command {
	
	private String commandString = new String("");
	private String dataString = new String("");
	private String checksumString = new String("");
	private String terminationString = new String("");
	
	public Command() {
		terminationString = "\r\n";
	}
	
	public static void main (String [] args) {
		Command command = new Command();
		command.test();
	}
	
	private void test() {
		Command command = new Command();
		command.setCommand("005");
		command.setData("654321");
	}
	
	public String getCompleteCommand() {
		StringBuffer sb = new StringBuffer();

		sb.append(commandString);
		sb.append(dataString);
		sb.append(checksumString);
		sb.append(terminationString);

		return sb.toString();
	}
	
	public String toString() {
		return getCompleteCommand();
	}
	
	public void setCommand(String commandString) {
		this.commandString = commandString;
		recalculateChecksum();
	}
	
	public void setData(String dataString) {
		this.dataString = dataString;
		recalculateChecksum();
	}
	
	private void recalculateChecksum() {
		int checkSum;
		int runningTotal = 0;
		checksumString = "ZZ";
		
		for(byte s : commandString.getBytes()) {
			runningTotal = s + runningTotal;
		}
		for(byte s : dataString.getBytes()) {
			runningTotal = s + runningTotal;
		}
		
		checkSum = runningTotal;
		String hexCheckSum = Integer.toHexString(checkSum);
		hexCheckSum = hexCheckSum.substring(hexCheckSum.length() - 2).toUpperCase();

		checksumString = hexCheckSum;
	}
}
